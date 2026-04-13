import boto3
import os
import json

s3 = boto3.client('s3')
rekognition = boto3.client('rekognition')
dynamodb = boto3.client('dynamodb')

def handler(event, context):
    bucket_name = os.environ['BUCKET_NAME']
    key = event['Records'][0]['s3']['object']['key']
    image = {
        'S3Object': {
            'Bucket': bucket_name,
            'Name': key
        }
    }

    try:
        response = rekognition.detect_labels(Image=image, MaxLabels=10, MinConfidence=70)

        labels = response["Labels"]
        label_names = [label["Name"] for label in labels]

        # Write results JSON to S3
        json_labels = json.dumps(labels)
        filename_prefix = os.path.splitext(os.path.basename(key))[0]
        s3.put_object(Body=json_labels, Bucket=bucket_name, Key="results/" + filename_prefix + ".json")

        # Write results to DynamoDB
        dynamodb.put_item(
            TableName=os.environ['TABLE_NAME'],
            Item={
                'image_name': {'S': key},
                'labels': {'S': str(label_names)}
            }
        )

        return {
            "statusCode": 200,
            "body": json.dumps({
                "image": key,
                "bucket": bucket_name,
                "labels": label_names,
                "label_count": len(label_names)
            })
        }

    except Exception as e:
        print(f"Error processing object {key} from bucket {bucket_name}: {e}")
        raise e
