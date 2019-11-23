package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.BaseService;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;

public class FargateStack extends Stack {

    private BaseService service;

    public FargateStack(Construct scope, String id) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // VPC & Cluster
        ///////////////////////////////////////////////////////////////////////////

        Vpc vpc = Vpc.Builder
                .create(this, "Vpc")
                .build();

        Cluster cluster = Cluster.Builder
                .create(this, "EcsCluster")
                .vpc(vpc)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Task Image
        ///////////////////////////////////////////////////////////////////////////

        IRepository repository = Repository.fromRepositoryName(this, "EcrRepository", "veengu-service"); // TODO create repository and pass it as a parameter to PipelineStack

        ContainerImage image = ContainerImage.fromEcrRepository(repository);

        ApplicationLoadBalancedTaskImageOptions taskImage = ApplicationLoadBalancedTaskImageOptions.builder()
                .image(image)
                .containerPort(8080)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Fargate Service
        ///////////////////////////////////////////////////////////////////////////

        ApplicationLoadBalancedFargateService fargateService = ApplicationLoadBalancedFargateService.Builder
                .create(this, "FargateService")
                .cluster(cluster)
                .taskImageOptions(taskImage)
                .build();

        this.service = fargateService.getService();
    }

    public BaseService getService() {
        return service;
    }
}
