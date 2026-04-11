import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as cdk from 'aws-cdk-lib';
import * as cognito from 'aws-cdk-lib/aws-cognito';

export class CognitoProtectedApi extends cdk.Stack {
  constructor(app: cdk.App, id: string) {
    super(app, id);

    const helloWorldFunction = new lambda.Function(this, 'helloWorldFunction', {
      code: new lambda.AssetCode('src'),
      handler: 'helloworld.handler',
      runtime: lambda.Runtime.NODEJS_22_X,
    });

    const helloWorldLambdaRestApi = new apigateway.LambdaRestApi(this, 'helloWorldLambdaRestApi', {
      restApiName: 'Hello World API',
      handler: helloWorldFunction,
      proxy: false,
    });

    const userPool = new cognito.UserPool(this, 'userPool', {
      signInAliases: {
        email: true,
      },
    });

    const authorizer = new apigateway.CfnAuthorizer(this, 'cfnAuth', {
      restApiId: helloWorldLambdaRestApi.restApiId,
      name: 'HelloWorldAPIAuthorizer',
      type: 'COGNITO_USER_POOLS',
      identitySource: 'method.request.header.Authorization',
      providerArns: [userPool.userPoolArn],
    });

    const hello = helloWorldLambdaRestApi.root.addResource('HELLO');

    hello.addMethod('GET', new apigateway.LambdaIntegration(helloWorldFunction), {
      authorizationType: apigateway.AuthorizationType.COGNITO,
      authorizer: {
        authorizerId: authorizer.ref,
      },
    });
  }
}

const app = new cdk.App();
new CognitoProtectedApi(app, 'CognitoProtectedApi');
app.synth();
