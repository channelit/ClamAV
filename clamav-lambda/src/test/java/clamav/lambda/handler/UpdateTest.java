package clamav.lambda.handler;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UpdateTest {

    @Test
    public void testSimpleEvent() throws IOException {
        Update handler = new Update();
        Map event = new HashMap();
        event.put("task", "update");
        Context ctx = getFakeContext();
        Object output = handler.handleRequest(event, ctx);
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