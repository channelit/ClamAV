package biz.cits.clamav;

public class VirusScanInput {

    private String scanFileBucketName;
    private String scanFileKey;

    public String getScanFileBucketName() {
        return scanFileBucketName;
    }

    public void setScanFileBucketName(String scanFileBucketName) {
        this.scanFileBucketName = scanFileBucketName;
    }

    public String getScanFileKey() {
        return scanFileKey;
    }

    public void setScanFileKey(String scanFileKey) {
        this.scanFileKey = scanFileKey;
    }
}
