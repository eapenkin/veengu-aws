package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {

    public static final String REPOSITORY_NAME = "veengu-back";
    public static final String DEVELOP_BRANCH = "develop";
    public static final int INTERNET_PORT = 80;
    public static final int CONTAINER_PORT = 8080;

    public static void main(final String[] args) {
        App app = new App();
        NetworkStack networkStack = new NetworkStack(app, "VeenguNetwork");
        ContainerStack developStack = new ContainerStack(app, "DevelopBranch", REPOSITORY_NAME, DEVELOP_BRANCH, INTERNET_PORT, CONTAINER_PORT, networkStack.getCluster(), networkStack.getBalancer());
        app.synth();
    }
}
