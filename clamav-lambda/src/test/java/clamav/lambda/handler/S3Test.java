package clamav.lambda.handler;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

public class S3Test {

    @Test
    public void testS3Event() throws IOException {

        S3Event s3Event = EventLoader.loadS3Event("events/s3events.json");
        S3 handler = new S3();
        Context ctx = getFakeContext();
        Object output = handler.handleRequest(s3Event, ctx);
        if (output != null) {
            System.out.println(output.toString());
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
