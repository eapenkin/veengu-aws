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

        NetworkStack net = new NetworkStack(app, "Network", INTERNET_PORT, INTERNET_DOMAIN, DOMAIN_ID);

        ///////////////////////////////////////////////////////////////////////////
        // Demo Environment
        ///////////////////////////////////////////////////////////////////////////

        DatabaseStack demoDb = new DatabaseStack(app, "DemoDatabase", net, DATABASE_PORT);
        ContainerStack demoApp = new ContainerStack(app, "DemoContainer", net, demoDb, VEENGU_REPO, DEMO_BRANCH, 10);
        StreamingStack demoStream = new StreamingStack(app, "DemoStream", net, demoDb);

        ///////////////////////////////////////////////////////////////////////////
        // Develop Environment
        ///////////////////////////////////////////////////////////////////////////

        DatabaseStack devDb = new DatabaseStack(app, "DevelopDatabase", net, DATABASE_PORT);
        ContainerStack devApp = new ContainerStack(app, "DevelopContainer", net, devDb, VEENGU_REPO, DEVELOP_BRANCH, 20);
        StreamingStack devStream = new StreamingStack(app, "DevelopStream", net, devDb);

        app.synth();
    }
}
