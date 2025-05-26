package co.verze.genai.mcpserver.configuration;

import co.verze.genai.mcpserver.tools.WebPageTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.verze.genai.mcpserver.tools.CalculatorTool;
import co.verze.genai.mcpserver.tools.WeatherTool;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransport;
import io.modelcontextprotocol.spec.ServerMcpTransport;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

@Configuration
public class McpServerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(McpServerConfiguration.class);

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public StdioServerTransport stdioServerTransport() {
        log.info("Creating StdioServerTransport");
        return new StdioServerTransport();
    }
    
    @Bean(destroyMethod = "close")
    public McpSyncServer mcpSyncServer(ServerMcpTransport transport,
                                       CalculatorTool calculatorTool,
                                       WeatherTool weatherTool,
                                       WebPageTool webPageTool) {
        log.info("Initializing McpSyncServer with transport: {}", transport);
        
        // Create a server with custom configuration
        McpSyncServer syncServer = McpServer.sync(transport)
            .serverInfo(new Implementation("my-server", "1.0.0"))
            .capabilities(ServerCapabilities.builder()
                .tools(true)         // Enable tool support
                .prompts(false)      // Change to false if not implementing prompts
                .resources(false,false)
                .logging()           // Enable logging support
                .build())
            .build();
        
        // Send initial logging notification
        syncServer.loggingNotification(LoggingMessageNotification.builder()
            .level(LoggingLevel.INFO)
            .logger("custom-logger")
            .data("Server initialized")
            .build());

        // Register the calculator tool
        var calculatorToolRegistration = new McpServerFeatures.SyncToolRegistration(
            calculatorTool.getToolDefinition(),
            calculatorTool
        );
        
        // Register the weather tool
        var weatherToolRegistration = new McpServerFeatures.SyncToolRegistration(
            weatherTool.getToolDefinition(),
            weatherTool
        );

        // Register the weather tool
        var webPageToolRegistration = new McpServerFeatures.SyncToolRegistration(
                webPageTool.getToolDefinition(),
                webPageTool
        );

        syncServer.addTool(calculatorToolRegistration);
        syncServer.addTool(weatherToolRegistration);
        syncServer.addTool(webPageToolRegistration);

        log.info("MCP Server initialized with capabilities: tools={}, prompts={}, resources={}",
                syncServer.getServerCapabilities().tools(),
                syncServer.getServerCapabilities().prompts(),
                syncServer.getServerCapabilities().resources());
        return syncServer;
    }
}