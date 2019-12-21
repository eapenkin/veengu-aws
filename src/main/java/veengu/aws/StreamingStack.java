package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dms.CfnEndpoint;
import software.amazon.awscdk.services.dms.CfnEndpoint.KinesisSettingsProperty;
import software.amazon.awscdk.services.dms.CfnReplicationInstance;
import software.amazon.awscdk.services.dms.CfnReplicationSubnetGroup;
import software.amazon.awscdk.services.dms.CfnReplicationTask;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.kinesis.Stream;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static software.amazon.awscdk.services.iam.ManagedPolicy.fromAwsManagedPolicyName;

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

        Role targetRole = Role.Builder
                .create(this, "TargetRole")
                .assumedBy(principal)
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

        CfnReplicationInstance instance = CfnReplicationInstance.Builder
                .create(this, "Instance")
                .allocatedStorage(20)
                .replicationInstanceClass("dms.t2.micro")
                .engineVersion("3.3.0")
                .replicationSubnetGroupIdentifier(subnetGroup.getRef())
                .publiclyAccessible(false)
                .build();

        instance.getNode().addDependency(vpcRole);
        subnetGroup.getNode().addDependency(vpcRole);

        ///////////////////////////////////////////////////////////////////////////
        // Source Endpoint
        ///////////////////////////////////////////////////////////////////////////

        CfnEndpoint source = CfnEndpoint.Builder
                .create(this, "Source")
                .endpointType("source")
                .engineName("mysql")
                .serverName(databaseStack.getHost())
                .port(databaseStack.getPort())
                .username(databaseStack.getSchemaUsername())
                .password(databaseStack.getSchemaPassword())
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Target Endpoint
        ///////////////////////////////////////////////////////////////////////////

        Stream stream = Stream.Builder
                .create(this, "Stream")
                .shardCount(1)
                .build();
        stream.grantWrite(targetRole);

        KinesisSettingsProperty kinesisSettings = KinesisSettingsProperty.builder()
                .messageFormat("json")
                .streamArn(stream.getStreamArn())
                .serviceAccessRoleArn(targetRole.getRoleArn())
                .build();

        CfnEndpoint target = CfnEndpoint.Builder
                .create(this, "Target")
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
                .sourceEndpointArn(source.getRef())
                .targetEndpointArn(target.getRef())
                .replicationTaskSettings("{\n" +
                        "   " +
                        "\"TargetMetadata\": {\n" +
                        "      \"SupportLobs\": true,\n" +
                        "      \"FullLobMode\": true,\n" +
                        "      \"LobChunkSize\": 64\n" +
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