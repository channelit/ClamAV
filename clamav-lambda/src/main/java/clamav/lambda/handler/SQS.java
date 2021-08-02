package clamav.lambda.handler;

import clamav.lambda.Scanner;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;

public class SQS implements RequestHandler<SQSEvent, String> {
    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("\rProcessing Event: " + sqsEvent.toString());
        sqsEvent.getRecords().stream().forEach(
                sqsRecord -> {
                    S3EventNotification s3EventNotification = S3EventNotification.parseJson(sqsRecord.getBody());
                    logger.log("\rReceived S3EventRecords: " + s3EventNotification.getRecords().size());
                    s3EventNotification.getRecords().forEach(
                            s3Record -> {
                                logger.log("\rProcessing s3: " + s3Record.getS3());
                                String srcBucket = s3Record.getS3().getBucket().getName();
                                String dstBucket = System.getenv("dstBucket");
                                String srcKey = s3Record.getS3().getObject().getUrlDecodedKey();
                                String dstKey = "scanned-" + srcKey;
                                Scanner scanner = new Scanner(context);
                                String response = scanner.scan(srcBucket, srcKey);
                                logger.log("\rResponse: " + response);
                            }
                    );
                }
        );
        return "Completed";

    }
}
