package biz.cits.clamav;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

@RestController
public class VirusScanController {

    @Value("${lambda.awsAccessKeyId}")
    private String awsAccessKeyId;

    @Value("${lambda.awsSecretAccessKey}")
    private String awsSecretAccessKey;

    @Value("${lambda.functionName}")
    private String functionName;

    @GetMapping("/scan")
    public String scanFile() {

        Region region = Region.US_EAST_1;
        LambdaClient awsLambda = LambdaClient.builder()
                .region(region)
                .build();
        InvokeResponse res = null;
        try {
            String json = "{\n" +
                    "   \"s3Object\":\"True\",\n" +
                    "   \"srcBucket\":\"arn:aws:s3:us-east-1:122936777114:accesspoint/clamav\",\n" +
                    "   \"srcKey\":\"JPG-01.jpg\"\n" +
                    "}";
            SdkBytes payload = SdkBytes.fromUtf8String(json);
            System.out.println(payload);
            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(payload)
                    .build();
            res = awsLambda.invoke(request);
            String value = res.payload().asUtf8String();
            System.out.println(value);
        } catch (LambdaException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "Greetings from Spring Boot!";
    }

}