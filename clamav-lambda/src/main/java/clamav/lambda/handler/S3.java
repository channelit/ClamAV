package clamav.lambda.handler;

import clamav.lambda.Scanner;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class S3 implements RequestHandler<S3Event, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT: " + gson.toJson(s3event));
        S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);
        String srcBucket = record.getS3().getBucket().getName();
        String dstBucket = System.getenv("dstBucket");
        String srcKey = record.getS3().getObject().getUrlDecodedKey();
        String dstKey = "scanned-" + srcKey;
        Scanner scanner = new Scanner(context);
        String response = "Scan Completed";
        response = scanner.scan(srcBucket, srcKey);
        logger.log("\rResponse: " + response);
        return response;
    }
}