package main

import (
	"context"
	"encoding/json"
	"os"
	"runtime"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

type response struct {
	Service   string `json:"service"`
	Runtime   string `json:"runtime"`
	GoVersion string `json:"go_version"`
	Region    string `json:"region"`
	Message   string `json:"message"`
}

func handleRequest(ctx context.Context, request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	resp := &response{
		Service:   "restapi-lambda",
		Runtime:   "provided.al2023",
		GoVersion: runtime.Version(),
		Region:    os.Getenv("AWS_REGION"),
		Message:   "hello world!",
	}
	body, err := json.Marshal(resp)
	if err != nil {
		return events.APIGatewayProxyResponse{Body: "Error parsing payload", StatusCode: 400}, err
	}
	return events.APIGatewayProxyResponse{Body: string(body), StatusCode: 200}, nil
}

func main() {
	lambda.Start(handleRequest)
}
