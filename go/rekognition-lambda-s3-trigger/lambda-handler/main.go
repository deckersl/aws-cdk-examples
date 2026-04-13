package main

import (
	"context"
	"fmt"
	"os"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb/types"
	"github.com/aws/aws-sdk-go-v2/service/rekognition"
	rekognitionTypes "github.com/aws/aws-sdk-go-v2/service/rekognition/types"
)

type Event struct {
	Records []struct {
		S3 struct {
			Object struct {
				Key string `json:"key"`
			} `json:"object"`
		} `json:"s3"`
	} `json:"Records"`
}

type Response struct {
	ImageName string   `json:"image_name"`
	Labels    []string `json:"labels"`
	Table     string   `json:"table"`
	Status    string   `json:"status"`
}

func handler(event Event) (*Response, error) {
	cfg, err := config.LoadDefaultConfig(context.TODO())
	if err != nil {
		return nil, fmt.Errorf("unable to load SDK config: %v", err)
	}

	dynamodbClient := dynamodb.NewFromConfig(cfg)
	rekognitionClient := rekognition.NewFromConfig(cfg)

	key := event.Records[0].S3.Object.Key

	detectLabelsOutput, err := rekognitionClient.DetectLabels(context.TODO(), &rekognition.DetectLabelsInput{
		Image: &rekognitionTypes.Image{
			S3Object: &rekognitionTypes.S3Object{
				Bucket: aws.String(os.Getenv("BUCKET_NAME")),
				Name:   aws.String(key),
			},
		},
		MaxLabels:     aws.Int32(10),
		MinConfidence: aws.Float32(70),
	})
	if err != nil {
		return nil, fmt.Errorf("unable to detect labels: %v", err)
	}

	labels := make([]string, 0, len(detectLabelsOutput.Labels))
	for _, label := range detectLabelsOutput.Labels {
		labels = append(labels, *label.Name)
	}
	fmt.Println(labels)

	tableName := os.Getenv("TABLE_NAME")
	_, err = dynamodbClient.PutItem(context.TODO(), &dynamodb.PutItemInput{
		TableName: aws.String(tableName),
		Item: map[string]types.AttributeValue{
			"image_name": &types.AttributeValueMemberS{Value: key},
			"labels":     &types.AttributeValueMemberS{Value: fmt.Sprintf("%v", labels)},
		},
		ConditionExpression: aws.String("attribute_not_exists(image_name)"),
	})
	if err != nil {
		return nil, fmt.Errorf("unable to put item: %v", err)
	}

	return &Response{
		ImageName: key,
		Labels:    labels,
		Table:     tableName,
		Status:    "ok",
	}, nil
}

func main() {
	lambda.Start(handler)
}
