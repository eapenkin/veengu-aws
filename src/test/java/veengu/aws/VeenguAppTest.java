package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void test() {
        App app = new App();

        NetworkStack net = new NetworkStack(app, "NetStack", 9999, "veengu.xyz", "Z3K66451X409D1");

        DatabaseStack db1 = new DatabaseStack(app, "DatabaseStack1", net, 3333);
        ContainerStack stack1 = new ContainerStack(app, "TestStack1", net, db1, "TEST_REPO", "TEST_BRANCH_1", 10);
        StreamingStack stream1 = new StreamingStack(app, "TestStream1", net, db1);

        DatabaseStack db2 = new DatabaseStack(app, "DatabaseStack2", net, 3333);
        ContainerStack stack2 = new ContainerStack(app, "TestStack2", net, db2, "TEST_REPO", "TEST_BRANCH_2", 20);
        StreamingStack stream2 = new StreamingStack(app, "TestStream2", net, db2);
    }
}
