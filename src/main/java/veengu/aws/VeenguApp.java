package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {
    public static void main(final String[] args) {
        App app = new App();

        ///////////////////////////////////////////////////////////////////////////
        // App Stacks
        ///////////////////////////////////////////////////////////////////////////

        GitRepository gitRepository = new GitRepository(app, "GitRepository", "scala-sandbox");
        DockerRegistry dockerRegistry = new DockerRegistry(app, "DockerRegistry", "scala-sandbox");
        PublishPipeline publishPipeline = new PublishPipeline(app, "PublishPipeline", gitRepository.getRepository(), "research/rest-api", dockerRegistry.getRegistry());
        FargateCluster fargateCluster = new FargateCluster(app, "FargateCluster", dockerRegistry.getRegistry());

        ///////////////////////////////////////////////////////////////////////////
        // Stacks Dependencies
        ///////////////////////////////////////////////////////////////////////////

        publishPipeline.addDependency(gitRepository);
        publishPipeline.addDependency(dockerRegistry);
        fargateCluster.addDependency(publishPipeline);

        app.synth();
    }
}
