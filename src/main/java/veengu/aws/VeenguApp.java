package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {
    public static void main(final String[] args) {
        App app = new App();
        RepositoryStack repositoryStack = new RepositoryStack(app, "RepositoryStack", "scala-sandbox");
        PipelineStack pipelineStack = new PipelineStack(app, "PipelineStack", repositoryStack.getRepository(), "research/rest-api");
        app.synth();
    }
}
