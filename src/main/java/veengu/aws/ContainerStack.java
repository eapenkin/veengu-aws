package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ecs.ICluster;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationLoadBalancer;

public class ContainerStack extends Stack {

    public static final String HEALTH_CHECKS = "/health-checks";

    public ContainerStack(Construct scope, String id, String repositoryName, String branchName, int internetPort, int containerPort, ICluster cluster, IApplicationLoadBalancer loadBalancer) {
        super(scope, id);
        ContainerRegistry containerRegistry = new ContainerRegistry(this, "ContainerRegistry", repositoryName);
        ContainerService containerService = new ContainerService(this, "ContainerService", internetPort, containerPort, HEALTH_CHECKS, containerRegistry.getRegistry(), cluster, loadBalancer);
        ContainerPipeline containerPipeline = new ContainerPipeline(this, "ContainerPipeline", getRegion(), getAccount(), repositoryName, branchName, containerPort, containerRegistry.getRegistry(), containerService.getService());
    }
}
