package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {

    public static final int CONTAINER_PORT = 8080;
    public static final String REGISTRY_NAME = "veengu-back";
    public static final String REPOSITORY_NAME = "scala-sandbox";
    public static final String BRANCH_NAME = "develop";
    public static final String ZONE_NAME = "veengu.xyz";
    public static final String ZONE_ID = "Z3K66451X409D1";

    public static void main(final String[] args) {
        App app = new App();

        ///////////////////////////////////////////////////////////////////////////
        // App Stacks
        ///////////////////////////////////////////////////////////////////////////

        GitRepository gitRepository = new GitRepository(app, "GitRepository", REPOSITORY_NAME);
        DockerRegistry dockerRegistry = new DockerRegistry(app, "DockerRegistry", REGISTRY_NAME);
        PublishPipeline publishPipeline = new PublishPipeline(app, "PublishPipeline", BRANCH_NAME, CONTAINER_PORT, gitRepository.getRepository(), dockerRegistry.getRegistry());
        FargateCluster fargateCluster = new FargateCluster(app, "FargateCluster", CONTAINER_PORT, dockerRegistry.getRegistry());
        DomainName domainName = new DomainName(app, "DomainName", ZONE_NAME, ZONE_ID, fargateCluster.getLoadBalancer());

        ///////////////////////////////////////////////////////////////////////////
        // Stacks Dependencies
        ///////////////////////////////////////////////////////////////////////////

        // publishPipeline.addDependency(gitRepository); TODO why does it create cyclic dependency?
        publishPipeline.addDependency(dockerRegistry);
        fargateCluster.addDependency(publishPipeline);
        domainName.addDependency(fargateCluster);

        app.synth();
    }
}
