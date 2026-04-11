def lambda_handler(event, context):
    status = "SUCCEEDED" if event.get("status") == "SUCCEEDED" else "FAILED"
    return {
        "status": status,
        "source": "status",
        "function_name": context.function_name,
        "request_id": context.aws_request_id,
        "event": event,
    }
