import json

def main(event, context):
    print(json.dumps(event))

    records = event.get("Records", [])
    bucket = records[0]["s3"]["bucket"]["name"] if records else None
    key = records[0]["s3"]["object"]["key"] if records else None

    return {
        "statusCode": 200,
        "body": json.dumps({
            "project": "lambda-s3-trigger",
            "trigger": "S3 OBJECT_CREATED",
            "bucket": bucket,
            "key": key,
            "recordCount": len(records)
        })
    }
