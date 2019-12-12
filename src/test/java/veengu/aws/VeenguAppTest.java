package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void test() {
        App app = new App();

        NetworkStack net = new NetworkStack(app, "BaseStack", 9999, "veengu.xyz", "Z3K66451X409D1");

        DatabaseStack db1 = new DatabaseStack(app, "DatabaseStack1", 3333, net.getVpc(), net.getPlacement());
        ContainerStack stack1 = new ContainerStack(app, "TestStack1", "TEST_REPO", "TEST_BRANCH_1", 10, db1.getEndpoint(), net.getCluster(), net.getPlacement(), net.getZone(), net.getListener());

        DatabaseStack db2 = new DatabaseStack(app, "DatabaseStack2", 3333, net.getVpc(), net.getPlacement());
        ContainerStack stack2 = new ContainerStack(app, "TestStack2", "TEST_REPO", "TEST_BRANCH_2", 20, db2.getEndpoint(), net.getCluster(), net.getPlacement(), net.getZone(), net.getListener());
    }
}
