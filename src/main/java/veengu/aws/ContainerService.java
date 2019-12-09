package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetGroupsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

import java.util.List;
import java.util.Map;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.lang.String.valueOf;
import static software.amazon.awscdk.core.Duration.seconds;
import static software.amazon.awscdk.services.ec2.SubnetType.ISOLATED;
import static software.amazon.awscdk.services.route53.RecordTarget.fromAlias;

public class ContainerService extends Construct {

    private final FargateService service;

    private static String upperCamel(String string) {
        return LOWER_HYPHEN.to(UPPER_CAMEL, string);
    }

    public ContainerService(final Construct scope,
                            final String id,
                            final String branchName,
                            final int containerPort,
                            final String healthPath,
                            final int routingPriority,
                            final IRepository registry,
                            final Cluster cluster,
                            final ApplicationListener listener,
                            final IHostedZone zone) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Task Definition
        ///////////////////////////////////////////////////////////////////////////

        TaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, "TaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerImage containerImage = ContainerImage.fromEcrRepository(registry);

        Map<String, String> environmentVariables = Map.of(
                "CONTAINER_PORT", valueOf(containerPort));

        PortMapping portMapping = PortMapping.builder()
                .containerPort(containerPort)
                .hostPort(containerPort)
                .protocol(Protocol.TCP)
                .build();

        AwsLogDriver logDriver = AwsLogDriver.Builder
                .create()
                .streamPrefix(registry.getRepositoryName())
                .build();

        ContainerDefinition containerDefinition = ContainerDefinition.Builder
                .create(this, upperCamel(branchName) + "Container")
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

        SubnetSelection isolatedSubnets = SubnetSelection.builder()
                .subnetType(ISOLATED)
                .build();

        service = FargateService.Builder
                .create(this, "FargateService")
                .cluster(cluster)
                .taskDefinition(taskDefinition)
                .vpcSubnets(isolatedSubnets)
                .assignPublicIp(false)
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
                .path(healthPath)
                .build();

        ApplicationTargetGroup targetGroup = ApplicationTargetGroup.Builder
                .create(listener.getStack(), upperCamel(branchName) + "Group") // FIXME try to create inside this stack in 1.19.0
                .vpc(listener.getLoadBalancer().getVpc())
                .port(containerPort)
                .targets(List.of(service))
                .healthCheck(healthCheck)
                .deregistrationDelay(seconds(10))
                .build();

        AddApplicationTargetGroupsProps listenerRule = AddApplicationTargetGroupsProps.builder()
                .targetGroups(List.of(targetGroup))
                .hostHeader(branchName + "." + zone.getZoneName())
                .priority(routingPriority)
                .build();

        listener.addTargetGroups(upperCamel(branchName) + "Listener", listenerRule);

        ///////////////////////////////////////////////////////////////////////////
        // Domain Name
        ///////////////////////////////////////////////////////////////////////////

        ARecord.Builder
                .create(this, "Alias")
                .zone(zone)
                .recordName(branchName + "." + zone.getZoneName())
                .target(fromAlias(new LoadBalancerTarget(listener.getLoadBalancer())))
                .build();
    }

    public FargateService getService() {
        return service;
    }
}