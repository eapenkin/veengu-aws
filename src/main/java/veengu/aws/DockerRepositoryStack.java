package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;

public class DockerRepositoryStack extends Stack {

    private Repository repository;

    public DockerRepositoryStack(Construct scope, String id, String name) {
        super(scope, id);
        this.repository = Repository.Builder
                .create(this, "DockerRepository")
                .repositoryName(name)
                .build();
    }

    public IRepository getRepository() {
        return repository;
    }
}
