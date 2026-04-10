import * as cdk from 'aws-cdk-lib';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

export class WidgetService extends Construct {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    const bucket = new s3.Bucket(this, 'WidgetStore', {
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
    });

    const handler = new lambda.Function(this, 'WidgetHandler', {
      runtime: lambda.Runtime.NODEJS_22_X,
      code: lambda.AssetCode.fromAsset('resources'),
      handler: 'widgets.main',
      environment: {
        BUCKET: bucket.bucketName,
      },
    });

    bucket.grantReadWrite(handler);

    const api = new apigateway.RestApi(this, 'widgets-api', {
      restApiName: 'Widget Service',
      description: 'This service serves widgets.',
    });

    const getWidgetsIntegration = new apigateway.LambdaIntegration(handler, {
      requestTemplates: { 'application/json': '{ "statusCode": "200" }' },
    });

    api.root.addMethod('GET', getWidgetsIntegration);

    const widget = api.root.addResource('{id}');
    const postWidgetIntegration = new apigateway.LambdaIntegration(handler);
    const getWidgetIntegration = new apigateway.LambdaIntegration(handler);
    const deleteWidgetIntegration = new apigateway.LambdaIntegration(handler);

    widget.addMethod('POST', postWidgetIntegration);
    widget.addMethod('GET', getWidgetIntegration);
    widget.addMethod('DELETE', deleteWidgetIntegration);
  }
}
