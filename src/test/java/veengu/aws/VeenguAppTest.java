package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void test() {
        App app = new App();
        NetworkStack networkStack = new NetworkStack(app, "BaseStack");
        ContainerStack stack = new ContainerStack(app, "TestStack", "TEST_REPO", "TEST_BRANCH", 80, 8080, networkStack.getCluster(), networkStack.getBalancer());
    }
}
