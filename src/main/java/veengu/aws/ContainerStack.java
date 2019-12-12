package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.CodeCommitSourceProps;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codecommit.IRepository;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.EcsDeployAction;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetGroupsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.lang.String.valueOf;
import static software.amazon.awscdk.core.Duration.days;
import static software.amazon.awscdk.core.Duration.seconds;
import static software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType.PLAINTEXT;
import static software.amazon.awscdk.services.codebuild.Cache.local;
import static software.amazon.awscdk.services.codebuild.LinuxBuildImage.UBUNTU_14_04_OPEN_JDK_11;
import static software.amazon.awscdk.services.codebuild.LocalCacheMode.*;
import static software.amazon.awscdk.services.codebuild.Source.codeCommit;
import static software.amazon.awscdk.services.codecommit.Repository.fromRepositoryName;
import static software.amazon.awscdk.services.codepipeline.actions.CodeCommitTrigger.EVENTS;
import static software.amazon.awscdk.services.ecr.TagStatus.ANY;
import static software.amazon.awscdk.services.ecs.Protocol.TCP;
import static software.amazon.awscdk.services.route53.RecordTarget.fromAlias;

public class ContainerStack extends Stack {

    private static final String HEALTH_CHECKS = "/health-checks";
    private static final int CONTAINER_PORT = 8080;

    private static String upperCamel(String string) {
        return LOWER_HYPHEN.to(UPPER_CAMEL, string);
    }

    private static BuildEnvironmentVariable plaintext(String value) {
        return BuildEnvironmentVariable.builder().type(PLAINTEXT).value(value).build();
    }

    private static BuildEnvironmentVariable plaintext(int value) {
        return plaintext(valueOf(value));
    }

    public ContainerStack(final Construct scope,
                          final String id,
                          final NetworkStack networkStack,
                          final DatabaseStack databaseStack,
                          final String repositoryName,
                          final String branchName,
                          final int routingPriority) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Docker Registry
        ///////////////////////////////////////////////////////////////////////////
        LifecycleRule oneDayImage = LifecycleRule.builder()
                .tagStatus(ANY)
                .maxImageAge(days(1))
                .build();

        Repository dockerRegistry = Repository.Builder
                .create(this, "Registry")
                .repositoryName(repositoryName + "/" + branchName)
                .lifecycleRules(List.of(oneDayImage))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Task Definition
        ///////////////////////////////////////////////////////////////////////////
        TaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, "Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerImage containerImage = ContainerImage.fromEcrRepository(dockerRegistry);

        Map<String, String> environmentVariables = new TreeMap<>(Map.of(
                "SERVER_ADDRESS", "0.0.0.0",
                "SERVER_PORT", valueOf(CONTAINER_PORT),
                "DATASOURCE_URL", "jdbc:mysql://" + databaseStack.getSocketAddress(),
                "DATASOURCE_USERNAME", "user",
                "DATASOURCE_PASSWORD", "password",
                "SCHEMA_USERNAME", "admin",
                "SCHEMA_PASSWORD", "password"));

        PortMapping portMapping = PortMapping.builder()
                .containerPort(CONTAINER_PORT)
                .hostPort(CONTAINER_PORT)
                .protocol(TCP)
                .build();

        AwsLogDriver logDriver = AwsLogDriver.Builder
                .create()
                .streamPrefix(dockerRegistry.getRepositoryName())
                .build();

        String containerName = upperCamel(branchName) + "Container";

        ContainerDefinition containerDefinition = ContainerDefinition.Builder
                .create(this, containerName)
                .essential(true)
                .image(containerImage)
                .environment(environmentVariables)
                .taskDefinition(taskDefinition)
                .logging(logDriver)
                .build();

        containerDefinition.addPortMappings(portMapping);

        ///////////////////////////////////////////////////////////////////////////
        // Fargate Service
        ///////////////////////////////////////////////////////////////////////////
        FargateService service = FargateService.Builder
                .create(this, "Service")
                .cluster(networkStack.getCluster())
                .vpcSubnets(networkStack.getPlacement())
                .assignPublicIp(false)
                .taskDefinition(taskDefinition)
                .desiredCount(0)
                .minHealthyPercent(100)
                .maxHealthyPercent(200)
                .healthCheckGracePeriod(seconds(120))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Application Listener
        ///////////////////////////////////////////////////////////////////////////
        HealthCheck healthCheck = HealthCheck.builder()
                .healthyThresholdCount(2)
                .interval(seconds(10))
                .timeout(seconds(2))
                .path(HEALTH_CHECKS)
                .build();

