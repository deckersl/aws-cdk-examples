import json
import boto3
import os
client = boto3.client('sns')

def main(event, context):
    response = client.publish(TopicArn=os.environ.get('TOPIC_ARN'), Message="Test message")
    return {
        "statusCode": 200,
        "body": json.dumps({
            "message": "EventBridge triggered Lambda executed successfully",
            "sns_message_id": response.get("MessageId"),
            "function_name": context.function_name,
            "request_id": context.aws_request_id
        })
    }
