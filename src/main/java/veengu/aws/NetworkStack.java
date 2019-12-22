package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddFixedResponseProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

import java.util.List;

import static software.amazon.awscdk.services.ec2.GatewayVpcEndpointAwsService.S3;
import static software.amazon.awscdk.services.ec2.InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS;
import static software.amazon.awscdk.services.ec2.InterfaceVpcEndpointAwsService.ECR_DOCKER;
import static software.amazon.awscdk.services.ec2.Peer.anyIpv4;
import static software.amazon.awscdk.services.ec2.Port.tcp;
import static software.amazon.awscdk.services.ec2.SubnetType.ISOLATED;
import static software.amazon.awscdk.services.ec2.SubnetType.PUBLIC;
import static software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol.HTTP;
import static software.amazon.awscdk.services.elasticloadbalancingv2.ContentType.APPLICATION_JSON;
import static software.amazon.awscdk.services.route53.HostedZone.fromHostedZoneAttributes;
import static software.amazon.awscdk.services.route53.RecordTarget.fromAlias;

public class NetworkStack extends Stack {

    private final Cluster cluster;
    private final ApplicationListener listener;
    private final IHostedZone zone;
    private final Vpc vpc;
    private final SubnetSelection subnets;

    public NetworkStack(final Construct scope,
                        final String id,
                        final int internetPort,
                        final String zoneName,
                        final String zoneId) {
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

        vpc = Vpc.Builder
                .create(this, "VPC")
                .natGateways(0)
                .maxAzs(2)
                .subnetConfiguration(List.of(publicSubnet, isolatedSubnet))
                .build();

        subnets = SubnetSelection.builder()
                .subnetType(ISOLATED)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // AWS PrivateLink
        ///////////////////////////////////////////////////////////////////////////

        SecurityGroup securityGroup = SecurityGroup.Builder
                .create(this, "PLSecurityGroup")
                .vpc(vpc)
                .allowAllOutbound(true)
                .build();
        securityGroup.addIngressRule(anyIpv4(), tcp(443));

        InterfaceVpcEndpointOptions dockerInterface = InterfaceVpcEndpointOptions.builder()
                .service(ECR_DOCKER)
                .subnets(subnets)
                .securityGroups(List.of(securityGroup))
                .build();

        InterfaceVpcEndpointOptions logsInterface = InterfaceVpcEndpointOptions.builder()
                .service(CLOUDWATCH_LOGS)
                .subnets(subnets)
                .securityGroups(List.of(securityGroup))
                .build();

        GatewayVpcEndpointOptions s3Gateway = GatewayVpcEndpointOptions.builder()
                .service(S3)
                .subnets(List.of(subnets))
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
                .create(this, "ALB")
                .vpc(vpc)
                .internetFacing(true)
                .build();

        listener = ApplicationListener.Builder
                .create(this, "HTTP")
                .open(true)
                .port(internetPort)
                .protocol(HTTP)
                .loadBalancer(balancer)
                .build();

        AddFixedResponseProps defaultResponse = AddFixedResponseProps.builder()
                .statusCode("400")
                .contentType(APPLICATION_JSON)
                .messageBody("{\"message\":\"Route not found\",\"errors\":[{\"code\":\"ROUTE_NOT_FOUND\",\"message\":\"Please use console to find out a proper value of Host header. It should be formatted as <branch_name>.veengu.xyz.\"}]}")
                .build();

        listener.addFixedResponse("DefaultResponse", defaultResponse);

        ///////////////////////////////////////////////////////////////////////////
        // Hosted Zone
        ///////////////////////////////////////////////////////////////////////////

        HostedZoneAttributes zoneAttributes = HostedZoneAttributes.builder()
                .hostedZoneId(zoneId)
                .zoneName(zoneName)
                .build();

        zone = fromHostedZoneAttributes(this, "Zone", zoneAttributes);

        ///////////////////////////////////////////////////////////////////////////
        // Alias Record
        ///////////////////////////////////////////////////////////////////////////

        ARecord.Builder
                .create(this, "ARecord")
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

    public Vpc getVpc() {
        return vpc;
    }

    public SubnetSelection getSubnets() {
        return subnets;
    }
}
