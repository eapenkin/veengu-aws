package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void pipelineStack() {
        App app = new App();
        RepositoryStack repositoryStack = new RepositoryStack(app, "test-repo", "test-repo");
        PipelineStack pipelineStack = new PipelineStack(app, "test-pipe", repositoryStack.getRepository(), "master");
    }
}
