package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;

public class ContainerStack extends Stack {

    private static final String HEALTH_CHECKS = "/health-checks";
    private static final int CONTAINER_PORT = 8080;

    public ContainerStack(final Construct scope,
                          final String id,
                          final NetworkStack networkStack,
                          final DatabaseStack databaseStack,
                          final String repositoryName,
                          final String branchName,
                          final int routingPriority) {
        super(scope, id);
        ContainerRegistry containerRegistry = new ContainerRegistry(this, "ContainerRegistry", repositoryName + "/" + branchName);
        ContainerService containerService = new ContainerService(this, "ContainerService", branchName, CONTAINER_PORT, HEALTH_CHECKS, routingPriority, databaseStack, containerRegistry.getRegistry(), networkStack.getCluster(), networkStack.getPlacement(), networkStack.getZone(), networkStack.getListener());
        ContainerPipeline containerPipeline = new ContainerPipeline(this, "ContainerPipeline", getRegion(), getAccount(), repositoryName, branchName, CONTAINER_PORT, containerRegistry.getRegistry(), containerService.getService());
    }
}