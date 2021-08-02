package clamav.lambda;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class S3Operations {

    private final AmazonS3 s3client;
    private final S3Client s3ClientV2;
    private static final Logger logger = LoggerFactory.getLogger(S3Operations.class);

    public S3Operations(String accessKey, String secretKey, S3Client s3ClientV) {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        this.s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_EAST_1)
                .build();
        this.s3ClientV2 = S3Client.builder().region(Region.US_EAST_1).build();
    }

    public S3Operations() {
        this.s3client = AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        this.s3ClientV2 = S3Client.builder().region(Region.US_EAST_1).build();
    }

    public void downloadFolder(String bucketName, String prefix, String folderPath) throws InterruptedException {
        TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3client).build();
        System.out.println("Downloading folder:bucket=" + bucketName + " prefix=" + prefix + " folderPath=" + folderPath);
        MultipleFileDownload download = tm.downloadDirectory(bucketName, prefix, new File(folderPath), true);
        download.addProgressListener((ProgressListener) progressEvent -> logger.info("Download status update: {}", progressEvent));
        download.waitForCompletion();
        System.out.println("Done downloading folder:bucket=" + bucketName + " prefix=" + prefix + " folderPath=" + folderPath);
    }

    public void deleteFolder(String bucketName, String folderPath) throws InterruptedException {
        for (S3ObjectSummary file : s3client.listObjects(bucketName, folderPath).getObjectSummaries()) {
            s3client.deleteObject(bucketName, file.getKey());
        }
    }

    public void downloadObject(String bucketName, String srcFilePath, String dstFilePath) throws IOException {
        Path dst = new File(dstFilePath).toPath();
        Files.deleteIfExists(dst);
        ResponseBytes<GetObjectResponse> getObjectResponseResponseBytes = s3ClientV2.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(srcFilePath).build());
        byte[] data = getObjectResponseResponseBytes.asByteArray();
        Files.createDirectories(dst.getParent());
        File localFile = new File(dstFilePath);
        OutputStream os = new FileOutputStream(localFile);
        os.write(data);
        os.close();
    }

    public void uploadFolder(String bucketName, String prefix, String folderPath) throws InterruptedException {
        TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3client).build();
        System.out.println("Uploading folder:bucket=" + bucketName + " prefix=" + prefix + " folderPath=" + folderPath);
        MultipleFileUpload upload = tm.uploadDirectory(bucketName, prefix, new File(folderPath), true);
        upload.addProgressListener((ProgressListener) progressEvent -> logger.info("Upload status update: {}", progressEvent));
        upload.waitForCompletion();
        System.out.println("Done uploading folder:bucket=" + bucketName + " prefix=" + prefix + " folderPath=" + folderPath);
    }

    public void uploadObject(String bucketName, String key, String filePath) {
        if (s3client.doesBucketExistV2(bucketName)) {
            logger.info("Bucket name is not available. Creating bucket.");
        }
        s3client.createBucket(bucketName);
        s3client.putObject(bucketName, key, new File(filePath));
    }

    public void moveObject(String srcBucket, String dstBucket, String srcKey, String dstKey) {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(srcBucket)
                .sourceKey(srcKey)
                .destinationBucket(dstBucket)
                .destinationKey(dstKey)
                .build();
        s3ClientV2.copyObject(copyObjectRequest);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(srcBucket)
                .key(srcKey)
                .build();
        s3ClientV2.deleteObject(deleteObjectRequest);
    }

    public void setTag(String srcBucket, String srcKey, Map<String, String> tags) {
        GetObjectTaggingRequest getTaggingRequest = GetObjectTaggingRequest.builder().bucket(srcBucket).key(srcKey).build();
        GetObjectTaggingResponse getObjectTaggingResponse = s3ClientV2.getObjectTagging(getTaggingRequest);
        List<Tag> oldTags = getObjectTaggingResponse.tagSet();
        Collection<Tag> newTags = new ArrayList<>();
        tags.forEach((k, v) -> {
            newTags.add(Tag.builder().key(k).value(v).build());
        });
        oldTags.forEach(t -> {
                    if (!tags.containsKey(t.key())) {
                        newTags.add(Tag.builder().key(t.key()).value(t.value()).build());
                    }
                }
        );
        Tagging tagging = Tagging.builder().tagSet(newTags).build();
        PutObjectTaggingResponse putObjectTaggingResponse = s3ClientV2.putObjectTagging(PutObjectTaggingRequest.builder().bucket(srcBucket).key(srcKey).tagging(tagging).build());
        System.out.println("Tags added :" + putObjectTaggingResponse.toString());
    }

    public boolean hasTag(String srcBucket, String srcKey, String tagKey) {
        GetObjectTaggingRequest getObjectTaggingRequest = GetObjectTaggingRequest.builder().bucket(srcBucket).key(srcKey).build();
        GetObjectTaggingResponse getObjectTaggingResponse = s3ClientV2.getObjectTagging(getObjectTaggingRequest);
        return getObjectTaggingResponse.tagSet().stream().anyMatch(t->t.key().equalsIgnoreCase(tagKey));
    }

    public String getTag(String srcBucket, String srcKey, String tagKey) {
        GetObjectTaggingRequest getObjectTaggingRequest = GetObjectTaggingRequest.builder().bucket(srcBucket).key(srcKey).build();
        GetObjectTaggingResponse getObjectTaggingResponse = s3ClientV2.getObjectTagging(getObjectTaggingRequest);
        return getObjectTaggingResponse.tagSet().stream().filter(t->t.key().equalsIgnoreCase(tagKey)).map(t->t.value()).collect(Collectors.joining());
    }

    public AmazonS3 getS3client() {
        return this.s3client;
    }
}
