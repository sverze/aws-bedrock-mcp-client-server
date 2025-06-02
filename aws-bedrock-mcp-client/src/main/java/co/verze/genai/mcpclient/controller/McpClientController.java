package co.verze.genai.mcpclient.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.verze.genai.mcpclient.utils.DocumentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.swagger.v3.oas.annotations.Operation;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

@RestController
public class  McpClientController {
    private static final Logger logger = LoggerFactory.getLogger(McpClientController.class);
    private static final String MODEL_ID = "us.anthropic.claude-sonnet-4-20250514-v1:0";
    private static final int MAX_TURNS = 10;
    private final BedrockRuntimeClient bedrockClient;
    private final McpSyncClient stdioMcpServer;

    @Autowired
    public McpClientController(BedrockRuntimeClient bedrockClient, McpSyncClient stdioMcpServer) {
        this.bedrockClient = bedrockClient;
        this.stdioMcpServer = stdioMcpServer;
    }


    @GetMapping("/hello")
    @Operation(summary = "Hello World endpoint", description = "Returns a simple hello world message for testing purposes")
    public ResponseEntity<String> helloWorld() {
        logger.info("Hello World requested");
        return ResponseEntity.ok("Hello World from MCP Client!");
    }

    @GetMapping("/tools")
    @Operation(summary = "List tools from server", description = "Returns a list of available tools from the server")
    public ResponseEntity<ListToolsResult> listTools() {
        logger.info("Listing tools from server");
        ListToolsResult result = stdioMcpServer.listTools();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate endpoint", description = "Performs calculations using the calculate tool")
    public ResponseEntity<CallToolResult> calculate(
            @RequestParam String operation,
            @RequestParam double a,
            @RequestParam double b) {
        
        // Create a CallToolRequest with the "calculator" tool and parameters
        CallToolRequest toolRequest = new CallToolRequest(
            "calculator",  // Tool name
            Map.of(
                "operation", operation,
                "a", a,
                "b", b
            )  // Tool parameters
        );

        logger.info("Calculate requested: " + a + ", " + operation + ", " + b);

        // Call the tool and get the result
        CallToolResult result = stdioMcpServer.callTool(toolRequest);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/weather")
    @Operation(summary = "Weather endpoint", description = "Gets weather information using the weather tool")
    public ResponseEntity<CallToolResult> weather(
            @RequestParam String location,
            @RequestParam(required = false, defaultValue = "celsius") String format) {
        logger.info("Weather requested for location: {} with format: {}", location, format);
        
        // Create a CallToolRequest with the "get_current_weather" tool and parameters
        CallToolRequest toolRequest = new CallToolRequest(
            "get_current_weather",  // Tool name
            Map.of(
                "location", location,
                "format", format
            )  // Tool parameters
        );
        
        // Call the tool and get the result
        CallToolResult result = stdioMcpServer.callTool(toolRequest);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/webpage")
    @Operation(summary = "Web page endpoint", description = "Gets web page as markdown")
    public ResponseEntity<CallToolResult> webPage(
            @RequestParam String url) {
        logger.info("Get web page as markdown: {}", url);

        // Create a CallToolRequest with the "get_current_weather" tool and parameters
        CallToolRequest toolRequest = new CallToolRequest(
                "get_web_page",  // Tool name
                Map.of(
                        "url", url
                )  // Tool parameters
        );

        // Call the tool and get the result
        CallToolResult result = stdioMcpServer.callTool(toolRequest);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/query")
    @Operation(summary = "Query endpoint", description = "Queries bedrock model and uses tools to answer query")
    public ResponseEntity<String> query(String query) {
        logger.info("Processing query: {}", query);

        // Get available tools from MCP server
        ListToolsResult availableTools = stdioMcpServer.listTools();
        logger.info("Available tools: {}", availableTools.tools().size());

        ToolConfiguration toolConfiguration = convertToToolConfiguration(availableTools);
        logger.debug("Tool Configuration: {}", toolConfiguration);


        List<Message> bedrockMessages = new ArrayList<>();
        bedrockMessages.add(Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(query))
                .build());

        return query(bedrockMessages, toolConfiguration);
    }


    private ResponseEntity<String> query(List<Message> messages, ToolConfiguration toolConfiguration) {
        logger.info("Making initial bedrock request with messages '{}' and tool config '{}'", messages, toolConfiguration);
        ConverseResponse response = makeBedrockRequest(messages, toolConfiguration);
        logger.info("Initial bedrock response: {}", response);

        logger.info("Processing response with stop reason: {}", response.stopReason());
        ConverseResponse currentResponse = response;
        List<String> finalText = new ArrayList<>();
        int turnCount = 0;

        while (turnCount < MAX_TURNS) {
            switch (currentResponse.stopReason()) {
                case TOOL_USE -> {
                    finalText.add("received toolUse request");

                    for (ContentBlock content : currentResponse.output().message().content()) {
                        if (content.text() != null) {
                            logger.info("Received toolUse request: {}", content.text());
                            finalText.add("[Thinking: " + content.text() + "]");
                            messages.add(Message.builder().role(ConversationRole.ASSISTANT).content(content).build());
                        } else if (content.toolUse() != null) {
                            logger.info("Received toolUse response: {}", content.toolUse());
                            messages.add(Message.builder().role(ConversationRole.ASSISTANT)
                                    .content(ContentBlock.builder().toolUse(content.toolUse()).build()).build());
                            ToolResultBlock toolResultBlock = handleToolUseBlock(content.toolUse());
                            messages.add(Message.builder().role(ConversationRole.USER)
                                    .content(ContentBlock.builder().toolResult(toolResultBlock).build()).build());
                            finalText.add(toolResultBlock.toString());

                            logger.info("Sending toolUse response to bedrock: {}", messages);
                            currentResponse = makeBedrockRequest(messages, toolConfiguration);
                            logger.info("Received response after toolUse: {}", currentResponse);
                        }
                    }
                }
                case MAX_TOKENS -> {
                    logger.info("Max tokens reached, ending conversation.");
                    finalText.add("[Max tokens reached, ending conversation.]");
                    turnCount = MAX_TURNS;
                }
                case STOP_SEQUENCE -> {
                    logger.info("Stop sequence reached, ending conversation.");
                    finalText.add("[Stop sequence reached, ending conversation.]");
                    turnCount = MAX_TURNS;
                }
                case CONTENT_FILTERED -> {
                    logger.info("Content filtered, ending conversation.");
                    finalText.add("[Content filtered, ending conversation.]");
                    turnCount = MAX_TURNS;
                }
                case END_TURN -> {
                    logger.info("End turn reached, ending conversation.");
                    if (!currentResponse.output().message().content().isEmpty()) {
                        ContentBlock firstContent = currentResponse.output().message().content().get(0);
                        if (firstContent.text() != null) {
                            finalText.add(firstContent.text());
                        }
                    }
                    turnCount = MAX_TURNS;
                }
            }
            turnCount++;
        }

        // At the end, replace the simple ResponseEntity.ok with:
        String responseText = String.join("\n\n", finalText);
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=utf-8")
                .header("X-Content-Type-Options", "nosniff")
                .body(responseText);
    }

    private ToolResultBlock  handleToolUseBlock(ToolUseBlock toolUseRequest) {
        String toolUseId = toolUseRequest.toolUseId();
        String toolName = toolUseRequest.name();
        Map<String, Object> toolArgs = (Map<String, Object>)toolUseRequest.input().unwrap();

        logger.info("Calling tool {} with args {} with use ID {}", toolName, toolArgs, toolUseId);
        CallToolResult result = stdioMcpServer.callTool(new CallToolRequest(toolName, toolArgs));
        logger.info("Tool result: {}", result);

        return ToolResultBlock.builder()
                .toolUseId(toolUseId)
                .status(!result.isError() ? ToolResultStatus.SUCCESS : ToolResultStatus.ERROR)
                .content(result.content().stream().map(
                        content -> ToolResultContentBlock.builder().text(content.toString()).build()).toList())
                .build();
    }

    private ToolConfiguration convertToToolConfiguration(ListToolsResult listToolsResult) {
        return ToolConfiguration.builder().tools(listToolsResult.tools().stream()
                .map(tool -> {
                    try {
                        String inputSchemaJson = new ObjectMapper().writeValueAsString(tool.inputSchema());

                        // Parse to JsonNode first
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(inputSchemaJson);

                        // Convert JsonNode to Document using recursive conversion
                        Document inputSchemaDocument = DocumentUtils.ConvertJsonNodeToDocument(jsonNode);

                        return Tool.builder()
                                .toolSpec(ToolSpecification.builder()
                                        .name(tool.name())
                                        .description(tool.description())
                                        .inputSchema(ToolInputSchema.builder()
                                                .json(inputSchemaDocument)
                                                .build())
                                        .build())
                                .build();
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to convert input schema to JSON: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                }).toList()
        ).build();
    }

    private ConverseResponse makeBedrockRequest(List<Message> messages, ToolConfiguration toolConfiguration) {
        ConverseRequest.Builder requestBuilder = ConverseRequest.builder()
                .modelId(MODEL_ID)
                .messages(messages)
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(1000)
                        .temperature(0.8F)
                        .build())
                .toolConfig(toolConfiguration);

        return bedrockClient.converse(requestBuilder.build());
    }
}
