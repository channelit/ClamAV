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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class S3Operations {

    private final AmazonS3 s3client;
    private static final Logger logger = LoggerFactory.getLogger(S3Operations.class);

    public S3Operations(String accessKey, String secretKey) {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    public S3Operations() {
        s3client = AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    public void downloadFolder(String bucketName, String prefix, String folderPath) throws InterruptedException {
        TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3client).build();
        System.out.println("Downloading folder.");
        MultipleFileDownload download = tm.downloadDirectory(bucketName, prefix, new File(folderPath), true);
        download.addProgressListener((ProgressListener) progressEvent -> logger.info("Download status update: {}", progressEvent));
        download.waitForCompletion();
        System.out.println("Done downloading folder");
    }

    public void deleteFolder(String bucketName, String folderPath) throws InterruptedException {
        for (S3ObjectSummary file : s3client.listObjects(bucketName, folderPath).getObjectSummaries()){
            s3client.deleteObject(bucketName, file.getKey());
        }
    }

    public void downloadObject(String bucketName, String srcFilePath, String dstFilePath) throws IOException {
        S3Object s3object = s3client.getObject(bucketName, srcFilePath);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        Path dst = new File(dstFilePath).toPath();
        Files.createDirectories(dst.getParent());
        Files.copy(inputStream, dst);
    }

    public void uploadFolder(String bucketName, String prefix, String folderPath) throws InterruptedException {
        TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3client).build();
        System.out.println("Uploading folder.");
        MultipleFileUpload upload = tm.uploadDirectory(bucketName, prefix, new File(folderPath), true);
        upload.addProgressListener((ProgressListener) progressEvent -> logger.info("Upload status update: {}", progressEvent));
        upload.waitForCompletion();
        System.out.println("Done uploading folder");
    }

    public void uploadObject(String bucketName, String key, String filePath) {
        if (s3client.doesBucketExistV2(bucketName)) {
            logger.info("Bucket name is not available. Creating bucket.");
        }
        s3client.createBucket(bucketName);
        s3client.putObject(bucketName, key, new File(filePath));
    }

    public AmazonS3 getS3client(){
        return this.s3client;
    }

}
