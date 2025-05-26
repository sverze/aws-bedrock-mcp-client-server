package co.verze.genai.mcpserver.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
public class WeatherToolTest {

    private WeatherTool weatherTool;

    @Value("${tools.weather-api-key}")
    private String apiKey;

    @BeforeEach
    void setUp() {
        // Skip tests if API key is not available
        assumeTrue(
            apiKey != null && !apiKey.isEmpty(),
            "Weather API key is required for this test"
        );

        weatherTool = new WeatherTool(apiKey);
    }

    @Test
    void testApplyHappyCase() {
        // Prepare test data
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("location", "London");
        arguments.put("format", "celsius");

        // Call the method
        CallToolResult result = weatherTool.apply(arguments);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.content().size());

        TextContent textContent = (TextContent) result.content().get(0);
        String weatherText = textContent.text();

        // Verify the response contains expected elements
        assertTrue(weatherText.contains("Current weather in London"));
        assertTrue(weatherText.contains("°C"));
        assertTrue(weatherText.contains("Precipitation:"));
    }

    @Test
    void testApplyWithFahrenheitFormat() {
        // Prepare test data
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("location", "New York");
        arguments.put("format", "fahrenheit");

        // Call the method
        CallToolResult result = weatherTool.apply(arguments);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.content().size());

        TextContent textContent = (TextContent) result.content().get(0);
        String weatherText = textContent.text();

        // Verify the response contains expected elements
        assertTrue(weatherText.contains("Current weather in New York"));
        assertTrue(weatherText.contains("°F"));
        assertTrue(weatherText.contains("Precipitation:"));
    }

    @Test
    void testApplyWithDefaultFormat() {
        // Prepare test data with no format specified (should default to celsius)
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("location", "Paris");
        // No format specified

        // Call the method
        CallToolResult result = weatherTool.apply(arguments);

        // Verify the result
        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.content().size());

        TextContent textContent = (TextContent) result.content().get(0);
        String weatherText = textContent.text();

        // Verify the response contains expected elements
        assertTrue(weatherText.contains("Current weather in Paris"));
        assertTrue(weatherText.contains("°C")); // Should default to Celsius
        assertTrue(weatherText.contains("Precipitation:"));
    }
}