package clamav.lambda;

import org.junit.jupiter.api.Test;

public class ClamavTest {

    @Test
    public void testResultToJson(){
        String result = "Known viruses: 8508924\n" +
                "Engine version: 0.103.0\n" +
                "Scanned directories: 1\n" +
                "Scanned files: 4\n" +
                "Scanned files: 5\n" +
                "Infected files: 4\n" +
                "Data scanned: 0.00 MB\n" +
                "Data read: 0.00 MB (ratio 0.00:1)\n" +
                "Time: 56.389 sec (0 m 56 s)\n" +
                "Start Date: 2021:03:12 21:38:40\n" +
                "End Date:   2021:03:12 21:39:36\n" +
                "End Date:   2021:03:12 21:39:36\n" +
                "ClamAV update process started at Fri Mar 12 21:39:36 2021\n" +
                "daily.cvd database is up to date (version: 26106, sigs: 3959276, f-level: 63, builder: raynman)\n" +
                "main.cvd database is up to date (version: 59, sigs: 4564902, f-level: 60, builder: sigmgr)\n" +
                "bytecode.cvd database is up to date (version: 333, sigs: 92, f-level: 63, builder: awillia2)\n" +
                "END RequestId: 2feab1fd-48d1-4f9d-ad0a-4c18f17eba28\n" +
                "REPORT RequestId: 2feab1fd-48d1-4f9d-ad0a-4c18f17eba28\tDuration: 56520.11 ms\tBilled Duration: 56600 ms\tMemory Size: 3008 MB\tMax Memory Used: 3008 MB";
        System.out.println(Clamav.resultToJson(result));
    }
}
