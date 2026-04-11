package main

import (
	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsdynamodb"
	"github.com/aws/aws-cdk-go/awscdk/v2/awslambda"
	"github.com/aws/aws-cdk-go/awscdklambdagoalpha/v2"
	"github.com/aws/constructs-go/constructs/v10"
	"github.com/aws/jsii-runtime-go"
)

type LambdaDynamodbStackProps struct {
	awscdk.StackProps
}

func NewLambdaDynamodbStack(scope constructs.Construct, id string, props *LambdaDynamodbStackProps) awscdk.Stack {
	var sprops awscdk.StackProps
	if props != nil {
		sprops = props.StackProps
	}
	stack := awscdk.NewStack(scope, &id, &sprops)

	// create DynamoDB table (no hardcoded name — let CFN generate it)
	table := awsdynamodb.NewTable(stack, jsii.String("myDynamoDB"), &awsdynamodb.TableProps{
		BillingMode: awsdynamodb.BillingMode_PAY_PER_REQUEST,
		PartitionKey: &awsdynamodb.Attribute{
			Name: jsii.String("ID"),
			Type: awsdynamodb.AttributeType_STRING,
		},
		RemovalPolicy: awscdk.RemovalPolicy_DESTROY,
	})

	// create lambda function with provided.al2023 runtime
	lambdaFunction := awscdklambdagoalpha.NewGoFunction(stack, jsii.String("myGoHandler"), &awscdklambdagoalpha.GoFunctionProps{
		Runtime: awslambda.Runtime_PROVIDED_AL2023(),
		Entry:   jsii.String("./lambda-handler"),
		Bundling: &awscdklambdagoalpha.BundlingOptions{
			GoBuildFlags: jsii.Strings(`-ldflags "-s -w"`),
		},
		Environment: &map[string]*string{
			"TABLE_NAME": table.TableName(),
		},
	})

	// grant the lambda read/write access to the table
	table.GrantReadWriteData(lambdaFunction)

	// log lambda function ARN
	awscdk.NewCfnOutput(stack, jsii.String("lambdaFunctionArn"), &awscdk.CfnOutputProps{
		Value:       lambdaFunction.FunctionArn(),
		Description: jsii.String("Lambda function ARN"),
	})

	return stack
}

func main() {
	app := awscdk.NewApp(nil)

	NewLambdaDynamodbStack(app, "LambdaDynamodbStack", &LambdaDynamodbStackProps{
		awscdk.StackProps{
			Env: env(),
		},
	})

	app.Synth(nil)
}

func env() *awscdk.Environment {
	return nil
}
