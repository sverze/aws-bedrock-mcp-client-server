package co.verze.genai.mcpclient;

import co.verze.genai.mcpclient.configuration.McpClientConfiguration;
import co.verze.genai.mcpclient.configuration.OpenApiConfig;
import co.verze.genai.mcpclient.controller.McpClientController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@Import({McpClientConfiguration.class, OpenApiConfig.class, McpClientController.class})
public class McpClientApplication {
	private static final Logger logger = LoggerFactory.getLogger(McpClientApplication.class);

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOriginPatterns("http://localhost:8080", "https://*.amplifyapp.com")
						.allowedMethods("GET", "POST", "PUT", "DELETE")
						.allowedHeaders("*")
						.allowCredentials(true);
			}
		};
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception e) throws Exception {
		logger.error("Unhandled exception occurred", e);
		throw e;
	}

	public static void main(String[] args) {
		SpringApplication.run(McpClientApplication.class, args);
	}
}
