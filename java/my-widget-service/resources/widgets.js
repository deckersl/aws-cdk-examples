const { S3 } = require("@aws-sdk/client-s3");
const s3 = new S3();

const bucketName = process.env.BUCKET;

exports.main = async function(event, context) {
  try {
    var method = event.httpMethod;
    var widgetName = event.path.startsWith('/') ? event.path.substring(1) : event.path;

    if (method === "GET") {
      if (event.path === "/") {
        const data = await s3.listObjectsV2({ Bucket: bucketName });
        var body = {
          project: "my-widget-service",
          language: "JavaScript",
          runtime: "nodejs22.x",
          cdkConstructs: ["S3 Bucket", "Lambda Function", "API Gateway RestApi", "IAM Role"],
          widgets: data.Contents ? data.Contents.map(function(e) { return e.Key }) : []
        };
        return {
          statusCode: 200,
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body)
        };
      }

      if (widgetName) {
        const data = await s3.getObject({ Bucket: bucketName, Key: widgetName });
        var body = await data.Body.transformToString();
        return {
          statusCode: 200,
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body)
        };
      }
    }

    if (method === "POST") {
      if (!widgetName) {
        return { statusCode: 400, headers: {}, body: "Widget name missing" };
      }
      const now = new Date();
      var data = widgetName + " created: " + now;
      await s3.putObject({
        Bucket: bucketName,
        Key: widgetName,
        Body: Buffer.from(data, "binary"),
        ContentType: "application/json"
      });
      return {
        statusCode: 200,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ created: widgetName })
      };
    }

    if (method === "DELETE") {
      if (!widgetName) {
        return { statusCode: 400, headers: {}, body: "Widget name missing" };
      }
      await s3.deleteObject({ Bucket: bucketName, Key: widgetName });
      return {
        statusCode: 200,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ deleted: widgetName })
      };
    }

    return { statusCode: 400, headers: {}, body: "We only accept GET, POST, and DELETE, not " + method };
  } catch(error) {
    return { statusCode: 400, headers: {}, body: error.stack || JSON.stringify(error, null, 2) };
  }
}