        ApplicationTargetGroup targetGroup = ApplicationTargetGroup.Builder
                .create(networkStack, upperCamel(branchName) + "Group") // FIXME try to create inside this stack in 1.19.0
                .vpc(networkStack.getVpc())
                .port(CONTAINER_PORT)
                .targets(List.of(service))
                .healthCheck(healthCheck)
                .deregistrationDelay(seconds(10))
                .build();

        String domainName = branchName + "." + networkStack.getZone().getZoneName();

        AddApplicationTargetGroupsProps listenerRule = AddApplicationTargetGroupsProps.builder()
                .targetGroups(List.of(targetGroup))
                .hostHeader(domainName)
                .priority(routingPriority)
                .build();

        ARecord.Builder
                .create(this, "ARecord")
                .zone(networkStack.getZone())
                .recordName(domainName)
                .target(fromAlias(new LoadBalancerTarget(networkStack.getListener().getLoadBalancer())))
                .build();

        networkStack.getListener().addTargetGroups(upperCamel(branchName) + "Listener", listenerRule);

        ///////////////////////////////////////////////////////////////////////////
        // Git Repository
        ///////////////////////////////////////////////////////////////////////////
        IRepository gitRepository = fromRepositoryName(this, "Repository", repositoryName);

        ///////////////////////////////////////////////////////////////////////////
        // Build Project
        ///////////////////////////////////////////////////////////////////////////
        CodeCommitSourceProps repositorySource = CodeCommitSourceProps.builder()
                .cloneDepth(1)
                .repository(gitRepository)
                .build();

        BuildEnvironment buildEnvironment = BuildEnvironment.builder()
                .buildImage(UBUNTU_14_04_OPEN_JDK_11)
                .privileged(true)
                .build();

        Map<String, BuildEnvironmentVariable> buildVariables = new TreeMap<>(Map.of(
                "AWS_DEFAULT_REGION", plaintext(getRegion()),
                "SERVER_PORT", plaintext(CONTAINER_PORT),
                "CONTAINER_NAME", plaintext(containerName),
                "IMAGE_NAME", plaintext(dockerRegistry.getRepositoryName()),
                "REGISTRY_HOST", plaintext(getAccount() + ".dkr.ecr." + getRegion() + ".amazonaws.com")));

        Project buildProject = Project.Builder
                .create(this, "Builder")
                .source(codeCommit(repositorySource))
                .environment(buildEnvironment)
                .environmentVariables(buildVariables)
                .cache(local(SOURCE, DOCKER_LAYER, CUSTOM))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Source Stage
        ///////////////////////////////////////////////////////////////////////////
        Artifact sourceOutput = Artifact.artifact("SourceOutput");

        CodeCommitSourceAction sourceAction = CodeCommitSourceAction.Builder
                .create()
                .actionName("CodeCommit")
                .repository(gitRepository)
                .branch(branchName)
                .trigger(EVENTS)
                .output(sourceOutput)
                .build();

        StageProps sourceStage = StageProps.builder()
                .stageName("Source")
                .actions(List.of(sourceAction))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Build Stage
        ///////////////////////////////////////////////////////////////////////////
        Artifact buildOutput = Artifact.artifact("BuildOutput");

        CodeBuildAction buildAction = CodeBuildAction.Builder
                .create()
                .actionName("CodeBuild")
                .project(buildProject)
                .input(sourceOutput)
                .outputs(List.of(buildOutput))
                .build();

        StageProps buildStage = StageProps.builder()
                .stageName("Build")
                .actions(List.of(buildAction))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Deploy Stage
        ///////////////////////////////////////////////////////////////////////////
        EcsDeployAction deployAction = EcsDeployAction.Builder
                .create()
                .actionName("ECSDeploy")
                .service(service)
                .input(buildOutput)
                .build();

        StageProps deployStage = StageProps.builder()
                .stageName("Deploy")
                .actions(List.of(deployAction))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Code Pipeline
        ///////////////////////////////////////////////////////////////////////////
        Pipeline.Builder
                .create(this, "Pipeline")
                .stages(List.of(sourceStage, buildStage, deployStage))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Registry Grants
        ///////////////////////////////////////////////////////////////////////////
        dockerRegistry.grantPullPush(buildProject);
        dockerRegistry.grantPull(taskDefinition.getExecutionRole());
    }
}