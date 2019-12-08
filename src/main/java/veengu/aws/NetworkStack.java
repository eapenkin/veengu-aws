package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddFixedResponseProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

import java.util.List;

import static software.amazon.awscdk.services.ec2.InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS;
import static software.amazon.awscdk.services.ec2.InterfaceVpcEndpointAwsService.ECR_DOCKER;
import static software.amazon.awscdk.services.ec2.Peer.anyIpv4;
import static software.amazon.awscdk.services.ec2.Port.tcp;
import static software.amazon.awscdk.services.ec2.SubnetType.ISOLATED;
import static software.amazon.awscdk.services.ec2.SubnetType.PUBLIC;
import static software.amazon.awscdk.services.elasticloadbalancingv2.ContentType.TEXT_PLAIN;
import static software.amazon.awscdk.services.route53.HostedZone.fromHostedZoneAttributes;
import static software.amazon.awscdk.services.route53.RecordTarget.fromAlias;

public class NetworkStack extends Stack {

    public static final String ZONE_NAME = "veengu.xyz";
    public static final String ZONE_ID = "Z3K66451X409D1";

    private final Cluster cluster;

    private final ApplicationListener listener;

    private final IHostedZone zone;

    public NetworkStack(final Construct scope,
                        final String id,
                        final int internetPort) {
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
                .create(this, "VPC")
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

        SecurityGroup httpsSecurityGroup = SecurityGroup.Builder
                .create(this, "SecurityGroup")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();
        httpsSecurityGroup.addIngressRule(anyIpv4(), tcp(443));

        InterfaceVpcEndpointOptions dockerInterface = InterfaceVpcEndpointOptions.builder()
                .service(ECR_DOCKER)
                .subnets(isolatedSubnets)
                .securityGroups(List.of(httpsSecurityGroup))
                .build();

        InterfaceVpcEndpointOptions logsInterface = InterfaceVpcEndpointOptions.builder()
                .service(CLOUDWATCH_LOGS)
                .subnets(isolatedSubnets)
                .securityGroups(List.of(httpsSecurityGroup))
                .build();

        GatewayVpcEndpointOptions s3Gateway = GatewayVpcEndpointOptions.builder()
                .service(GatewayVpcEndpointAwsService.S3)
                .subnets(List.of(isolatedSubnets))
                .build();

        vpc.addInterfaceEndpoint("RegistryInterface", dockerInterface);
        vpc.addInterfaceEndpoint("LogsInterface", logsInterface);
        vpc.addGatewayEndpoint("StorageGateway", s3Gateway);

        ///////////////////////////////////////////////////////////////////////////
        // ECS Cluster
        ///////////////////////////////////////////////////////////////////////////

        cluster = Cluster.Builder
                .create(this, "Cluster")
                .vpc(vpc)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Load Balancer
        ///////////////////////////////////////////////////////////////////////////

        ApplicationLoadBalancer balancer = ApplicationLoadBalancer.Builder
                .create(this, "ELB")
                .vpc(vpc)
                .internetFacing(true)
                .build();

        listener = ApplicationListener.Builder
                .create(this, "HTTPListener")
                .open(true)
                .port(internetPort)
                .protocol(ApplicationProtocol.HTTP)
                .loadBalancer(balancer)
                .build();

        AddFixedResponseProps defaultResponse = AddFixedResponseProps.builder()
                .statusCode("400")
                .contentType(TEXT_PLAIN)
                .messageBody("Route not found")
                .build();

        listener.addFixedResponse("DefaultResponse", defaultResponse);

        ///////////////////////////////////////////////////////////////////////////
        // Hosted Zone
        ///////////////////////////////////////////////////////////////////////////

        HostedZoneAttributes zoneAttributes = HostedZoneAttributes.builder()
                .hostedZoneId(ZONE_ID)
                .zoneName(ZONE_NAME)
                .build();

        zone = fromHostedZoneAttributes(this, "Zone", zoneAttributes);

        ///////////////////////////////////////////////////////////////////////////
        // Alias Record
        ///////////////////////////////////////////////////////////////////////////

        ARecord.Builder
                .create(this, "Alias")
                .zone(zone)
                .target(fromAlias(new LoadBalancerTarget(balancer)))
                .build();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public ApplicationListener getListener() {
        return listener;
    }

    public IHostedZone getZone() {
        return zone;
    }
}
