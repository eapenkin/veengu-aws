package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;

import java.util.List;

import static software.amazon.awscdk.core.Duration.days;
import static software.amazon.awscdk.services.ecr.TagStatus.ANY;

public class ContainerRegistry extends Construct {

    private final Repository registry;

    public ContainerRegistry(final Construct scope,
                             final String id,
                             final String name) {
        super(scope, id);

        LifecycleRule oneDayImage = LifecycleRule.builder()
                .tagStatus(ANY)
                .maxImageAge(days(1))
                .build();

        registry = Repository.Builder
                .create(this, "Registry")
                .repositoryName(name)
                .lifecycleRules(List.of(oneDayImage))
                .build();
    }

    public IRepository getRegistry() {
        return registry;
    }
}
