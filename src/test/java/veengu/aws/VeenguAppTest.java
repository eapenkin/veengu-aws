package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void pipelineStack() {
        App app = new App();
        GitRepositoryStack gitRepositoryStack = new GitRepositoryStack(app, "test-git", "test-git");
        DockerRepositoryStack dockerRepositoryStack = new DockerRepositoryStack(app, "test-docker", "test-docker");
        FargateStack fargateStack = new FargateStack(app, "test-fargate", dockerRepositoryStack.getRepository());
        PipelineStack pipelineStack = new PipelineStack(app, "test-pipe", gitRepositoryStack.getRepository(), "master", dockerRepositoryStack.getRepository());
    }
}
