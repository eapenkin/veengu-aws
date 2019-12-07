package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;

public class VeenguStack extends Stack {

    public static final String ZONE_NAME = "veengu.xyz";
    public static final String ZONE_ID = "Z3K66451X409D1";
    public static final String HEALTH_CHECKS = "/health-checks";

    public VeenguStack(Construct scope, String id, String repositoryName, String branchName, int internetPort, int containerPort) {
        super(scope, id);
        DockerRegistry dockerRegistry = new DockerRegistry(this, "DockerRegistry", repositoryName);
        FargateClusterV2 fargateCluster = new FargateClusterV2(this, "FargateCluster", internetPort, containerPort, HEALTH_CHECKS, dockerRegistry.getRegistry());
        DomainName domainName = new DomainName(this, "DomainName", ZONE_NAME, ZONE_ID, fargateCluster.getBalancer());
        ContainerPipeline containerPipeline = new ContainerPipeline(this, "ContainerPipeline", getRegion(), getAccount(), repositoryName, branchName, containerPort, dockerRegistry.getRegistry(), fargateCluster.getService());
    }
}
