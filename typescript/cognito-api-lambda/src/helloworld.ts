export const handler = async (_event: any = {}): Promise<any> => {
  return {
    statusCode: 200,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      service: 'cognito-api-lambda',
      runtime: 'nodejs22.x',
      region: process.env.AWS_REGION,
      functionName: process.env.AWS_LAMBDA_FUNCTION_NAME,
      timestamp: new Date().toISOString(),
    }),
  };
};
