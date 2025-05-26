package co.verze.genai.mcpserver.configuration;

import co.verze.genai.mcpserver.tools.WebPageTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.client.WebClient;

import co.verze.genai.mcpserver.tools.CalculatorTool;
import co.verze.genai.mcpserver.tools.WeatherTool;

/**
 * Configuration for MCP tools.
 */
@Configuration
public class ToolsConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ToolsConfiguration.class);
    
    @Bean
    public CalculatorTool calculatorTool() {
        log.info("Creating CalculatorTool bean");
        return new CalculatorTool();
    }
    
    @Bean
    public WeatherTool weatherTool(@Value("${tools.weather-api-key}") String apiKey) {
        log.info("Creating WeatherTool bean with API key '{}'", apiKey);
        return new WeatherTool(apiKey);
    }

    @Bean
    public WebPageTool webPageTool() {
        log.info("Creating WebPageTool bean");
        return new WebPageTool();
    }
}
