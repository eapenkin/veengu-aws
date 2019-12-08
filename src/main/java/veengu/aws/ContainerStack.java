package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.route53.IHostedZone;

public class ContainerStack extends Stack {

    private static final String HEALTH_CHECKS = "/health-checks";
    private static final int CONTAINER_PORT = 8080;

    public ContainerStack(final Construct scope,
                          final String id,
                          final String repositoryName,
                          final String branchName,
                          final int routingPriority,
                          final Cluster cluster,
                          final ApplicationListener listener,
                          final IHostedZone zone) {
        super(scope, id);
        ContainerRegistry containerRegistry = new ContainerRegistry(this, "ContainerRegistry", repositoryName + "/" + branchName);
        ContainerService containerService = new ContainerService(this, "ContainerService", branchName, CONTAINER_PORT, HEALTH_CHECKS, routingPriority, containerRegistry.getRegistry(), cluster, listener, zone);
        ContainerPipeline containerPipeline = new ContainerPipeline(this, "ContainerPipeline", getRegion(), getAccount(), repositoryName, branchName, CONTAINER_PORT, containerRegistry.getRegistry(), containerService.getService());
    }
}