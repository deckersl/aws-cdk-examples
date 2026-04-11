package main

import (
	"testing"

	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/aws-cdk-go/awscdk/v2/assertions"
	"github.com/aws/jsii-runtime-go"
)

func TestLambdaDynamodbStack(t *testing.T) {
	app := awscdk.NewApp(nil)
	stack := NewLambdaDynamodbStack(app, "MyStack", nil)
	template := assertions.Template_FromStack(stack)

	template.HasResourceProperties(jsii.String("AWS::Lambda::Function"), map[string]interface{}{
		"Runtime": "provided.al2023",
	})

	template.HasResourceProperties(jsii.String("AWS::DynamoDB::Table"), map[string]interface{}{
		"BillingMode": "PAY_PER_REQUEST",
		"AttributeDefinitions": []map[string]interface{}{
			{
				"AttributeName": "ID",
				"AttributeType": "S",
			},
		},
	})
}
