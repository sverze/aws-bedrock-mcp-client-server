# AWS Bedrock MCP Client and Server

A Spring Boot application that serves as a client for the Model Context Protocol (MCP), enabling interaction with Amazon Bedrock AI models with tool-using capabilities.

## Application Overview

This application provides a REST API for:

- Communicating with Amazon Bedrock models
- Using MCP tools for calculations, weather information, and web page retrieval
- Handling complex AI queries that might require multiple tools
- The application runs as an AWS Lambda function behind API Gateway, making it serverless and scalable.

## Prerequisites

- Java 21 or later
- Maven 3.8+
- AWS CLI configured with appropriate credentials
- AWS CDK installed (npm install -g aws-cdk)
- Weather API key from https://www.weatherapi.com/my/

## Environment Setup 

### Clone the Repository

```commandline
git clone https://github.com/sverze/aws-bedrock-mcp-client-server
cd aws-bedrock-mcp-client-server
```

### Configure AWS

You will credentials that has priviledges to the following services
- Lambda
- API Gateway
- Bedrock
- Bedrock Model _Anthropic Sonnet 4.0_

To configure local credentials run the following command:

```commandline
aws configure
```

###Building the Application

The project follows a multi-module Maven structure. To build all modules run this from the parent directory:

```commandline
mvn clean package install 
```

This will build all modules in the correct order:

- aws-bedrock-mcp-server
- aws-bedrock-mcp-client
- aws-bedrock-mcp-client-server-cdk

### Deploying to AWS

The AWS deployment is managed using AWS CDK. To deploy the application, run the following commands:

```commandline
cd aws-bedrock-mcp-client-server-cdk
cdk bootstrap
cdk deploy --context WEATHER_API_KEY=<your_api_key_here>
```

After deployment completes, CDK will output the API Gateway URL in the terminal.
You will need this URL to interact with the API and perform the tests below.

## Testing the API
Once deployed, you can test the API endpoints using curl or a tool like Postman:

1. Test the Hello World endpoint
   ```commandline
   curl https://<your-api-id>.execute-api.<region>.amazonaws.com/prod/hello
   ```
   Expected response: _Hello World from MCP Client!_

2. List available tools
   ```commandline
   curl https://<your-api-id>.execute-api.<region>.amazonaws.com/prod/tools
   ```
3. Perform a calculation
   ```commandline
   curl -X POST "https://<your-api-id>.execute-api.<region>.amazonaws.com/prod/calculate?operation=add&a=5&b=3"
   ```
4. Get weather information
   ```commandline
   curl -X POST "https://<your-api-id>.execute-api.<region>.amazonaws.com/prod/weather?location=New%20York&format=celsius"
   ```
5. Convert a web page to markdown
   ```commandline
   curl -X POST "https://<your-api-id>.execute-api.<region>.amazonaws.com/prod/webpage?url=https://example.com"
   ```
6. Make a complex query - The Weather
   ```commandline
   curl -X POST "https://<your-api-id>.execute-api.<region>.amazonaws.com/prod/query?query=What%20is%20the%20weather%20in%20London%20and%20calculate%2045%20divided%20by%209"
   ```
7. Make a complex query - Summarise a Webpage
   ```commandline
   curl -X POST "https://<your-api-id>.execute-api.<region>.amazonaws.com/prod/query?query=Summarise%20the%20following%20webpage%20https://example.com"
   ```

## Monitoring and Logs
   
View application logs in CloudWatch Logs:

- Navigate to AWS CloudWatch console
- Check the /aws/lambda/McpClientServerFunction log group 

## Clean Up

To remove all deployed resources:

```commandline
cd aws-bedrock-mcp-client-server-cdk
cdk destroy
```
    
## Architecture

This application implements a serverless architecture:

- API Gateway serves as the HTTP endpoint
- AWS Lambda runs the Spring Boot application
- Amazon Bedrock provides the AI model capabilities
- MCP Server provides tool implementations
- The AWS CDK stack creates and configures all the necessary AWS resources, including IAM roles with appropriate permissions for accessing Amazon Bedrock.