import json
import os
import boto3

dynamodb = boto3.resource('dynamodb')
TABLE_NAME = os.environ['TABLE_NAME']


def lambda_handler(event, context):
    table = dynamodb.Table(TABLE_NAME)
    response = table.scan()
    items = response.get('Items', [])

    return {
        'statusCode': 200,
        'body': json.dumps({
            'service': 'dynamodb-lambda',
            'function': 'consumer',
            'action': 'scan',
            'item_count': len(items),
            'table': TABLE_NAME,
        }),
    }
