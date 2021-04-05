package biz.cits.clamav.lambda.handler;

import biz.cits.clamav.lambda.Clamav;
import biz.cits.clamav.lambda.S3Operations;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class S3 implements RequestHandler<S3Event, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(S3.class);

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        try {
            logger.info("EVENT: " + gson.toJson(s3event));
            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);

            String srcBucket = record.getS3().getBucket().getName();
            String dstBucket = System.getenv("dstBucket");
            String storeBucket = System.getenv("storeBucket");
            String srcKey = record.getS3().getObject().getUrlDecodedKey();
            String dstKey = "scanned-" + srcKey;

            S3Operations srcS3 = new S3Operations(System.getenv("s3srcAccessKey"), System.getenv("s3srcSecretKey"));
            S3Operations dstS3 = new S3Operations(System.getenv("s3dstAccessKey"), System.getenv("s3dstSecretKey"));
            S3Operations storeS3 = new S3Operations(System.getenv("s3storeAccessKey"), System.getenv("s3storeSecretKey"));

            String filePath = "/tmp/" + srcKey;
            srcS3.downloadObject(srcBucket, srcKey, filePath);
            storeS3.downloadFolder(storeBucket, "clamav_defs", "/tmp");

            String result = Clamav.scan(filePath);
            logger.info("Writing to: " + "/" + dstKey);
            JsonObject convertedObject = new Gson().fromJson(Clamav.resultToJson(result), JsonObject.class);
            if (convertedObject.get("Infected files").getAsString().equals("0")) {
                ObjectMetadata meta = new ObjectMetadata();
                meta.addUserMetadata("infected", "no");
                dstS3.uploadObject(dstBucket, dstKey, filePath);
            }
            logger.info("Successfully scanned file " + srcBucket + "/"
                    + srcKey + " and uploaded to " + "/" + dstKey);
            Boolean clearFile = new File(filePath).delete();
            return "Ok";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}