import platform, sys

def main(event, context):
    status = "SUCCEEDED" if event.get("status") == "SUCCEEDED" else "FAILED"
    return {
        "id": event["id"],
        "status": status,
        "info": {
            "project": "stepfunctions-job-poller",
            "language": "Python",
            "runtime_version": sys.version,
            "platform": platform.platform(),
            "architecture": platform.machine(),
            "cdk_construct": "aws-cdk-lib/aws-lambda.Function (InlineCode)",
            "service": "AWS Step Functions Job Poller (check_status)",
        },
    }
