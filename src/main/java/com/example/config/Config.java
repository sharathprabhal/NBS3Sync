package com.example.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class Config {
    private static final Logger logger = Logger.getLogger(Config.class.getName());

    private final Map<String, Object> props = new HashMap<String, Object>();
    private Path propFilePath;

    public static String BUCKET_NAME = "bucket";
    public static String BASE_DIRECTORY = "baseDirectory";
    public static String QUEUE_NAME = "queueName";
    public static String AWS_ACCESS_KEY = "awsAccessKey";
    public static String AWS_SECRET_KEY = "awsSecretKey";

    public Config(Path path) {
        propFilePath = path;
        populateProperties();
    }

    private void populateProperties() {
        // Clear the props first
        props.clear();
        try {
            Scanner scanner = new Scanner(propFilePath);
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] nameValuePair = line.split("=");
                props.put(nameValuePair[0], nameValuePair[1]);
            }
        } catch (IOException e) {
            logger.severe("Exception while reading properties");
        }
    }

    public Object getProperty(String key) {
        Object value = null;
        if(props.containsKey(key)) {
            value = props.get(key);
        }
        return value;
    }

    public String getBucketName() {
        return getProperty(BUCKET_NAME).toString();
    }

    public Path getBaseDir() {
        return new File(getProperty(BASE_DIRECTORY).toString()).toPath();
    }

    public String getQueueName() {
        return props.get(QUEUE_NAME).toString();
    }

    public String getAccessKey() {
        return props.get(AWS_ACCESS_KEY).toString();
    }

    public String getSecretKey() {
        return props.get(AWS_SECRET_KEY).toString();
    }

    public AWSCredentials getAWSCredentials() {
        return new BasicAWSCredentials(getAccessKey(), getSecretKey());
    }
}
