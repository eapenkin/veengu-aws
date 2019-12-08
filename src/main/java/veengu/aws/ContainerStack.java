package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.route53.IHostedZone;

public class ContainerStack extends Stack {

    public ContainerStack(final Construct scope,
                          final String id,
                          final String repositoryName,
                          final String branchName,
                          final int containerPort,
                          final int routingPriority,
                          final Cluster cluster,
                          final ApplicationListener listener,
                          final IHostedZone zone) {
        super(scope, id);
        ContainerRegistry containerRegistry = new ContainerRegistry(this, "ContainerRegistry", repositoryName + "/" + branchName);
        ContainerService containerService = new ContainerService(this, "ContainerService", branchName, containerPort, "/health-checks", routingPriority, containerRegistry.getRegistry(), cluster, listener, zone);
        ContainerPipeline containerPipeline = new ContainerPipeline(this, "ContainerPipeline", getRegion(), getAccount(), repositoryName, branchName, containerPort, containerRegistry.getRegistry(), containerService.getService());
    }
}