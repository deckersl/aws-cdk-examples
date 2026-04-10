import platform, sys

def main(event, context):
    return {
        "id": event["id"],
        "status": "SUCCEEDED",
        "info": {
            "project": "stepfunctions-job-poller",
            "language": "Python",
            "runtime_version": sys.version,
            "platform": platform.platform(),
            "architecture": platform.machine(),
            "cdk_construct": "aws-cdk-lib/aws-lambda.Function (InlineCode)",
            "service": "AWS Step Functions Job Poller (submit)",
        },
    }
