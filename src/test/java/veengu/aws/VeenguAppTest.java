package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void test() {
        App app = new App();
        NetworkStack networkStack = new NetworkStack(app, "BaseStack", 9999, "veengu.xyz", "Z3K66451X409D1");
        ContainerStack stack1 = new ContainerStack(app, "TestStack1", "TEST_REPO", "TEST_BRANCH_1", 10, networkStack.getCluster(), networkStack.getListener(), networkStack.getZone());
        ContainerStack stack2 = new ContainerStack(app, "TestStack2", "TEST_REPO", "TEST_BRANCH_2", 20, networkStack.getCluster(), networkStack.getListener(), networkStack.getZone());
    }
}
