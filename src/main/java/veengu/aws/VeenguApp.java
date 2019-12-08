package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {

    public static final int INTERNET_PORT = 80;

    public static void main(final String[] args) {
        App app = new App();
        NetworkStack networkStack = new NetworkStack(app, "VeenguNetwork", INTERNET_PORT);
        ContainerStack developStack = new ContainerStack(app, "DevelopBranch", "veengu-back", "develop", 8080, 15, networkStack.getCluster(), networkStack.getListener(), networkStack.getZone());
        ContainerStack demoStack = new ContainerStack(app, "DemoBranch", "veengu-back", "demo", 8080, 5, networkStack.getCluster(), networkStack.getListener(), networkStack.getZone());
        app.synth();
    }
}
