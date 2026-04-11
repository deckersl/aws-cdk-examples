package main

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

type response struct {
	Status       string `json:"status"`
	Source       string `json:"source"`
	RecordCount  int    `json:"record_count"`
	FirstMessage string `json:"first_message"`
}

func handleRequest(ctx context.Context, event events.SQSEvent) (string, error) {
	firstMsg := ""
	if len(event.Records) > 0 {
		firstMsg = event.Records[0].Body
	}
	resp := response{
		Status:       "processed",
		Source:       "sqs-lambda",
		RecordCount:  len(event.Records),
		FirstMessage: firstMsg,
	}
	out, err := json.Marshal(resp)
	if err != nil {
		return "", fmt.Errorf("marshal response: %w", err)
	}
	fmt.Println(string(out))
	return string(out), nil
}

func main() {
	lambda.Start(handleRequest)
}
