package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void pipelineStack() {
        App app = new App();
        GitRepository gitRepository = new GitRepository(app, "test-git", "test-git");
        DockerRegistry dockerRegistry = new DockerRegistry(app, "test-docker", "test-docker");
        FargateCluster fargateCluster = new FargateCluster(app, "test-fargate", dockerRegistry.getRegistry());
        PublishPipeline publishPipeline = new PublishPipeline(app, "test-pipe", gitRepository.getRepository(), "master", dockerRegistry.getRegistry());
    }
}
