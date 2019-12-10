package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {

    private static final int INTERNET_PORT = 80;
    private static final String INTERNET_DOMAIN = "veengu.xyz";
    private static final String DOMAIN_ID = "Z3K66451X409D1";

    private static final String VEENGU_REPO = "veengu-back";
    private static final String DEMO_BRANCH = "demo";
    private static final String DEVELOP_BRANCH = "develop";

    public static void main(final String[] args) {
        App app = new App();
        NetworkStack networkStack = new NetworkStack(app, "NetworkStack", INTERNET_PORT, INTERNET_DOMAIN, DOMAIN_ID);
        DatabaseStack databaseStack = new DatabaseStack(app, "DatabaseStack", networkStack.getVpc(), networkStack.getPlacement());
        ContainerStack demoStack = new ContainerStack(app, "DemoContainer", VEENGU_REPO, DEMO_BRANCH, 10, networkStack.getCluster(), networkStack.getPlacement(), networkStack.getZone(), networkStack.getListener());
        ContainerStack developStack = new ContainerStack(app, "DevelopContainer", VEENGU_REPO, DEVELOP_BRANCH, 20, networkStack.getCluster(), networkStack.getPlacement(), networkStack.getZone(), networkStack.getListener());
        app.synth();
    }
}
