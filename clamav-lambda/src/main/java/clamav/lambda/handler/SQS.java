package clamav.lambda.handler;

import clamav.lambda.Scanner;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Locale;

public class SQS implements RequestHandler<SQSEvent, String> {
    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        sqsEvent.getRecords().stream().forEach(
                sqsMessage -> {
                    switch (sqsMessage.getEventSource().toLowerCase(Locale.ROOT)) {
                        case "aws:s3":
                            S3Event s3Event = gson.fromJson(sqsMessage.getBody(), S3Event.class);
                            S3EventNotification.S3EventNotificationRecord record = s3Event.getRecords().get(0);
                            String srcBucket = record.getS3().getBucket().getName();
                            String dstBucket = System.getenv("dstBucket");
                            String srcKey = record.getS3().getObject().getUrlDecodedKey();
                            String dstKey = "scanned-" + srcKey;
                            Scanner scanner = new Scanner(context);
                            String response = scanner.scan(srcBucket, srcKey);
                            logger.log("\rResponse: " + response);
                            break;
                        default:
                            break;
                    }
                }
        );
        return "Completed";

    }
}
