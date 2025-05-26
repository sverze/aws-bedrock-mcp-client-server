package co.verze.genai.mcpclient;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * AWS Lambda handler for the MCP client application.
 * This class initializes the Spring context and handles API Gateway requests.
 */
public class AwsLambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private static final Logger logger = LoggerFactory.getLogger(AwsLambdaHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
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

//    @Override
//    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
//        logger.info("Handling request. Input stream available bytes: " + inputStream.available());
//
//        try {
//            byte[] inputBytes = inputStream.readAllBytes();
//            String requestBody = new String(inputBytes, StandardCharsets.UTF_8);
//            logger.debug("Request body: {}", requestBody);
//
//            AwsProxyRequest awsProxyRequest = objectMapper.readValue(requestBody, AwsProxyRequest.class);
//
//            // Log the content type
//            logger.info("Content-Type: {}", awsProxyRequest.getHeaders().get("Content-Type"));
//
//            // For JSON requests, we don't need to do any special parsing
//            // Spring will handle the JSON body parsing
//            InputStream newInputStream = new ByteArrayInputStream(objectMapper.writeValueAsBytes(awsProxyRequest));
//            handler.proxyStream(newInputStream, outputStream, context);
//
//            logger.info("Request processed successfully.");
//        } catch (Exception e) {
//            logger.error("Error handling request", "handleRequest", e);
//            throw e;
//        }
//    }

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
