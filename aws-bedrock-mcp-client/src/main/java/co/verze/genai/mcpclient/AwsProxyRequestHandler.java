package co.verze.genai.mcpclient;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * AWS Lambda handler for the MCP client application.
 * This class initializes the Spring context and handles API Gateway requests.
 */
public class AwsProxyRequestHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private static final Logger logger = LoggerFactory.getLogger(AwsProxyRequestHandler.class);
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        logger.info("Initializing Stream Handler spring context");
        try {
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(McpClientApplication.class);
        } catch (ContainerInitializationException e) {
            logger.error("Could not initialize Spring Boot application", e);
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        logger.info("Handling request for path: {}", input.getPath());

        // Process the request through the Spring container
        AwsProxyResponse response = handler.proxy(input, context);

        logger.info("Request processed successfully with status code: {}", response.getStatusCode());

        // Ensure proper Content-Type headers are set
        if (response.getHeaders() == null) {
            response.setHeaders(new HashMap<>());
        }

        // Force text content types for your endpoints
        response.getHeaders().put("Content-Type", "application/json; charset=utf-8");
        response.setBase64Encoded(false);

        return response;
    }
}
