package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;

import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static software.amazon.awscdk.core.Duration.seconds;

public class FargateClusterV2 extends Construct {

    private static final String AMAZON_LINUX = "arn:aws:ecr:us-west-2:137112412989:repository/amazonlinux";

    private FargateService service;

    private ApplicationLoadBalancer balancer;

    public FargateClusterV2(final Construct scope,
                            final String id,
                            final int containerPort) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Private Cloud
        ///////////////////////////////////////////////////////////////////////////

        Vpc vpc = Vpc.Builder
                .create(this, "Vpc")
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Task Definition
        ///////////////////////////////////////////////////////////////////////////

        // This is a workaround. The Amazon Linux container will be replaced by our container after the first execution of CodePipeline.
        IRepository defaultRegistry = Repository.fromRepositoryArn(this, "DefaultRegistry", AMAZON_LINUX);

        ContainerImage defaultImage = ContainerImage.fromEcrRepository(defaultRegistry);

        Map<String, String> environmentVariables = Map.of(
                "CONTAINER_PORT", valueOf(containerPort));

        TaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, "TaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinition containerDefinition = ContainerDefinition.Builder
                .create(this, "ContainerDefinition")
                .essential(true)
                .image(defaultImage)
                .environment(environmentVariables)
                .taskDefinition(taskDefinition)
                .build();

        PortMapping portMapping = PortMapping.builder()
                .containerPort(containerPort)
                .hostPort(containerPort)
                .protocol(Protocol.TCP)
                .build();

        containerDefinition.addPortMappings(portMapping);

        ///////////////////////////////////////////////////////////////////////////
        // Fargate Cluster
        ///////////////////////////////////////////////////////////////////////////

        Cluster cluster = Cluster.Builder
                .create(this, "FargateCluster")
                .vpc(vpc)
                .build();

        FargateService fargateService = FargateService.Builder
                .create(this, "FargateService")
                .cluster(cluster)
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Load Balancer
        ///////////////////////////////////////////////////////////////////////////

        ApplicationLoadBalancer loadBalancer = ApplicationLoadBalancer.Builder
                .create(this, "LoadBalancer")
                .vpc(vpc)
                .internetFacing(true)
                .build();

        ApplicationListener applicationListener = ApplicationListener.Builder
                .create(this, "ApplicationListener")
                .open(true)
                .port(80)
                .protocol(ApplicationProtocol.HTTP)
                .loadBalancer(loadBalancer)
                .build();

        HealthCheck healthCheck = HealthCheck.builder()
                .healthyThresholdCount(2)
                .interval(seconds(10))
                .path("/health-checks")
                .build();

        AddApplicationTargetsProps applicationTarget = AddApplicationTargetsProps.builder()
                .port(containerPort)
                .targets(List.of(fargateService))
                .healthCheck(healthCheck)
                .deregistrationDelay(seconds(0))
                .build();

        applicationListener.addTargets("Target", applicationTarget);

        this.service = fargateService;
        this.balancer = loadBalancer;
    }

    public FargateService getService() {
        return service;
    }

    public ApplicationLoadBalancer getBalancer() {
        return balancer;
    }
}
