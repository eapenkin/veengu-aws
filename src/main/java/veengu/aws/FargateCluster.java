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
import static software.amazon.awscdk.services.ec2.InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS;
import static software.amazon.awscdk.services.ec2.InterfaceVpcEndpointAwsService.ECR_DOCKER;
import static software.amazon.awscdk.services.ec2.Peer.anyIpv4;
import static software.amazon.awscdk.services.ec2.Port.tcp;
import static software.amazon.awscdk.services.ec2.SubnetType.ISOLATED;
import static software.amazon.awscdk.services.ec2.SubnetType.PUBLIC;

public class FargateCluster extends Construct {

    private final FargateService service;

    private final ApplicationLoadBalancer balancer;

    public FargateCluster(final Construct scope,
                          final String id,
                          final int internetPort,
                          final int containerPort,
                          final String healthPath,
                          final IRepository dockerRegistry) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Private Cloud
        ///////////////////////////////////////////////////////////////////////////

        SubnetConfiguration publicSubnet = SubnetConfiguration.builder()
                .name("Public")
                .subnetType(PUBLIC)
                .cidrMask(18)
                .build();

        SubnetConfiguration isolatedSubnet = SubnetConfiguration.builder()
                .name("Isolated")
                .subnetType(ISOLATED)
                .cidrMask(18)
                .build();

        Vpc vpc = Vpc.Builder
                .create(this, "Vpc")
                .natGateways(0)
                .maxAzs(2)
                .subnetConfiguration(List.of(publicSubnet, isolatedSubnet))
                .build();

        SubnetSelection isolatedSubnets = SubnetSelection.builder()
                .subnetType(ISOLATED)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // AWS PrivateLink
        ///////////////////////////////////////////////////////////////////////////

        SecurityGroup allowIngressHttps = SecurityGroup.Builder
                .create(this, "AllowIngressHTTPS")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();
        allowIngressHttps.addIngressRule(anyIpv4(), tcp(443));

        InterfaceVpcEndpointOptions dockerInterface = InterfaceVpcEndpointOptions.builder()
                .service(ECR_DOCKER)
                .subnets(isolatedSubnets)
                .securityGroups(List.of(allowIngressHttps))
                .build();

        InterfaceVpcEndpointOptions logsInterface = InterfaceVpcEndpointOptions.builder()
                .service(CLOUDWATCH_LOGS)
                .subnets(isolatedSubnets)
                .build();

        GatewayVpcEndpointOptions s3Gateway = GatewayVpcEndpointOptions.builder()
                .service(GatewayVpcEndpointAwsService.S3)
                .subnets(List.of(isolatedSubnets))
                .build();

        vpc.addInterfaceEndpoint("DockerInterface", dockerInterface);
        vpc.addInterfaceEndpoint("CloudWatchInterface", logsInterface);
        vpc.addGatewayEndpoint("S3Gateway", s3Gateway);

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
                .vpcSubnets(isolatedSubnets)
                .assignPublicIp(false)
                .desiredCount(0)
                .minHealthyPercent(100)
                .maxHealthyPercent(200)
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
        this.balancer = loadBalancer;
    }

    public FargateService getService() {
        return service;
    }

    public ApplicationLoadBalancer getBalancer() {
        return balancer;
    }
}
