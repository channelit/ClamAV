package clamav.lambda.handler;

import clamav.lambda.Scanner;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Locale;

public class SQS implements RequestHandler<SQSEvent, String> {
    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        logger.log("\rReceived Event:" + sqsEvent.toString());
        sqsEvent.getRecords().stream().forEach(
                sqsMessage -> {
                    switch (sqsMessage.getEventSource().toLowerCase(Locale.ROOT)) {
                        case "aws:s3":
                            logger.log("\rS3Event:" + sqsMessage.getBody());
                            S3Event s3Event = gson.fromJson(sqsMessage.getBody(), S3Event.class);
                            s3Event.getRecords().forEach(
                                    s3Record -> {
                                        logger.log("\rProcessing s3Record:" + s3Record);
                                        String srcBucket = s3Record.getS3().getBucket().getName();
                                        String dstBucket = System.getenv("dstBucket");
                                        String srcKey = s3Record.getS3().getObject().getUrlDecodedKey();
                                        String dstKey = "scanned-" + srcKey;
                                        Scanner scanner = new Scanner(context);
                                        String response = scanner.scan(srcBucket, srcKey);
                                        logger.log("\rResponse: " + response);
                                    }
                            );
                            break;
                        default:
                            logger.log("\rUnknown Source:" + sqsMessage.getEventSource().toLowerCase(Locale.ROOT));
                            break;
                    }
                }
        );
        return "Completed";

    }
}
