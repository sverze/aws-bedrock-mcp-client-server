package co.verze.genai.mcpserver;

import co.verze.genai.mcpserver.configuration.McpServerConfiguration;
import co.verze.genai.mcpserver.configuration.ToolsConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({McpServerConfiguration.class, ToolsConfiguration.class})
public class McpServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}
}
