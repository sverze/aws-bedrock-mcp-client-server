package co.verze.genai.mcpserver.tools;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebPageTool implements Function<Map<String, Object>, CallToolResult> {
    private static final Logger logger = LoggerFactory.getLogger(WebPageTool.class);
    private final Tool toolDefinition;

    public WebPageTool() {
        logger.info("Creating WebPageTool bean");

        this.toolDefinition = new Tool(
            "get_web_page",
            "Get the the content of a web page as Markdown",
            """
            {
                "type": "object",
                "properties": {
                    "url": {
                        "type": "string",
                        "description": "The web page URL"
                    }
                },
                "required": ["url"]
            }
            """
        );
    }

    public Tool getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public CallToolResult apply(Map<String, Object> arguments) {
        // Extract arguments
        String url = (String) arguments.get("url");
        if (url == null || url.trim().isEmpty()) {
            return new CallToolResult(
                List.of(new TextContent("Error: Web Page URL is required")),
                true
            );
        }

        logger.info("Visiting webpage: " + url);
        try {
            // Send a GET request to the URL with a 30 second timeout
            Document document = Jsoup.connect(url)
                    .timeout(15000)
                    .get();

            // Convert the HTML content to Markdown using Flexmark
            FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
            String markdownContent = converter.convert(document).trim();

            // Remove multiple line breaks
            Pattern pattern = Pattern.compile("\\n{3,}");
            Matcher matcher = pattern.matcher(markdownContent);
            markdownContent = matcher.replaceAll("\n\n");

            logger.info("Converted " + markdownContent.split("\\n").length + " lines of markdown");
            return new CallToolResult(
                    List.of(new TextContent(markdownContent)),
                    false
            );

        } catch (Exception e) {
            logger.error("Error fetching the webpage", e);
            return new CallToolResult(
                    List.of(new TextContent("Error fetching the webpage: " + e.getMessage())),
                    true
            );
        }
    }
}
