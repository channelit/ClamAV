package clamav.lambda;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Scanner {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();;

    public static String scan(Context context, String srcBucket, String srcKey) {
        try {
            LambdaLogger logger = context.getLogger();
            S3Operations s3Ops = new S3Operations();
            if (hasTag(s3Ops.getS3client(), srcBucket, srcKey, "scan")) {
                logger.log(gson.toJson("Skipping:" + srcKey));
                return "skipped:" + srcKey;
            }
            setTag(s3Ops.getS3client(), srcBucket, srcKey, Map.of("scan", "Started"));

            String folder = "/tmp";
            if (System.getenv().containsKey("folder")) {
                folder = System.getenv("folder");
            }
            logger.log("Using folder:" + folder);
            String defFolder = folder + "/clamav_defs";
            logger.log("Using definition folder:" + defFolder);
            String filePath = folder + "/" + srcKey.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            s3Ops.downloadObject(srcBucket, srcKey, filePath);
            logger.log("Downloaded file:" + filePath);

            if (System.getenv().containsKey("useS3") && System.getenv("useS3").equalsIgnoreCase("true")) {
                logger.log("Using definitions from S3 : " + System.getenv("useS3"));
                String storeBucket = System.getenv("storeBucket");
                logger.log("Downloading definitions from S3 to folder:" + folder);
                s3Ops.downloadFolder(storeBucket, "clamav_defs", folder);
                logger.log("Downloaded definitions from S3 to folder:" + folder);
            } else if (Files.notExists(Paths.get(defFolder))) {
                logger.log("Definitions not found. Downloading from mirror to folder:" + defFolder);
                Files.createDirectories(Path.of(defFolder));
                String response = Clamav.update(defFolder);
                logger.log(response);
            } else {
                logger.log("Using existing virus definitions in folder:" + defFolder);
            }

            String result = Clamav.scan(filePath, defFolder);
            logger.log("result:" + result);
            JsonObject convertedObject = new Gson().fromJson(Clamav.resultToJson(result), JsonObject.class);
            if (convertedObject.get("Infected files").getAsString().equals("0")) {
                setTag(s3Ops.getS3client(), srcBucket, srcKey, Map.of("scan", "completed", "result", "ok"));
            } else {
                setTag(s3Ops.getS3client(), srcBucket, srcKey, Map.of("scan", "completed", "result", "infected"));
            }
            logger.log(gson.toJson("Scanned:" + srcKey));
            boolean clearFile = new File(filePath).delete();
            logger.log("Temp file deleted:" + clearFile);
            return "Ok";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasTag(AmazonS3 s3client, String srcBucket, String srcKey, String tagKey) {
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

    private static void setTag(AmazonS3 s3client, String srcBucket, String srcKey, Map<String, String> tags) {
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
}
