package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {
    public static void main(final String[] args) {
        App app = new App();
        GitRepositoryStack gitRepositoryStack = new GitRepositoryStack(app, "RepositoryStack", "scala-sandbox");
        DockerRepositoryStack dockerRepositoryStack = new DockerRepositoryStack(app, "DockerStack", "scala-sandbox");
        FargateStack fargateStack = new FargateStack(app, "FargateStack", dockerRepositoryStack.getRepository());
        PipelineStack pipelineStack = new PipelineStack(app, "PipelineStack", gitRepositoryStack.getRepository(), "research/rest-api", dockerRepositoryStack.getRepository());
        app.synth();
    }
}
