package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.Endpoint;
import software.amazon.awscdk.services.rds.ParameterGroup;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static software.amazon.awscdk.core.Duration.days;
import static software.amazon.awscdk.core.RemovalPolicy.DESTROY;
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

        ParameterGroup parameterGroup = ParameterGroup.Builder
                .create(this, "ParameterGroup")
                .family("mysql5.7")
                .parameters(new TreeMap<>(Map.of(
                        "character_set_server", "utf8mb4",
                        "collation_server", "utf8mb4_bin",
                        "innodb_log_buffer_size", "8388608",
                        "max_allowed_packet", "8388608",
                        "binlog_format", "ROW",
                        "binlog_checksum", "NONE",
                        "binlog_row_image", "FULL",
                        "innodb_flush_log_at_trx_commit", "1",
                        "sync_binlog", "1")))
                .build();

        InstanceType instanceClass = InstanceType.of(BURSTABLE2, MICRO);

        DatabaseInstance database = DatabaseInstance.Builder
                .create(this, "DB")
                .vpc(networkStack.getVpc())
                .vpcPlacement(networkStack.getSubnets())
                .instanceClass(instanceClass)
                .engine(MYSQL)
                .engineVersion("5.7.26")
                .securityGroups(List.of(securityGroup))
                .port(databasePort)
                .masterUsername(SCHEMA_USERNAME)
                .masterUserPassword(plainText(SCHEMA_PASSWORD))
                .allocatedStorage(20)
                .parameterGroup(parameterGroup)
                .backupRetention(days(1))
                .removalPolicy(DESTROY)
                .deleteAutomatedBackups(true)
                .deletionProtection(false)
                .build();

        endpoint = database.getInstanceEndpoint();
    }

    public String getHost() {
        return endpoint.getHostname();
    }

    public Number getPort() {
        return endpoint.getPort();
    }

    public String getSocket() {
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