package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.DatabaseInstance;

import static software.amazon.awscdk.services.ec2.InstanceClass.BURSTABLE2;
import static software.amazon.awscdk.services.ec2.InstanceSize.MICRO;
import static software.amazon.awscdk.services.rds.DatabaseInstanceEngine.MYSQL;

public class DatabaseStack extends Stack {

    public DatabaseStack(final Construct scope,
                         final String id,
                         final Vpc vpc,
                         final SubnetSelection placement) {
        super(scope, id);

        DatabaseInstance.Builder
                .create(this, "Database")
                .engine(MYSQL)
                .engineVersion("8.0.16")
                .instanceClass(InstanceType.of(BURSTABLE2, MICRO))
                .vpc(vpc)
                .vpcPlacement(placement)
                .allocatedStorage(20)
                .deletionProtection(false)
                .masterUsername("test")
                .build();
    }
}