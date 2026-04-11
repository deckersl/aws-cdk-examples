import json

def handler(event, context):
    return {
        'statusCode': 200,
        'body': json.dumps({
            'service': 'api-cors-lambda',
            'framework': 'aws-cdk',
            'language': 'python',
            'message': 'Hello from API Gateway + CORS + Lambda'
        })
    }
