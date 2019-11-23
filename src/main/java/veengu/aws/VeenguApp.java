package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {
    public static void main(final String argv[]) {
        App app = new App();
        new PipelineStack(app, "PipelineStack", "scala-sandbox", "research/rest-api");
        app.synth();
    }
}
