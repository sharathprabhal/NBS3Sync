package com.example.utils;

import com.example.config.Config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestingUtils {

    public static Config getMockConfig() {
        Path path = getConfigFile("key", "secret", "/foo/bar", "foo", "testQueue");
        return new Config(path);
    }

    public static Path getConfigFile(String awsKey, String secretKey, String baseDir, String bucketName, String queueName) {
        Path path = null;
        try {
            String prefix = "temp";
            String suffix = ".properties";
            File tempFile = File.createTempFile(prefix, suffix);
            tempFile.deleteOnExit();

            PrintWriter printWriter = new PrintWriter(tempFile);
            String sampleProperties = "awsAccessKey=" + awsKey +
                    "\nawsSecretKey=" + secretKey +
                    "\nbaseDirectory=" + baseDir +
                    "\nbucket=" + bucketName +
                    "\nqueueName=" + queueName;
            printWriter.write(sampleProperties);
            printWriter.close();
            path = tempFile.toPath();
        } catch (IOException e) {
            //TODO: Log it..
        }
        return path;

    }

    public static Path createTempFile(Path baseDir, String prefix, String suffix) throws IOException {
        Path tmp = Files.createTempFile(baseDir, prefix, suffix);
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    public static Path createTempFile(String prefix, String suffix) throws IOException {
        Path tmp = Files.createTempFile(prefix, suffix);
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    public static Path createTempDirectory(String prefix) throws IOException {
        Path dir = Files.createTempDirectory(prefix);
        dir.toFile().deleteOnExit();
        return dir;
    }

    public static Path createTempDirectory(Path baseDir, String prefix) throws IOException {

        Path dir = Files.createTempDirectory(baseDir, prefix);
        dir.toFile().deleteOnExit();
        return dir;
    }


}
