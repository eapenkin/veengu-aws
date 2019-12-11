package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.CodeCommitSourceProps;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codecommit.IRepository;
import software.amazon.awscdk.services.codecommit.Repository;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.EcsDeployAction;
import software.amazon.awscdk.services.ecs.FargateService;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.valueOf;
import static software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType.PLAINTEXT;
import static software.amazon.awscdk.services.codebuild.Cache.local;
import static software.amazon.awscdk.services.codebuild.LinuxBuildImage.UBUNTU_14_04_OPEN_JDK_11;
import static software.amazon.awscdk.services.codebuild.LocalCacheMode.*;
import static software.amazon.awscdk.services.codebuild.Source.codeCommit;
import static software.amazon.awscdk.services.codepipeline.actions.CodeCommitTrigger.EVENTS;


public class ContainerPipeline extends Construct {

    private static BuildEnvironmentVariable plaintext(String value) {
        return BuildEnvironmentVariable.builder().type(PLAINTEXT).value(value).build();
    }

    private static BuildEnvironmentVariable plaintext(int value) {
        return plaintext(valueOf(value));
    }

    public ContainerPipeline(final Construct scope,
                             final String id,
                             final String region, // TODO remove
                             final String account, // TODO remove
                             final String repositoryName,
                             final String branchName,
                             final int containerPort,
                             final software.amazon.awscdk.services.ecr.IRepository dockerRegistry,
                             final FargateService fargateService) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Git Repository
        ///////////////////////////////////////////////////////////////////////////

        IRepository gitRepository = Repository.fromRepositoryName(this, "GIT", repositoryName);

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

        Map<String, BuildEnvironmentVariable> environmentVariables = new TreeMap<>(Map.of(
                "AWS_DEFAULT_REGION", plaintext(region),
                "SERVER_PORT", plaintext(containerPort),
                "CONTAINER_NAME", plaintext(fargateService.getTaskDefinition().getDefaultContainer().getContainerName()),
                "IMAGE_NAME", plaintext(dockerRegistry.getRepositoryName()),
                "REGISTRY_HOST", plaintext(account + ".dkr.ecr." + region + ".amazonaws.com")));

        Project buildProject = Project.Builder
                .create(this, "Builder")
                .source(codeCommit(repositorySource))
                .environment(buildEnvironment)
                .environmentVariables(environmentVariables)
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
                .service(fargateService)
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
        dockerRegistry.grantPull(fargateService.getTaskDefinition().getExecutionRole());
    }
}
