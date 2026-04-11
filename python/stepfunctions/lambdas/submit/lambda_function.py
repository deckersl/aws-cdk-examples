def lambda_handler(event, context):
    return {
        "status": "SUCCEEDED",
        "source": "submit",
        "function_name": context.function_name,
        "request_id": context.aws_request_id,
        "event": event,
    }
