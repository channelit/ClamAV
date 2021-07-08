package clamav.lambda.handler;

import clamav.lambda.S3Operations;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.iterable.S3Versions;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import org.junit.Test;

import java.io.IOException;

public class S3Test {

    @Test
    public void testS3Event() {

        S3Event s3Event = EventLoader.loadS3Event("events/s3events.json");
        S3 handler = new S3();
        Context ctx = getFakeContext();
        Object output = handler.handleRequest(s3Event, ctx);
        if (output != null) {
            System.out.println(output.toString());
        }
    }

    @Test
    public void ReScanS3() {
        S3Operations s3Ops = new S3Operations(System.getenv("s3dstAccessKey"), System.getenv("s3dstSecretKey"));
        AmazonS3 client = s3Ops.getS3client();
        for ( S3ObjectSummary summary : S3Objects.withPrefix(client, "channel-test-s3", "Chief Information Officer - Solutions and Partners 4 (CIO-SP4)/") ) {
            System.out.printf("Object with key '%s' Has tag '%s'\n", summary.getKey(), summary.getETag());
        }

    }

    private Context getFakeContext() {
        Context ctx = new Context() {

            @Override
            public String getAwsRequestId() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getLogGroupName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getLogStreamName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getFunctionName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getFunctionVersion() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getInvokedFunctionArn() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public CognitoIdentity getIdentity() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int getMemoryLimitInMB() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public LambdaLogger getLogger() {
                return new LambdaLogger() {

                    @Override
                    public void log(String string) {
                        System.out.println(string);

                    }

                    @Override
                    public void log(byte[] bytes) {

                    }

                };
            }

        };
        return ctx;
    }
}
