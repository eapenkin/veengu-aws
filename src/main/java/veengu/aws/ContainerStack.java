package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ecs.ICluster;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationLoadBalancer;

public class ContainerStack extends Stack {

    public static final String HEALTH_CHECKS = "/health-checks";

    public ContainerStack(Construct scope, String id, String repositoryName, String branchName, int internetPort, int containerPort, ICluster cluster, IApplicationLoadBalancer loadBalancer) {
        super(scope, id);
        DockerRegistry dockerRegistry = new DockerRegistry(this, "DockerRegistry", repositoryName);
        ContainerService containerService = new ContainerService(this, "FargateCluster", internetPort, containerPort, HEALTH_CHECKS, dockerRegistry.getRegistry(), cluster, loadBalancer);
        ContainerPipeline containerPipeline = new ContainerPipeline(this, "ContainerPipeline", getRegion(), getAccount(), repositoryName, branchName, containerPort, dockerRegistry.getRegistry(), containerService.getService());
    }
}
