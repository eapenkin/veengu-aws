package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void test() {
        App app = new App();
        NetworkStack networkStack = new NetworkStack(app, "BaseStack", 9999);
        ContainerStack stack = new ContainerStack(app, "TestStack", "TEST_REPO", "TEST_BRANCH", 8080, 10, networkStack.getCluster(), networkStack.getListener(), networkStack.getZone());
    }
}
