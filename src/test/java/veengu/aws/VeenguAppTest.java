package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void test() {
        App app = new App();
        ContainerStack stack = new ContainerStack(app, "TestStack", "TEST_REPO", "TEST_BRANCH", 80, 8080);
    }
}
