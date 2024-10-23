package clamav.lambda.handler;

import clamav.lambda.Clamav;
import clamav.lambda.S3Operations;
import clamav.lambda.Scanner;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Stream implements RequestStreamHandler {
    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII)));
        try {
            HashMap event = gson.fromJson(reader, HashMap.class);
            logger.log("\rSTREAM TYPE: " + inputStream.getClass().toString());
            logger.log("\rEVENT TYPE: " + event.getClass().toString());
            logger.log("\rEVENT: " + event);
            if (event.containsKey("s3Object")) {
                String srcBucket = event.get("srcBucket").toString();
                String srcKey = event.get("srcKey").toString();
                logger.log("srcBucket:" + srcBucket + " srcKey:" + srcKey);
                Scanner scanner = new Scanner(context);
                String response = scanner.scan(srcBucket, srcKey);
                logger.log("\rResponse: " + response);
                writer.write(response);
            } else if (event.containsKey("Records")) {
//                SQSEvent sqsEvent = new SQSEvent();
                JsonElement jsonTree = gson.toJsonTree(event);
                logger.log("\rEVENT JSON: " + jsonTree);
                JsonArray recordsArray = jsonTree.getAsJsonObject().get("Records").getAsJsonArray();
                logger.log("\rrecordsArray: " + recordsArray);
//                Type listType = new TypeToken<ArrayList<SQSEvent.SQSMessage>>() {}.getType();
//                List<SQSEvent.SQSMessage> records = gson.fromJson(recordsArray, listType);
//                sqsEvent.setRecords(records);
//                sqsEvent.getRecords().forEach(
                for (JsonElement record : recordsArray) {
                    logger.log("\rrecord: " + record);
                    if (record.getAsJsonObject().has("body")) {
                        String bodyAsString = record.getAsJsonObject().get("body").getAsString();
                        logger.log("\rbodyAsString: " + bodyAsString);
                        S3EventNotification s3EventNotification = S3EventNotification.parseJson(bodyAsString);
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
                }
            } else if (event.containsKey("task")) {
                S3Operations s3 = new S3Operations();
                String response = "200 OK";
                try {
                    String folder = "/tmp";
                    if (System.getenv().containsKey("folder")) {
                        folder = System.getenv("folder");
                    }
                    logger.log("Using folder:" + folder);
                    String defFolder = folder + "/clamav_defs";
                    switch (event.get("task").toString()) {
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
                            response = Clamav.scan(event.get("file").toString(), defFolder);
                            logger.log("Response: " + response);
                            break;
                        case "rescan":
                            if (System.getenv().containsKey("useS3") && System.getenv("useS3").equalsIgnoreCase("true")) {
                                logger.log("Using definitions from S3 : " + System.getenv("useS3"));
                                String storeBucket = System.getenv("storeBucket");
                                logger.log("Downloading definitions from S3 to folder:" + folder);
                                s3.downloadFolder(storeBucket, "clamav_defs", folder);
                                logger.log("Downloaded definitions from S3 to folder:" + folder);
                            }
                            S3Operations s3Ops = new S3Operations();
                            AmazonS3 client = s3Ops.getS3client();
                            Scanner scanner = new Scanner(context);
                            for (S3ObjectSummary summary : S3Objects.withPrefix(client, event.get("bucket").toString(), event.get("folder").toString())) {
                                System.out.printf("Scanning key '%s'\n", summary.getKey());
                                scanner.scan(event.get("bucket").toString(), summary.getKey());
                            }
                            response = Clamav.scan(event.get("file").toString(), defFolder);
                            logger.log("Response: " + response);
                            break;
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (writer.checkError()) {
                logger.log("\rWARNING: Writer encountered an error.");
            }
        } catch (IllegalStateException | JsonSyntaxException exception) {
            logger.log(exception.toString());
        } finally {
            reader.close();
            writer.close();
        }
    }
}