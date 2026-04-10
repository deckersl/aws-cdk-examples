import { DynamoDBDocument } from '@aws-sdk/lib-dynamodb';
import { DynamoDB } from '@aws-sdk/client-dynamodb';

const TABLE_NAME = process.env.TABLE_NAME || '';

const db = DynamoDBDocument.from(new DynamoDB());

export const handler = async (): Promise<any> => {
  try {
    const response = await db.scan({ TableName: TABLE_NAME });
    return {
      statusCode: 200,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        project: 'api-cors-lambda-crud-dynamodb',
        description: 'AWS CDK Example — CRUD API with DynamoDB',
        language: 'TypeScript',
        runtime: `Node.js ${process.version}`,
        architecture: process.arch,
        constructs: ['RestApi', 'NodejsFunction', 'Table'],
        table: TABLE_NAME,
        itemCount: response.Items?.length ?? 0,
        items: response.Items,
      }),
    };
  } catch (dbError) {
    return { statusCode: 500, body: JSON.stringify(dbError) };
  }
};
