package clamav.lambda.handler;

import clamav.lambda.Clamav;
import clamav.lambda.S3Operations;
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
        S3Operations s3 = new S3Operations();
        try {
            String folder = "/tmp";
            if (System.getenv().containsKey("folder")) {
                folder = System.getenv("folder");
            }
            logger.log("Using folder:" + folder);
            String defFolder = folder + "/clamav_defs";
            switch (event.get("task")) {
                case "update":
                    logger.log("Updating virus definitions");
                    response = Clamav.update(defFolder);
                    logger.log("Virus definitions are updated");
                    if (System.getenv().containsKey("useS3") && System.getenv("useS3").equalsIgnoreCase("true")) {
                        logger.log("Using definitions from S3 : " + System.getenv("useS3"));
                        String storeBucket = System.getenv("storeBucket");
                        logger.log("Uploading Defs to S3 bucket:" + storeBucket);
                        s3.uploadFolder(storeBucket, "clamav_defs", defFolder);
                        logger.log("Defs uploaded to S3");
                    }
                    break;
                case "scan":
                    if (System.getenv().containsKey("useS3") && System.getenv("useS3").equalsIgnoreCase("true")) {
                        logger.log("Using definitions from S3 : " + System.getenv("useS3"));
                        String storeBucket = System.getenv("storeBucket");
                        logger.log("Downloading definitions from S3 to folder:" + folder);
                        s3.downloadFolder(storeBucket, "clamav_defs", folder);
                        logger.log("Downloaded definitions from S3 to folder:" + folder);
                    }
                    response = Clamav.scan(event.get("file"), defFolder);
                    logger.log("Response: " + response);
                    break;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }
}
