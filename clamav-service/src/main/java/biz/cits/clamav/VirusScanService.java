package biz.cits.clamav;

import com.amazonaws.services.lambda.invoke.LambdaFunction;

public interface VirusScanService {


    @LambdaFunction(functionName="$lambda.functionName")
    VirusScanOutput scanFile(VirusScanInput input);
}
