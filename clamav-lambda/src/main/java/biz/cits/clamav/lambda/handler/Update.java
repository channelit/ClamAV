package biz.cits.clamav.lambda.handler;

import biz.cits.clamav.lambda.Clamav;
import biz.cits.clamav.lambda.S3Operations;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Map;

public class Update implements RequestHandler<Map<String, String>, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        LambdaLogger logger = context.getLogger();
        String response = "200 OK";
        // log execution details
        logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
        logger.log("CONTEXT: " + gson.toJson(context));
        // process event
        logger.log("EVENT: " + gson.toJson(event));
        logger.log("EVENT TYPE: " + event.getClass());
        S3Operations s3 = new S3Operations(System.getenv("s3storeAccessKey"), System.getenv("s3storeSecretKey"));
        try {
            switch (event.get("task")) {
                case "update":
                    response = Clamav.update();
                    logger.log("Uploading Defs to S3");
                    s3.uploadFolder(System.getenv("storeBucket"), "clamav_defs", "/tmp/clamav_defs");
                    logger.log("Defs uploaded to S3");
                    break;
                case "scan":
                    s3.downloadFolder(System.getenv("storeBucket"), "clamav_defs", "/tmp");
                    response = Clamav.scan(event.get("file"));
                    logger.log("Response: " + response);
                    break;
                case "upload":
                    System.out.println("Uploading Defs to S3");
                    s3.uploadFolder(System.getenv("storeBucket"), "virus-def", "/tmp");
                    logger.log("Defs uploaded to S3");
                    break;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }
}
