package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;

public class VeenguStack extends Stack {

    public static final int CONTAINER_PORT = 8080;
    public static final String REGISTRY_NAME = "veengu-back";
    public static final String ZONE_NAME = "veengu.xyz";
    public static final String ZONE_ID = "Z3K66451X409D1";

    public VeenguStack(Construct scope, String id, String repositoryName, String branchName) {
        super(scope, id);
        FargateClusterV2 fargateCluster = new FargateClusterV2(this, "FargateCluster", CONTAINER_PORT);
        DomainName domainName = new DomainName(this, "DomainName", ZONE_NAME, ZONE_ID, fargateCluster.getBalancer());
        DockerRegistry dockerRegistry = new DockerRegistry(this, "DockerRegistry", REGISTRY_NAME);
        ContainerPipeline containerPipeline = new ContainerPipeline(this, "ContainerPipeline", getRegion(), getAccount(), repositoryName, branchName, CONTAINER_PORT, dockerRegistry.getRegistry(), fargateCluster.getService());
    }
}
