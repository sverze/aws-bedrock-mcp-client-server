package co.verze.genai.mcpclientserver.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class McpClientServerCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        // Define the environment (region and account)
        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        // Create the stack
        new McpClientServerCdkStack(app, "McpClientServerStack", StackProps.builder()
                .env(env)
                .description("MCP Client Server API and Lambda Stack")
                .build());

        app.synth();
    }
}
