package main

import (
	"context"
	"encoding/json"
	"os"
	"runtime"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

func handleRequest(ctx context.Context, request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	resp := map[string]interface{}{
		"service":    "httpapi-lambda",
		"language":   "go",
		"goVersion":  runtime.Version(),
		"goArch":     runtime.GOARCH,
		"region":     os.Getenv("AWS_REGION"),
		"timestamp":  time.Now().UTC().Format(time.RFC3339),
	}
	body, err := json.Marshal(resp)
	if err != nil {
		return events.APIGatewayProxyResponse{StatusCode: 500}, err
	}
	return events.APIGatewayProxyResponse{Body: string(body), StatusCode: 200}, nil
}

func main() {
	lambda.Start(handleRequest)
}
