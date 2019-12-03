package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.TagStatus;

import java.util.List;

public class DockerRegistry extends Construct {

    private Repository registry;

    public DockerRegistry(final Construct scope,
                          final String id,
                          final String name) {
        super(scope, id);

        LifecycleRule oneUntaggedImage = LifecycleRule.builder()
                .tagStatus(TagStatus.UNTAGGED)
                .maxImageCount(1)
                .build();

        this.registry = Repository.Builder
                .create(this, "DockerRegistry")
                .repositoryName(name)
                .lifecycleRules(List.of(oneUntaggedImage))
                .build();
    }

    public IRepository getRegistry() {
        return registry;
    }
}
