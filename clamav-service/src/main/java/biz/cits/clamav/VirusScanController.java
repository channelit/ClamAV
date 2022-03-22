package biz.cits.clamav;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@RestController
public class VirusScanController {

    @Value("${lambda.awsSecretAccessKey}")
    private static String awsAccessKeyId;

    @Value("${lambda.awsSecretAccessKey}")
    private static String awsSecretAccessKey;

    @Value("${lambda.functionName}")
    private static String functionName;

    @GetMapping("/")
    public String scanFile() {

        AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);

        InvokeRequest lambdaRequest = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload("inputJSON");
        lambdaRequest.setInvocationType(InvocationType.RequestResponse);

        AWSLambda lambda = AWSLambdaClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();

        InvokeResult lmbResult = lambda.invoke(lambdaRequest);

        String resultJSON = new String(lmbResult.getPayload().array(), StandardCharsets.UTF_8);

        System.out.println(resultJSON);

        return "Greetings from Spring Boot!";
    }

    private SQSEvent buildEvent() {
        S3EventNotification.S3EventNotificationRecord s3EventNotificationRecord = new S3EventNotification.S3EventNotificationRecord();
        S3EventNotification s3EventNotification = new S3EventNotification();
        s3EventNotification.getRecords().add()
        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody();
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords()
    }
}
