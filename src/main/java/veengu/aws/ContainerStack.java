package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ecs.ICluster;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationLoadBalancer;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;

import static software.amazon.awscdk.services.route53.RecordTarget.fromAlias;

public class ContainerStack extends Stack {

    public static final String HEALTH_CHECKS = "/health-checks";

    public ContainerStack(final Construct scope,
                          final String id,
                          final String repositoryName,
                          final String branchName,
                          final int internetPort,
                          final int containerPort,
                          final ICluster cluster,
                          final IApplicationLoadBalancer balancer,
                          final IHostedZone zone) {
        super(scope, id);
        ContainerRegistry containerRegistry = new ContainerRegistry(this, "ContainerRegistry", repositoryName);
        ContainerService containerService = new ContainerService(this, "ContainerService", internetPort, containerPort, HEALTH_CHECKS, containerRegistry.getRegistry(), cluster, balancer);
        ContainerPipeline containerPipeline = new ContainerPipeline(this, "ContainerPipeline", getRegion(), getAccount(), repositoryName, branchName, containerPort, containerRegistry.getRegistry(), containerService.getService());

        ARecord.Builder
                .create(this, "AliasRecord")
                .zone(zone)
                .recordName(branchName + "." + zone.getZoneName())
                .target(fromAlias(new LoadBalancerTarget(balancer)))
                .build();
    }
}
