package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecs.BaseService;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;

public class FargateCluster extends Stack {

    private BaseService service;

    public FargateCluster(Construct scope, String id, IRepository registry) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // VPC & Cluster
        ///////////////////////////////////////////////////////////////////////////

        Vpc vpc = Vpc.Builder
                .create(this, "Vpc")
                .maxAzs(2)
                .build();

        Cluster cluster = Cluster.Builder
                .create(this, "FargateCluster")
                .vpc(vpc)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Task Image
        ///////////////////////////////////////////////////////////////////////////

        ContainerImage image = ContainerImage.fromEcrRepository(registry);

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
