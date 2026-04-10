import { SNSClient, PublishCommand } from "@aws-sdk/client-sns";

const snsClient = new SNSClient();

export const handler = async (event) => {
    const results = [];
    try {
        for (const record of event.Records) {
            if (record.eventName === "MODIFY") {
                const newImage = record.dynamodb.NewImage;
                const oldImage = record.dynamodb.OldImage;
                const newCount = newImage.count ? parseInt(newImage.count.N) : null;
                const oldCount = oldImage?.count ? parseInt(oldImage.count.N) : null;

                if (newCount === 0 && oldCount > 0) {
                    const itemName = newImage.itemName ? newImage.itemName.S : "Unknown item";
                    await snsClient.send(new PublishCommand({
                        Message: `Alert: ${itemName} has reached zero inventory! Previous count was ${oldCount}.`,
                        Subject: `Stock Alert - ${itemName} Out of Stock`,
                        TopicArn: process.env.SNS_TOPIC_ARN
                    }));
                    results.push({ itemName, oldCount, newCount, notified: true });
                }
            }
        }
        return { statusCode: 200, body: JSON.stringify({ message: "Processing complete", recordsProcessed: event.Records.length, notifications: results }) };
    } catch (error) {
        console.error("Error processing records:", error);
        return { statusCode: 500, body: JSON.stringify({ message: "Processing failed", error: error.message }) };
    }
};
