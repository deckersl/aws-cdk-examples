import json

def main(event, context):
    return {
        "statusCode": 200,
        "body": json.dumps({
            "message": "Lambda cron executed successfully",
            "function_name": context.function_name,
            "request_id": context.aws_request_id
        })
    }
