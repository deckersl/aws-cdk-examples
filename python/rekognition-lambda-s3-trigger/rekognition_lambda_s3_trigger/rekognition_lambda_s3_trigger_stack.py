# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

from aws_cdk import (
    aws_iam as iam,
    aws_lambda as _lambda,
    aws_s3 as s3,
    aws_dynamodb as ddb,
    aws_s3_notifications as s3_notifications,
    RemovalPolicy,
    Stack
)
from constructs import Construct

class RekognitionLambdaS3TriggerStack(Stack):
    def __init__(self, scope: Construct, id: str, **kwargs) -> None:
        super().__init__(scope, id, **kwargs)

        # create new IAM group and user
        group = iam.Group(self, "RekGroup")
        user = iam.User(self, "RekUser")
        user.add_to_group(group)

        # create S3 bucket to hold images
        bucket = s3.Bucket(self, 'Bucket',
            removal_policy=RemovalPolicy.DESTROY,
            auto_delete_objects=True
        )
        bucket.grant_read_write(user)

        # create DynamoDB table to hold Rekognition results
        table = ddb.Table(
            self, 'Classifications',
            partition_key=ddb.Attribute(name='image_name', type=ddb.AttributeType.STRING),
            removal_policy=RemovalPolicy.DESTROY
        )

        # create Lambda function
        lambda_function = _lambda.Function(
            self, 'RekFunction',
            runtime=_lambda.Runtime.PYTHON_3_13,
            handler='rekfunction.handler',
            code=_lambda.Code.from_asset('rekognition_lambda_s3_trigger/lambda'),
            environment={
                'BUCKET_NAME': bucket.bucket_name,
                'TABLE_NAME': table.table_name
            }
        )

        # add Rekognition permissions for Lambda function
        statement = iam.PolicyStatement()
        statement.add_actions("rekognition:DetectLabels")
        statement.add_resources("*")
        lambda_function.add_to_role_policy(statement)

        # create trigger for Lambda function with image type suffixes
        notification = s3_notifications.LambdaDestination(lambda_function)
        notification.bind(self, bucket)
        bucket.add_object_created_notification(notification, s3.NotificationKeyFilter(suffix='.jpg'))
        bucket.add_object_created_notification(notification, s3.NotificationKeyFilter(suffix='.jpeg'))
        bucket.add_object_created_notification(notification, s3.NotificationKeyFilter(suffix='.png'))

        # grant permissions for lambda to read/write to DynamoDB table and bucket
        table.grant_read_write_data(lambda_function)
        bucket.grant_read_write(lambda_function)
