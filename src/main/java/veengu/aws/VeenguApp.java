package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {

    public static final String REPOSITORY_NAME = "veengu-back";
    public static final String DEVELOP_BRANCH = "develop";

    public static void main(final String[] args) {
        App app = new App();
        VeenguStack stack = new VeenguStack(app, "DevStack", REPOSITORY_NAME, DEVELOP_BRANCH);
        app.synth();
    }
}
