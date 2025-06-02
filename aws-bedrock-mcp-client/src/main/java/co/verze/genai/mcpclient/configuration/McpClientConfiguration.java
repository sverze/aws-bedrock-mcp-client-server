package co.verze.genai.mcpclient.configuration;

import java.io.File;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Qualifier;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.ClientMcpTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/**
 * Configuration for Model Context Protocol Client.
 */
@Configuration
public class McpClientConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(McpClientConfiguration.class);
    @Value("${aws.region}")
    private String awsRegion;
    @Value("${tools.weather-api-key}")
    private String weatherApiKey;

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean(name = "stdio", destroyMethod = "close")
    public McpSyncClient stdioMcpSyncClient(@Qualifier("stdioTransport") ClientMcpTransport transport) {
        logger.info("Initializing McpSyncClient for Stdio MCP Server");

        // Create a sync client with custom configuration
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .capabilities(ClientCapabilities.builder()
                        .roots(true)      // Enable roots capability
                        .sampling()       // Enable sampling capability
                        .build())
                .build();

        // Initialize connection
        client.initialize();

        return client;
    }

    @Bean(name = "stdioTransport")
    public ClientMcpTransport createStdioTransport() {
        logger.info("Creating StdioClientTransport");

        String mcpServerFilePattern = "aws-bedrock-mcp-server.*.jar";
        File mcpServerAsset = GetFileThatStartsWith("./aws-bedrock-mcp-server/target", mcpServerFilePattern);
        if (mcpServerAsset == null) {
            mcpServerAsset = GetFileThatStartsWith("../aws-bedrock-mcp-server/target", mcpServerFilePattern);
            if (mcpServerAsset == null) {
                mcpServerAsset = GetFileThatStartsWith("./lib", mcpServerFilePattern);
                if (mcpServerAsset == null) {
                    throw new RuntimeException("Unable to find AWS Bedrock MCP Server library code");
                }
            }
        }

        logger.info("Starting MCP server process with: {}", mcpServerAsset.getAbsolutePath());

        // Using StdioClientTransport with local MCP server jar
        ServerParameters params = ServerParameters.builder("java")
                .args(
                        "-DWEATHER_API_KEY=" + weatherApiKey,
                        "-Dspring.main.web-application-type=none",  // Disable web server
                        "-Dspring.main.banner-mode=off",
                        "-Dlogging.pattern.console=",  // Disable console logging pattern
                        "-Dlogging.file.name=aws-bedrock-mcp-server.log",
                        "-jar",
                        mcpServerAsset.getAbsolutePath()
                )
                .build();

        return new StdioClientTransport(params);
    }


    private static File GetFileThatStartsWith(String path, String filePattern) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles(file ->
                file.isFile() && file.getName().matches(filePattern)
        );
        return listOfFiles != null && listOfFiles.length > 0 ? listOfFiles[0] : null;
    }}
