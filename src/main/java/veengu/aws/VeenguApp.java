package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {

    public static final int CONTAINER_PORT = 8080;
    public static final String REGISTRY_NAME = "veengu-back";
    public static final String REPOSITORY_NAME = "veengu-back";
    public static final String BRANCH_NAME = "develop";
    public static final String ZONE_NAME = "veengu.xyz";
    public static final String ZONE_ID = "Z3K66451X409D1";

    public static void main(final String[] args) {
        App app = new App();

        FargateCluster fargateCluster = new FargateCluster(app, "FargateCluster", CONTAINER_PORT);
        DomainName domainName = new DomainName(app, "DomainName", ZONE_NAME, ZONE_ID, fargateCluster.getBalancer());

        GitRepository gitRepository = new GitRepository(app, "GitRepository", REPOSITORY_NAME);
        DockerRegistry dockerRegistry = new DockerRegistry(app, "DockerRegistry", REGISTRY_NAME);
        PublishPipeline publishPipeline = new PublishPipeline(app, "PublishPipeline", BRANCH_NAME, CONTAINER_PORT, gitRepository.getRepository(), dockerRegistry.getRegistry(), fargateCluster.getService());

        app.synth();
    }
}
