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
        ContainerStack demoStack = new ContainerStack(app, "DemoBranch", VEENGU_REPO, DEMO_BRANCH, 10, networkStack.getCluster(), networkStack.getListener(), networkStack.getZone());
        ContainerStack developStack = new ContainerStack(app, "DevelopBranch", VEENGU_REPO, DEVELOP_BRANCH, 20, networkStack.getCluster(), networkStack.getListener(), networkStack.getZone());
        app.synth();
    }
}
