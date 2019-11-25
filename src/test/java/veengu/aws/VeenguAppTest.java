package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void pipelineStack() {
        App app = new App();
        GitRepository gitRepository = new GitRepository(app, "test-git", "test-git");
        DockerRegistry dockerRegistry = new DockerRegistry(app, "test-docker", "test-docker");
        PublishPipeline publishPipeline = new PublishPipeline(app, "test-pipe", "test-branch", gitRepository.getRepository(), dockerRegistry.getRegistry());
        FargateCluster fargateCluster = new FargateCluster(app, "test-fargate", dockerRegistry.getRegistry());
        DomainName domainName = new DomainName(app, "test-domain", "test-zone", "test-zone_id", fargateCluster.getLoadBalancer());
    }
}
