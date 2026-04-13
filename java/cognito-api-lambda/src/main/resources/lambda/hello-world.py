import json

def handler(event, context):
    return {
        'statusCode': 200,
        'headers': {'Content-Type': 'application/json'},
        'body': json.dumps({
            'service': 'cognito-api-lambda',
            'runtime': 'python3.12',
            'framework': 'aws-cdk',
            'language': 'java'
        })
    }
