package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void test() {
        App app = new App();
        VeenguStack stack = new VeenguStack(app, "TestStack", "TEST_REPO", "TEST_BRANCH", 80, 8080);
    }
}
