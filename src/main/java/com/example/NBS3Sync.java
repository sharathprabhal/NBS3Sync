package com.example;

import com.example.config.Config;
import com.example.services.DirectoryWatcher;
import com.example.services.impl.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class NBS3Sync {

    Logger logger = Logger.getLogger(NBS3Sync.class.getName());

    public NBS3Sync() {}

    /**
     * Creates a <code>Config</code> from the path and bootstraps
     * some AWS stuff and starts watching the base directory
     * @param path The path to the config file
     */
    public NBS3Sync(Path path) {
        Config conf = new Config(path);
        new NBS3Sync().initialize(conf);
    }

    private void initialize(Config config) {
        logger.info("Initializing NBS3Sync...");

        S3FileManager fileManager = new S3FileManager(config);
        S3Receiver receiver = new S3Receiver(config, fileManager);
        S3Publisher publisher = new S3Publisher(config, fileManager);

        // Create topic
        String topicArn = publisher.createTopic();
        // Create a queue for that topic
        String queueArn = receiver.createQueueForTopic(topicArn);
        publisher.subscribeQueue(queueArn);
        receiver.startListening();

        Path baseDir = config.getBaseDir();

        if (Files.isDirectory(baseDir)) {
            DirectoryWatcher watcher = new S3DirectoryWatcher(config, publisher);
            watcher.registerDirectory(baseDir);
        } else {
            logger.severe("Please specify a directory");
        }
    }

    /**
     * The main entry point to the program. Pass path to config file as first argument
     * @param args command line args
     */
    public static void main(String[] args) {

        String configFile = args[0];
        Path path = new File(configFile).toPath();
        new NBS3Sync(path);
    }
}
