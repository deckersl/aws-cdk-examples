package main

import (
	"context"
	"encoding/json"
	"os"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/dynamodb"
	"github.com/aws/aws-sdk-go/service/dynamodb/dynamodbattribute"
)

type MyEvent struct {
	ID      string `json:"ID"`
	Message string `json:"message"`
}

type Response struct {
	StatusCode int    `json:"statusCode"`
	Message    string `json:"message"`
}

func handleRequest(ctx context.Context, event MyEvent) (Response, error) {
	sess := session.Must(session.NewSessionWithOptions(session.Options{
		SharedConfigState: session.SharedConfigEnable,
	}))

	svc := dynamodb.New(sess)

	av, err := dynamodbattribute.MarshalMap(event)
	if err != nil {
		return Response{StatusCode: 500, Message: err.Error()}, err
	}

	tableName := os.Getenv("TABLE_NAME")

	_, err = svc.PutItem(&dynamodb.PutItemInput{
		Item:      av,
		TableName: aws.String(tableName),
	})
	if err != nil {
		return Response{StatusCode: 500, Message: err.Error()}, err
	}

	body, _ := json.Marshal(map[string]string{
		"project":   "go/lambda-dynamodb",
		"framework": "aws-cdk-go",
		"language":  "go",
		"status":    "item written to DynamoDB",
		"id":        event.ID,
		"message":   event.Message,
	})

	return Response{StatusCode: 200, Message: string(body)}, nil
}

func main() {
	lambda.Start(handleRequest)
}
