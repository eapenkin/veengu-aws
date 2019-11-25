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
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;

import java.util.Map;

import static java.lang.String.valueOf;

public class FargateCluster extends Stack {

    private BaseService service;

    private ApplicationLoadBalancer loadBalancer;

    public FargateCluster(final Construct scope,
                          final String id,
                          final int containerPort,
                          final IRepository registry) {
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

        Map<String, String> environmentVariables = Map.of(
                "CONTAINER_PORT", valueOf(containerPort));

        ApplicationLoadBalancedTaskImageOptions taskImage = ApplicationLoadBalancedTaskImageOptions.builder()
                .image(image)
                .containerPort(containerPort)
                .environment(environmentVariables)
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
        this.loadBalancer = fargateService.getLoadBalancer();

    }

    public BaseService getService() {
        return service;
    }

    public ApplicationLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }
}
