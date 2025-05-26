package co.verze.genai.mcpclientserver.cdk;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.CompositePrincipal;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McpClientServerCdkStack extends Stack {

    public static final String WEATHER_API_KEY = "WEATHER_API_KEY";

    public McpClientServerCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public McpClientServerCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // expect parameter to be passed in via CDK command
        Map<String, String> environment = Map.of(WEATHER_API_KEY, tryGetContext(WEATHER_API_KEY));

        // Find Lambda deployment package
        File mcpClientAsset = GetFileThatStartsWith("../aws-bedrock-mcp-client/target", "aws-bedrock-mcp-client");
        if (mcpClientAsset == null)
            throw new RuntimeException("Unable to find Lambda code file in target directory that starts with 'aws-bedrock-mcp-client'");

        // Create the Lambda function role with essential policies
        Role lambdaRole = Role.Builder.create(this, "aws-bedrock-mcp-client-server-role")
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"),
                        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchLogsFullAccess"),
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonBedrockFullAccess")))
                .assumedBy(new CompositePrincipal(
                        new ServicePrincipal("edgelambda.amazonaws.com"),
                        new ServicePrincipal("lambda.amazonaws.com")))
                .build();


        // Create the Lambda function
        Function mcpClientFunction = Function.Builder.create(this, "McpClientFunction")
                .runtime(Runtime.JAVA_21)
                .code(Code.fromAsset(mcpClientAsset.getAbsolutePath())) // Directory containing the JAR files
                .handler("co.verze.genai.mcpclient.AwsLambdaHandler::handleRequest")
                .role(lambdaRole)
                .memorySize(1024)
                .timeout(Duration.seconds(30))
                .environment(environment)
                .build();

        // Create the API
        RestApi api = RestApi.Builder.create(this, "McpClientApi")
                .restApiName("MCP Client API")
                .description("API Gateway for MCP Client")
                .binaryMediaTypes(List.of()) // No binary types
                .build();

        // Create Lambda integration that explicitly converts to text
        LambdaIntegration lambdaIntegration = LambdaIntegration.Builder.create(mcpClientFunction)
                .proxy(true)
                .contentHandling(ContentHandling.CONVERT_TO_TEXT)
                .build();

        // Add proxy resource
        api.getRoot().addProxy(ProxyResourceOptions.builder()
                .defaultIntegration(lambdaIntegration)
                .anyMethod(true)
                .build());

        // Output the API Gateway URL
        CfnOutput.Builder.create(this, "ApiUrl")
                .description("URL of the API Gateway")
                .value(api.getUrl())
                .build();
    }

    private static File GetFileThatStartsWith(String path, String filePattern) {
        File fileThatStartsWith = null;
        File folder = new File(path);

        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (final File listOfFile : listOfFiles) {
                if (listOfFile.isFile() && listOfFile.getName().startsWith(filePattern)) {
                    fileThatStartsWith = listOfFile;
                    break;
                }
            }
        }

        return fileThatStartsWith;
    }

    private String tryGetContext(String key) {
        String value = (String)this.getNode().tryGetContext(key);

        if (value == null) {
            throw new RuntimeException("Expected context value missing for '" + key + "'");
        }
        return value;
    }
}
