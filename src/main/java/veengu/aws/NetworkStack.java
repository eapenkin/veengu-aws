package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

import java.util.List;

import static software.amazon.awscdk.services.ec2.InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS;
import static software.amazon.awscdk.services.ec2.InterfaceVpcEndpointAwsService.ECR_DOCKER;
import static software.amazon.awscdk.services.ec2.Peer.anyIpv4;
import static software.amazon.awscdk.services.ec2.Port.tcp;
import static software.amazon.awscdk.services.ec2.SubnetType.ISOLATED;
import static software.amazon.awscdk.services.ec2.SubnetType.PUBLIC;

public class NetworkStack extends Stack {

    public static final String ZONE_NAME = "veengu.xyz";
    public static final String ZONE_ID = "Z3K66451X409D1";

    private final Cluster cluster;

    private final ApplicationLoadBalancer loadBalancer;

    private final IHostedZone hostedZone;

    public NetworkStack(final Construct scope,
                        final String id) {
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
                .create(this, "VirtualNetwork")
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

        SecurityGroup httpsIngressRule = SecurityGroup.Builder
                .create(this, "HttpsIngressRule")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();
        httpsIngressRule.addIngressRule(anyIpv4(), tcp(443));

        InterfaceVpcEndpointOptions dockerInterface = InterfaceVpcEndpointOptions.builder()
                .service(ECR_DOCKER)
                .subnets(isolatedSubnets)
                .securityGroups(List.of(httpsIngressRule))
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
        // ECS Cluster
        ///////////////////////////////////////////////////////////////////////////

        this.cluster = Cluster.Builder
                .create(this, "ContainerCluster")
                .vpc(vpc)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Load Balancer
        ///////////////////////////////////////////////////////////////////////////

        this.loadBalancer = ApplicationLoadBalancer.Builder
                .create(this, "LoadBalancer")
                .vpc(vpc)
                .internetFacing(true)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Hosted Zone
        ///////////////////////////////////////////////////////////////////////////

        HostedZoneAttributes zoneAttributes = HostedZoneAttributes.builder()
                .hostedZoneId(ZONE_ID)
                .zoneName(ZONE_NAME)
                .build();

        this.hostedZone = HostedZone.fromHostedZoneAttributes(this, "HostedZone", zoneAttributes);

        ///////////////////////////////////////////////////////////////////////////
        // Alias Record
        ///////////////////////////////////////////////////////////////////////////

        ARecord.Builder
                .create(this, "DomainRecord")
                .zone(hostedZone)
                .target(RecordTarget.fromAlias(new LoadBalancerTarget(loadBalancer)))
                .build();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public ApplicationLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public IHostedZone getHostedZone() {
        return hostedZone;
    }
}
