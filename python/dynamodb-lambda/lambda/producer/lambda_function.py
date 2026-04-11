import json
import uuid
import os
import boto3

dynamodb = boto3.resource('dynamodb')
TABLE_NAME = os.environ['TABLE_NAME']


def lambda_handler(event, context):
    table = dynamodb.Table(TABLE_NAME)
    item_id = str(uuid.uuid4())
    table.put_item(Item={'id': item_id})

    return {
        'statusCode': 200,
        'body': json.dumps({
            'service': 'dynamodb-lambda',
            'function': 'producer',
            'action': 'put_item',
            'item_id': item_id,
            'table': TABLE_NAME,
        }),
    }
