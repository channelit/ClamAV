package clamav.lambda.handler;

import clamav.lambda.Scanner;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Stream implements RequestStreamHandler {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII)));
        try {
            HashMap event = gson.fromJson(reader, HashMap.class);
            logger.log("STREAM TYPE: " + inputStream.getClass().toString());
            logger.log("EVENT TYPE: " + event.getClass().toString());
            logger.log("EVENT: " + event);
            if (event.containsKey("s3Object")) {
                String srcBucket = event.get("srcBucket").toString();
                String srcKey = event.get("srcKey").toString();
                logger.log("srcBucket:" + srcBucket + " srcKey:" + srcKey);
                Scanner scanner = new Scanner(context);
                String response = scanner.scan(srcBucket, srcKey);
                logger.log("\rResponse: " + response);
                writer.write(response);
            } else {
                SQSEvent sqsEvent = gson.fromJson(gson.toJson(event), SQSEvent.class);
                sqsEvent.getRecords().stream().forEach(
                        sqsRecord -> {
                            S3EventNotification s3EventNotification = S3EventNotification.parseJson(sqsRecord.getBody());
                            logger.log("\rReceived S3EventRecords: " + s3EventNotification.getRecords().size());
                            s3EventNotification.getRecords().forEach(
                                    s3Record -> {
                                        logger.log("\rProcessing s3: " + s3Record.getS3());
                                        String srcBucket = s3Record.getS3().getBucket().getName();
                                        String srcKey = s3Record.getS3().getObject().getUrlDecodedKey();
                                        Scanner scanner = new Scanner(context);
                                        String response = scanner.scan(srcBucket, srcKey);
                                        logger.log("\rResponse: " + response);
                                        writer.write(response);
                                    }
                            );
                        }
                );
            }
            if (writer.checkError()) {
                logger.log("WARNING: Writer encountered an error.");
            }
        } catch (IllegalStateException | JsonSyntaxException exception) {
            logger.log(exception.toString());
        } finally {
            reader.close();
            writer.close();
        }
    }
}