package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dms.CfnEndpoint;
import software.amazon.awscdk.services.dms.CfnEndpoint.KinesisSettingsProperty;
import software.amazon.awscdk.services.dms.CfnEndpoint.S3SettingsProperty;
import software.amazon.awscdk.services.dms.CfnReplicationInstance;
import software.amazon.awscdk.services.dms.CfnReplicationSubnetGroup;
import software.amazon.awscdk.services.dms.CfnReplicationTask;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.kinesis.Stream;
import software.amazon.awscdk.services.s3.Bucket;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static software.amazon.awscdk.core.RemovalPolicy.DESTROY;
import static software.amazon.awscdk.services.iam.ManagedPolicy.fromAwsManagedPolicyName;
import static software.amazon.awscdk.services.s3.BlockPublicAccess.BLOCK_ALL;

public class StreamingStack extends Stack {

    public StreamingStack(final Construct scope,
                          final String id,
                          final NetworkStack networkStack,
                          final DatabaseStack databaseStack) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // IAM Roles
        ///////////////////////////////////////////////////////////////////////////

        ServicePrincipal principal = ServicePrincipal.Builder
                .create("dms.amazonaws.com")
                .build();

        Role vpcRole = Role.Builder
                .create(this, "VPCRole")
                .assumedBy(principal)
                .roleName("dms-vpc-role")
                .build();
        vpcRole.addManagedPolicy(fromAwsManagedPolicyName("service-role/AmazonDMSVPCManagementRole"));

        Role logsRole = Role.Builder
                .create(this, "LogsRole")
                .assumedBy(principal)
                .roleName("dms-cloudwatch-logs-role")
                .build();
        logsRole.addManagedPolicy(fromAwsManagedPolicyName("service-role/AmazonDMSCloudWatchLogsRole"));

        ///////////////////////////////////////////////////////////////////////////
        // Replication Instance
        ///////////////////////////////////////////////////////////////////////////

        List<String> subnets = networkStack
                .getVpc()
                .getIsolatedSubnets().stream()
                .map(ISubnet::getSubnetId)
                .collect(toList());

        CfnReplicationSubnetGroup subnetGroup = CfnReplicationSubnetGroup.Builder
                .create(this, "SubnetGroup")
                .subnetIds(subnets)
                .replicationSubnetGroupDescription("Isolated Subnets")
                .build();

        SecurityGroup securityGroup = SecurityGroup.Builder
                .create(this, "SecurityGroup")
                .vpc(networkStack.getVpc())
                .allowAllOutbound(true)
                .build();

        CfnReplicationInstance instance = CfnReplicationInstance.Builder
                .create(this, "Instance")
                .allocatedStorage(20)
                .replicationInstanceClass("dms.t2.micro")
                .engineVersion("3.3.0")
                .replicationSubnetGroupIdentifier(subnetGroup.getRef())
                .vpcSecurityGroupIds(List.of(securityGroup.getSecurityGroupId()))
                .publiclyAccessible(false)
                .build();

        instance.getNode().addDependency(vpcRole);
        subnetGroup.getNode().addDependency(vpcRole);

        ///////////////////////////////////////////////////////////////////////////
        // MySQL Endpoint
        ///////////////////////////////////////////////////////////////////////////

        CfnEndpoint databaseSource = CfnEndpoint.Builder
                .create(this, "DatabaseSource")
                .endpointType("source")
                .engineName("mysql")
                .serverName(databaseStack.getHost())
                .port(databaseStack.getPort())
                .username(databaseStack.getSchemaUsername())
                .password(databaseStack.getSchemaPassword())
                .extraConnectionAttributes("afterConnectScript=call mysql.rds_set_configuration('binlog retention hours', 48);")
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // S3 Endpoint
        ///////////////////////////////////////////////////////////////////////////

        Role bucketRole = Role.Builder
                .create(this, "BucketRole")
                .assumedBy(principal)
                .build();

        Bucket bucket = Bucket.Builder
                .create(this, "Bucket")
                .blockPublicAccess(BLOCK_ALL)
                .removalPolicy(DESTROY)
                .build();
        bucket.grantReadWrite(bucketRole);

        S3SettingsProperty s3Settings = S3SettingsProperty.builder()
                .bucketName(bucket.getBucketName())
                .serviceAccessRoleArn(bucketRole.getRoleArn())
                .build();

        CfnEndpoint bucketTarget = CfnEndpoint.Builder
                .create(this, "BucketTarget")
                .endpointType("target")
                .engineName("s3")
                .s3Settings(s3Settings)
                .extraConnectionAttributes(
                        "timestampColumnName=committed_at;" +
                        "addColumnName=true;")
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Kinesis Endpoint
        ///////////////////////////////////////////////////////////////////////////

        Role streamRole = Role.Builder
                .create(this, "StreamRole")
                .assumedBy(principal)
                .build();

        Stream stream = Stream.Builder
                .create(this, "Stream")
                .shardCount(1)
                .build();
        stream.grantWrite(streamRole);

        KinesisSettingsProperty kinesisSettings = KinesisSettingsProperty.builder()
                .messageFormat("json")
                .streamArn(stream.getStreamArn())
                .serviceAccessRoleArn(streamRole.getRoleArn())
                .build();

        CfnEndpoint streamTarget = CfnEndpoint.Builder
                .create(this, "StreamTarget")
                .endpointType("target")
                .engineName("kinesis")
                .kinesisSettings(kinesisSettings)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Replication Task
        ///////////////////////////////////////////////////////////////////////////

        CfnReplicationTask task = CfnReplicationTask.Builder
                .create(this, "Task")
                .migrationType("cdc")
                .replicationInstanceArn(instance.getRef())
                .sourceEndpointArn(databaseSource.getRef())
                .targetEndpointArn(bucketTarget.getRef())
                .replicationTaskSettings("{\n" +
                        "   " +
                        "\"TargetMetadata\": {\n" +
                        "      \"SupportLobs\": true,\n" +
                        "      \"FullLobMode\": false,\n" +
                        "      \"LimitedSizeLobMode\": true,\n" +
                        "      \"LobMaxSize\": 96,\n" +
                        "      \"InlineLobMaxSize\": 0\n" +
                        "   " +
                        "},\n" +
                        "   \"Logging\": {\n" +
                        "      \"EnableLogging\": true\n" +
                        "   }\n" +
                        "}")
                .tableMappings("{\n" +
                        "   \"rules\": [\n" +
                        "      {\n" +
                        "         \"rule-type\": \"selection\",\n" +
                        "         \"rule-id\": \"1\",\n" +
                        "         \"rule-name\": \"xtc_vngu\",\n" +
                        "         \"object-locator\": {\n" +
                        "            \"schema-name\": \"xtc_vngu\",\n" +
                        "            \"table-name\": \"%\"\n" +
                        "         },\n" +
                        "         \"rule-action\": \"include\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "         \"rule-type\": \"selection\",\n" +
                        "         \"rule-id\": \"2\",\n" +
                        "         \"rule-name\": \"are_enbd\",\n" +
                        "         \"object-locator\": {\n" +
                        "            \"schema-name\": \"are_enbd\",\n" +
                        "            \"table-name\": \"%\"\n" +
                        "         },\n" +
                        "         \"rule-action\": \"include\"\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}")
                .build();
    }
}