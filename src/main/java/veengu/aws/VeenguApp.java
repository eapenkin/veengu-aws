package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {

    public static final int CONTAINER_PORT = 8080;
    public static final String REGISTRY_NAME = "scala-sandbox";
    public static final String REPOSITORY_NAME = "scala-sandbox";
    public static final String BRANCH_NAME = "research/rest-api";

    public static void main(final String[] args) {
        App app = new App();

        ///////////////////////////////////////////////////////////////////////////
        // App Stacks
        ///////////////////////////////////////////////////////////////////////////

        GitRepository gitRepository = new GitRepository(app, "GitRepository", REPOSITORY_NAME);
        DockerRegistry dockerRegistry = new DockerRegistry(app, "DockerRegistry", REGISTRY_NAME);
        PublishPipeline publishPipeline = new PublishPipeline(app, "PublishPipeline", gitRepository.getRepository(), BRANCH_NAME, dockerRegistry.getRegistry());
        FargateCluster fargateCluster = new FargateCluster(app, "FargateCluster", dockerRegistry.getRegistry());

        ///////////////////////////////////////////////////////////////////////////
        // Stacks Dependencies
        ///////////////////////////////////////////////////////////////////////////

        // publishPipeline.addDependency(gitRepository); TODO why it creates cyclic dependency?
        publishPipeline.addDependency(dockerRegistry);
        fargateCluster.addDependency(publishPipeline);

        app.synth();
    }
}
