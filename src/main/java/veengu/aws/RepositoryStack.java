package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.codecommit.IRepository;
import software.amazon.awscdk.services.codecommit.Repository;

public class RepositoryStack extends Stack {

    private IRepository repository;

    public RepositoryStack(final Construct scope, final String id, final String name) {
        super(scope, id);
        this.repository = Repository.fromRepositoryName(this, "CodeRepository", name);
    }

    public IRepository getRepository() {
        return repository;
    }
}
