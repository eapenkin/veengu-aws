package veengu.aws;

import software.amazon.awscdk.core.App;

public class VeenguApp {

    private static final int DATABASE_PORT = 3306;
    private static final int INTERNET_PORT = 80;
    private static final String INTERNET_DOMAIN = "veengu.xyz";
    private static final String DOMAIN_ID = "Z3K66451X409D1";

    private static final String VEENGU_REPO = "veengu-back";
    private static final String DEMO_BRANCH = "demo";
    private static final String DEVELOP_BRANCH = "develop";

    public static void main(final String[] args) {
        App app = new App();

        ///////////////////////////////////////////////////////////////////////////
        // Common Components
        ///////////////////////////////////////////////////////////////////////////
        NetworkStack net = new NetworkStack(app, "NetworkStack", INTERNET_PORT, INTERNET_DOMAIN, DOMAIN_ID);

        ///////////////////////////////////////////////////////////////////////////
        // Demo Environment
        ///////////////////////////////////////////////////////////////////////////
        DatabaseStack demoDb = new DatabaseStack(app, "DemoDatabase", DATABASE_PORT, net.getVpc(), net.getPlacement());
        ContainerStack demoApp = new ContainerStack(app, "DemoContainer", VEENGU_REPO, DEMO_BRANCH, 10, demoDb.getEndpoint(), net.getCluster(), net.getPlacement(), net.getZone(), net.getListener());

        ///////////////////////////////////////////////////////////////////////////
        // Develop Environment
        ///////////////////////////////////////////////////////////////////////////
        DatabaseStack devDb = new DatabaseStack(app, "DevelopDatabase", DATABASE_PORT, net.getVpc(), net.getPlacement());
        ContainerStack devApp = new ContainerStack(app, "DevelopContainer", VEENGU_REPO, DEVELOP_BRANCH, 20, devDb.getEndpoint(), net.getCluster(), net.getPlacement(), net.getZone(), net.getListener());

        app.synth();
    }
}
