package veengu.aws;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.customresources.Provider;
import software.amazon.awscdk.services.cloudformation.CustomResource;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.ILayerVersion;

import java.util.List;
import java.util.Map;

import static software.amazon.awscdk.core.Duration.minutes;
import static software.amazon.awscdk.core.RemovalPolicy.DESTROY;
import static software.amazon.awscdk.services.lambda.Code.fromInline;
import static software.amazon.awscdk.services.lambda.LayerVersion.fromLayerVersionArn;
import static software.amazon.awscdk.services.lambda.Runtime.NODEJS_10_X;
import static software.amazon.awscdk.services.lambda.Tracing.ACTIVE;

public class SqlStatement extends Construct {

    private static final String CODE =
            "var mysql = require('mysql');\n" +
            "var response = require('./cfn-response');\n" +
            "exports.handler = function (event, context) {\n" +
            "\n" +
            "    var connection = mysql.createConnection({\n" +
            "        socketPath: event.ResourceProperties.DatabaseSocket,\n" +
            "        user: event.ResourceProperties.DatabaseUsername,\n" +
            "        password: event.ResourceProperties.DatabasePassword,\n" +
            "        debug: true\n" +
            "    " +
            "});\n" +
            "\n" +
            "    connection.query(\n" +
            "        'call mysql.rds_set_configuration(\\'binlog retention hours\\', 24);',\n" +
            "        function (error) {\n" +
            "            if (error) {\n" +
            "                response.send(event, context, response.FAILED, {Error: error.code, Message: error.sqlMessage});\n" +
            "                return;\n" +
            "            }\n" +
            "        " +
            "});\n" +
            "\n" +
            "    response.send(event, context, response.SUCCESS);\n" +
            "    connection.destroy();\n" +
            "}";

    public SqlStatement(final Construct scope,
                        final String id,
                        final NetworkStack networkStack,
                        final DatabaseStack databaseStack) {
        super(scope, id);

        ///////////////////////////////////////////////////////////////////////////
        // Lambda Provider
        ///////////////////////////////////////////////////////////////////////////

        ILayerVersion layer = fromLayerVersionArn(this, "Layer", "arn:aws:lambda:ap-south-1:784873595558:layer:mysql-layer:2");

        Function handler = Function.Builder
                .create(this, "Handler")
                .runtime(NODEJS_10_X)
                .handler("index.handler")
                .vpc(networkStack.getVpc())
                .vpcSubnets(networkStack.getSubnets())
                .layers(List.of(layer))
                .tracing(ACTIVE)
                .timeout(minutes(1))
                .code(fromInline(CODE))
                .build();

        Provider provider = Provider.Builder
                .create(this, "Provider")
                .onEventHandler(handler)
                .build();

        ///////////////////////////////////////////////////////////////////////////
        // Custom Resource
        ///////////////////////////////////////////////////////////////////////////

        CustomResource.Builder
                .create(this, "Resource")
                .provider(provider)
                .removalPolicy(DESTROY)
                .properties(Map.of(
                        "Key", "Value",
                        "DatabaseSocket", databaseStack.getSocket(),
                        "DatabaseUsername", databaseStack.getUsername(),
                        "DatabasePassword", databaseStack.getPassword()))
                .build();
    }
}
