import json
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)


def lambda_handler(event, context):
    logger.info(event)

    return {
        "statusCode": 200,
        "body": json.dumps({
            "service": "api-eventbridge-lambda",
            "component": "event_consumer",
            "action": "process_event",
            "status": "success",
            "detail_type": event.get("detail-type", "unknown")
        }),
    }
