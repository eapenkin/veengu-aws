package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;

import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static software.amazon.awscdk.core.Duration.seconds;
import static software.amazon.awscdk.services.ec2.SubnetType.ISOLATED;

public class ContainerService extends Construct {

    private final FargateService service;

    public ContainerService(final Construct scope,
                            final String id,
                            final int internetPort,
                            final int containerPort,
                            final String healthPath,
                            final IRepository dockerRegistry,
                            final ICluster fargateCluster,
                            final IApplicationLoadBalancer loadBalancer) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Task Definition
        ///////////////////////////////////////////////////////////////////////////

        TaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, "TaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerImage containerImage = ContainerImage.fromEcrRepository(dockerRegistry);

        Map<String, String> environmentVariables = Map.of(
                "CONTAINER_PORT", valueOf(containerPort));

        PortMapping portMapping = PortMapping.builder()
                .containerPort(containerPort)
                .hostPort(containerPort)
                .protocol(Protocol.TCP)
                .build();

        ContainerDefinition containerDefinition = ContainerDefinition.Builder
                .create(this, "ContainerDefinition")
                .essential(true)
                .image(containerImage)
                .environment(environmentVariables)
                .taskDefinition(taskDefinition)
                .build();

        containerDefinition.addPortMappings(portMapping);

        ///////////////////////////////////////////////////////////////////////////
        // Fargate Service
        ///////////////////////////////////////////////////////////////////////////

        SubnetSelection isolatedSubnets = SubnetSelection.builder()
                .subnetType(ISOLATED)
                .build();

        FargateService fargateService = FargateService.Builder
                .create(this, "FargateService")
                .cluster(fargateCluster)
                .taskDefinition(taskDefinition)
                .vpcSubnets(isolatedSubnets)
                .assignPublicIp(false)
                .desiredCount(0)
                .minHealthyPercent(100)
                .maxHealthyPercent(200)
                .healthCheckGracePeriod(seconds(60))
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Load Balancer
        ///////////////////////////////////////////////////////////////////////////

        ApplicationListener applicationListener = ApplicationListener.Builder
                .create(this, "ApplicationListener")
                .open(true)
                .port(internetPort)
                .protocol(ApplicationProtocol.HTTP)
                .loadBalancer(loadBalancer)
                .build();

        HealthCheck healthCheck = HealthCheck.builder()
                .healthyThresholdCount(2)
                .interval(seconds(10))
                .timeout(seconds(2))
                .path(healthPath)
                .build();

        AddApplicationTargetsProps applicationTarget = AddApplicationTargetsProps.builder()
                .port(containerPort)
                .targets(List.of(fargateService))
                .healthCheck(healthCheck)
                .deregistrationDelay(seconds(10))
                .build();

        applicationListener.addTargets("Target", applicationTarget);

        this.service = fargateService;
    }

    public FargateService getService() {
        return service;
    }
}
