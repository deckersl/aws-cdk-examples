import json
import sys
import platform
import os

def main(event, context):
    info = {
        "project": "lambda-cron",
        "description": "AWS CDK Example — Scheduled Lambda",
        "language": "Python",
        "runtime": f"Python {sys.version}",
        "platform": platform.platform(),
        "architecture": platform.machine(),
        "handler": context.function_name,
        "memory_mb": context.memory_limit_in_mb,
        "region": os.environ.get("AWS_REGION", "unknown"),
        "cdk_construct": "lambda.Function + events.Rule",
        "schedule": "cron(0 18 ? * MON-FRI *)",
        "event": event,
    }
    print(json.dumps(info, indent=2))
    return info
