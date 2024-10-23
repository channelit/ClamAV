package clamav.lambda;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Clamav {
    private static final Logger logger = LoggerFactory.getLogger(Clamav.class);

    public static String scan(String file, String folder) throws IOException, InterruptedException {
        return runCommand("./clamscan -v -a --stdout -d " + folder + " " + file);
    }

    public static String update(String folder) throws IOException, InterruptedException {
        return runCommand("./freshclam --config-file=/var/task/freshclam.conf --datadir=" + folder);
    }

    private static String runCommand(String command) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(command);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String line;
        StringBuilder out = new StringBuilder();
        int exitValue = proc.waitFor();
        logger.info("Exit Value:" + exitValue);
        while ((line = inputReader.readLine()) != null) {
            out.append("\n").append(line);
        }
        if (exitValue != 0) {
            while ((line = errorReader.readLine()) != null) {
                out.append("\n").append(line);
                logger.error(line);
            }
        }
        return out.toString();
    }

    public static String resultToJson(String result) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        logger.info("Converting :{}", result);
        Set<String> keys = new HashSet<>();
        String convertedResuls = gson.toJson(Arrays.stream(
                        result.split("\n")).map(line -> line.split(":"))
                .filter(a -> !a[0].isEmpty() && keys.add(a[0]))
                .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1].trim() : "")));
        logger.info("Converted :{}", convertedResuls);
        return convertedResuls;
    }
}
