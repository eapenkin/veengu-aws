package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.Endpoint;

import java.util.List;

import static software.amazon.awscdk.core.Duration.days;
import static software.amazon.awscdk.core.SecretValue.plainText;
import static software.amazon.awscdk.services.ec2.InstanceClass.BURSTABLE2;
import static software.amazon.awscdk.services.ec2.InstanceSize.MICRO;
import static software.amazon.awscdk.services.ec2.Peer.anyIpv4;
import static software.amazon.awscdk.services.ec2.Port.tcp;
import static software.amazon.awscdk.services.rds.DatabaseInstanceEngine.MYSQL;

public class DatabaseStack extends Stack {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "enigma99";
    private static final String SCHEMA_USERNAME = "admin";
    private static final String SCHEMA_PASSWORD = "baobab88";

    private final Endpoint endpoint;

    public DatabaseStack(final Construct scope,
                         final String id,
                         final NetworkStack networkStack,
                         final int databasePort) {
        super(scope, id);

        SecurityGroup securityGroup = SecurityGroup.Builder
                .create(this, "DBSecurityGroup")
                .vpc(networkStack.getVpc())
                .allowAllOutbound(false)
                .build();
        securityGroup.addIngressRule(anyIpv4(), tcp(databasePort));

        InstanceType instanceClass = InstanceType.of(BURSTABLE2, MICRO);

        DatabaseInstance database = DatabaseInstance.Builder
                .create(this, "DB")
                .vpc(networkStack.getVpc())
                .vpcPlacement(networkStack.getPlacement())
                .instanceClass(instanceClass)
                .engine(MYSQL)
                .engineVersion("8.0.16")
                .securityGroups(List.of(securityGroup))
                .port(databasePort)
                .masterUsername(SCHEMA_USERNAME)
                .masterUserPassword(plainText(SCHEMA_PASSWORD))
                .allocatedStorage(20)
                .backupRetention(days(0))
                .deletionProtection(false)
                .build();

        endpoint = database.getInstanceEndpoint();
    }

    public String getAddress() {
        return endpoint.getSocketAddress();
    }

    public String getUsername() {
        return USERNAME;
    }

    public String getPassword() {
        return PASSWORD;
    }

    public String getSchemaUsername() {
        return SCHEMA_USERNAME;
    }

    public String getSchemaPassword() {
        return SCHEMA_PASSWORD;
    }
}