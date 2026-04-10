const { S3Client, ListObjectsV2Command, GetObjectCommand, PutObjectCommand, DeleteObjectCommand } = require("@aws-sdk/client-s3");

const s3 = new S3Client();
const bucketName = process.env.BUCKET;

exports.main = async function(event, context) {
  try {
    const method = event.httpMethod;
    const widgetName = event.path.startsWith('/') ? event.path.substring(1) : event.path;

    if (method === "GET" && event.path === "/") {
      const data = await s3.send(new ListObjectsV2Command({ Bucket: bucketName }));
      const widgets = (data.Contents || []).map(e => e.Key);
      return {
        statusCode: 200,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          project: "my-widget-service",
          language: "javascript",
          runtime: "nodejs22.x",
          constructs: ["API Gateway", "Lambda", "S3"],
          request: { method, path: event.path, resource: event.resource },
          widgets
        })
      };
    }

    if (method === "GET" && widgetName) {
      const data = await s3.send(new GetObjectCommand({ Bucket: bucketName, Key: widgetName }));
      const body = await data.Body.transformToString();
      return { statusCode: 200, headers: {}, body: JSON.stringify(body) };
    }

    if (method === "POST") {
      if (!widgetName) return { statusCode: 400, headers: {}, body: "Widget name missing" };
      const now = new Date();
      const data = widgetName + " created: " + now;
      await s3.send(new PutObjectCommand({ Bucket: bucketName, Key: widgetName, Body: Buffer.from(data), ContentType: "application/json" }));
      return { statusCode: 200, headers: {}, body: JSON.stringify({ created: widgetName }) };
    }

    if (method === "DELETE") {
      if (!widgetName) return { statusCode: 400, headers: {}, body: "Widget name missing" };
      await s3.send(new DeleteObjectCommand({ Bucket: bucketName, Key: widgetName }));
      return { statusCode: 200, headers: {}, body: "Successfully deleted widget " + widgetName };
    }

    return { statusCode: 400, headers: {}, body: "We only accept GET, POST, and DELETE, not " + method };
  } catch(error) {
    return { statusCode: 400, headers: {}, body: error.stack || JSON.stringify(error, null, 2) };
  }
}
