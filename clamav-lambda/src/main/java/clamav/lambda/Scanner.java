package clamav.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Map;

public class Scanner {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    LambdaLogger logger;

    public Scanner(Context context){
        this.logger = context.getLogger();
    }

    public String scan(String srcBucket, String srcKey) {
        try {
            S3Operations s3Ops = new S3Operations();
            if (s3Ops.hasTag(srcBucket, srcKey, "scan")) {
                logger.log(gson.toJson("Skipping:" + srcKey));
                return "skipped:" + srcKey;
            }
            s3Ops.setTag(srcBucket, srcKey, Map.of("scan", "Started"));

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
                s3Ops.setTag(srcBucket, srcKey, Map.of("scan", "completed", "result", "ok"));
            } else {
                s3Ops.setTag(srcBucket, srcKey, Map.of("scan", "completed", "result", "infected"));
            }
            logger.log(gson.toJson("Scanned:" + srcKey));
            boolean clearFile = new File(filePath).delete();
            logger.log("Temp file deleted:" + clearFile);
            return "Ok";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
