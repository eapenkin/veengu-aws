package veengu.aws;

import org.junit.Test;
import software.amazon.awscdk.core.App;

public class VeenguAppTest {

    @Test
    public void test() {
        App app = new App();

        FargateCluster fargateCluster = new FargateCluster(app, "test-fargate", 8080);
        DomainName domainName = new DomainName(app, "test-domain", "test-zone", "test-zone_id", fargateCluster.getBalancer());

        GitRepository gitRepository = new GitRepository(app, "test-git", "test-git");
        DockerRegistry dockerRegistry = new DockerRegistry(app, "test-docker", "test-docker");
        ContainerPipeline containerPipeline = new ContainerPipeline(app, "test-pipe", "test-branch", 8080, gitRepository.getRepository(), dockerRegistry.getRegistry(), fargateCluster.getService());
    }
}
