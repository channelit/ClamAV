package clamav.lambda.handler;

import clamav.lambda.Clamav;
import clamav.lambda.S3Operations;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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


            S3Operations srcS3 = new S3Operations();
            S3Operations dstS3 = new S3Operations(System.getenv("s3dstAccessKey"), System.getenv("s3dstSecretKey"));
            S3Operations storeS3 = new S3Operations(System.getenv("s3storeAccessKey"), System.getenv("s3storeSecretKey"));
            setTag(srcS3.getS3client(), srcBucket, srcKey, Map.of("scan", "Started"));
            setMeta(srcS3.getS3client(), srcBucket, srcKey, Map.of("scan", "Started"));

            if (hasTag(srcS3.getS3client(), srcBucket, srcKey, "scan")) {
                logger.info("Skipping file:" + srcKey);
                return "skipped";
            }
            String filePath = "/tmp/" + srcKey;
            srcS3.downloadObject(srcBucket, srcKey, filePath);
            storeS3.downloadFolder(storeBucket, "clamav_defs", "/tmp");

            String result = Clamav.scan(filePath);
            logger.info("Writing to: " + "/" + dstKey);
            JsonObject convertedObject = new Gson().fromJson(Clamav.resultToJson(result), JsonObject.class);
            if (convertedObject.get("Infected files").getAsString().equals("0")) {
                dstS3.uploadObject(dstBucket, dstKey, filePath);
                setTag(srcS3.getS3client(), srcBucket, srcKey, Map.of("scan", "completed", "result", "ok"));
                setMeta(srcS3.getS3client(), srcBucket, srcKey, Map.of("scan", "completed", "result", "ok"));
            } else {
                setTag(srcS3.getS3client(), srcBucket, srcKey, Map.of("scan", "completed", "result", "infected"));
                setMeta(srcS3.getS3client(), srcBucket, srcKey, Map.of("scan", "completed", "result", "infected"));
            }
            logger.info("Successfully scanned file " + srcBucket + "/"
                    + srcKey + " and uploaded to " + "/" + dstKey);
            Boolean clearFile = new File(filePath).delete();
            return "Ok";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasTag(AmazonS3 s3client, String srcBucket, String srcKey, String tagKey) {
        GetObjectTaggingRequest getTaggingRequest = new GetObjectTaggingRequest(srcBucket, srcKey);
        GetObjectTaggingResult getTagsResult = s3client.getObjectTagging(getTaggingRequest);
        List<Tag> objTags = getTagsResult.getTagSet();
        AtomicReference<Boolean> hasTag = new AtomicReference<>(false);
        objTags.forEach(t -> {
                    if (t.getKey().equals(tagKey)) {
                        hasTag.set(true);
                    }
                }
        );
        return hasTag.get();
    }

    private void setTag(AmazonS3 s3client, String srcBucket, String srcKey, Map<String, String> tags) {
        GetObjectTaggingRequest getTaggingRequest = new GetObjectTaggingRequest(srcBucket, srcKey);
        GetObjectTaggingResult getTagsResult = s3client.getObjectTagging(getTaggingRequest);
        List<Tag> objTags = getTagsResult.getTagSet();
        tags.forEach((k, v) -> {
            AtomicReference<Boolean> isDupe = new AtomicReference<>(false);
            objTags.forEach(t -> {
                        if (t.getKey().equals(k)) {
                            t.setValue(v);
                            isDupe.set(true);
                        }
                    }
            );
            if (!isDupe.get()) {
                objTags.add(new Tag(k, v));
            }
        });
        s3client.setObjectTagging(new SetObjectTaggingRequest(srcBucket, srcKey, new ObjectTagging(objTags)));
    }

    private void setMeta(AmazonS3 s3client, String srcBucket, String srcKey, Map<String, String> meta) {
        GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(srcBucket, srcKey);
        ObjectMetadata metadata = s3client.getObjectMetadata(getObjectMetadataRequest);
        Map<String, String> oldMeta = metadata.getUserMetadata();
        oldMeta.putAll(meta);
        metadata.setUserMetadata(oldMeta);
        CopyObjectRequest request = new CopyObjectRequest(srcBucket, srcKey, srcBucket, srcKey).withNewObjectMetadata(metadata);
        s3client.copyObject(request);
    }

}