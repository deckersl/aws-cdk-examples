package main

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

type DeveloperInfo struct {
	Status          string `json:"status"`
	MessagesHandled int    `json:"messages_handled"`
	Source          string `json:"source"`
}

func handler(ctx context.Context, event events.SQSEvent) (events.SQSEventResponse, error) {
	for _, msg := range event.Records {
		body := map[string]interface{}{}
		if err := json.Unmarshal([]byte(msg.Body), &body); err != nil {
			fmt.Printf("{\"error\": \"failed to parse message\", \"messageId\": %q}\n", msg.MessageId)
			continue
		}
		out, _ := json.Marshal(map[string]interface{}{"message": "Received sqs message", "sqsBody": body})
		fmt.Println(string(out))
	}

	info := DeveloperInfo{
		Status:          "ok",
		MessagesHandled: len(event.Records),
		Source:          "s3-event-processor",
	}
	infoJSON, _ := json.Marshal(info)
	fmt.Println(string(infoJSON))

	return events.SQSEventResponse{}, nil
}

func main() {
	lambda.Start(handler)
}
