package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

import java.io.IOException;

public class VeenguAppTest {

    @Test
    public void pipelineStack() throws IOException {
        App app = new App();
        new PipelineStack(app, "test", "code-repository", "master");
    }
}
