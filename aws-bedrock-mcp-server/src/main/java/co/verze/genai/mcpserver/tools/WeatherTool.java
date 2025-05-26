package co.verze.genai.mcpserver.tools;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Collectors;

import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * WeatherTool provides weather information as a tool for the MCP server.
 */
@Component
public class WeatherTool implements Function<Map<String, Object>, CallToolResult> {
    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);
    private final Tool toolDefinition;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    
    public WeatherTool(String apiKey) {
        log.info("Creating WeatherTool bean with API key '{}'", apiKey);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.baseUrl = "https://api.weatherapi.com/v1";
        this.apiKey = apiKey;
        
        this.toolDefinition = new Tool(
            "get_current_weather",
            "Get the current weather in a given location",
            """
            {
                "type": "object",
                "properties": {
                    "location": {
                        "type": "string",
                        "description": "The name of the city e.g. San Francisco, CA"
                    },
                    "format": {
                        "type": "string",
                        "enum": ["celsius", "fahrenheit"],
                        "description": "The format to return the weather in"
                    }
                },
                "required": ["location"]
            }
            """
        );
    }
    
    /**
     * Get the tool definition for registration with the MCP server.
     * 
     * @return The Tool definition
     */
    public Tool getToolDefinition() {
        return toolDefinition;
    }

    // Helper method to build encoded URL
    private String buildUrl(String path, Map<String, String> queryParams) {
        String encodedParams = queryParams.entrySet().stream()
                .map(entry -> encodeParam(entry.getKey()) + "=" + encodeParam(entry.getValue()))
                .collect(Collectors.joining("&"));

        return baseUrl + path + "?" + encodedParams;
    }

    private String encodeParam(String param) {
        return URLEncoder.encode(param, StandardCharsets.UTF_8);
    }
    
    /**
     * Apply the weather tool on the given arguments.
     * 
     * @param arguments Map containing location and format
     * @return The weather information
     */
    @Override
    public CallToolResult apply(Map<String, Object> arguments) {
        try {
            // Extract arguments
            String location = (String) arguments.get("location");
            if (location == null || location.trim().isEmpty()) {
                return new CallToolResult(
                    List.of(new TextContent("Error: Location is required")),
                    true
                );
            }
            
            Format format = Format.CELSIUS;  // Default format
            String formatStr = (String) arguments.get("format");
            if (formatStr != null && !formatStr.trim().isEmpty() && 
                formatStr.toLowerCase().equals("fahrenheit")) {
                format = Format.FAHRENHEIT;
            }
            log.info("Getting weather for '{}'", location);

            // Create query parameters map
            Map<String, String> params = Map.of(
                "key", apiKey,
                "q", location,
                "aqi", "no");

            // Build properly encoded URL
            String url = buildUrl("/current.json", params);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if response is successful
            if (httpResponse.statusCode() != 200) {
                throw new IOException("Unexpected response code: " + httpResponse.statusCode());
            }

            // Configure ObjectMapper to ignore unknown properties
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Parse JSON response
            Response response = mapper.readValue(httpResponse.body(), Response.class);

            log.info("Weather for '{}': {}", location, response);

            // Format response
            String weatherText;
            if (format == Format.CELSIUS) {
                weatherText = String.format(
                    "Current weather in %s, %s: %s, %.1f°C, Precipitation: %.1f mm",
                    response.location().name(),
                    response.location().country(),
                    response.current().condition().text(),
                    response.current().temp_c(),
                    response.current().precipIn() * 25.4 // Convert inches to mm
                );
            } else {
                weatherText = String.format(
                    "Current weather in %s, %s: %s, %.1f°F, Precipitation: %.1f in",
                    response.location().name(),
                    response.location().country(),
                    response.current().condition().text(),
                    response.current().temp_c() * 9/5 + 32, // Convert C to F
                    response.current().precipIn()
                );
            }
            
            return new CallToolResult(
                List.of(new TextContent(weatherText)),
                false
            );
        } catch (Exception e) {
            log.error("Error getting weather", e);
            return new CallToolResult(
                List.of(new TextContent("Error getting weather: " + e.getMessage())),
                true
            );
        }
    }
    
    enum Format {
        CELSIUS("celsius"), 
        FAHRENHEIT("fahrenheit");

        public final String formatName;

        Format(String formatName) {
            this.formatName = formatName;
        }

        @Override
        public String toString() {
            return formatName;
        }
    }
    
    @JsonInclude(Include.NON_NULL)
    public record Request(
        @JsonProperty(required = true) 
        @JsonPropertyDescription("The name of the city e.g. San Francisco, CA") 
        String location,
        @JsonProperty(required = true) 
        @JsonPropertyDescription("The format to return the weather in, e.g. 'celsius' or 'fahrenheit'") 
        Format format) {
    }
    
    public record Response(
        Location location,
        Current current
    ) {
        public record Current(
            double temp_c,
            @JsonProperty("precip_in")
            double precipIn,
            Condition condition
        ) {}
    
        public record Condition(
            String text
        ) {}

        public record Location(
            String name,
            String country
        ) {}
    }
}
