package com.example.services.impl;

import com.example.utils.FileUtils;
import com.example.utils.NBS3SyncThreadFactory;
import com.example.config.Config;
import com.example.services.DirectoryWatcher;
import com.example.services.FileOperation;
import com.example.services.Publisher;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import static java.nio.file.LinkOption.*;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class S3DirectoryWatcher implements DirectoryWatcher {

    private WatchService watcher;
    private static final Logger logger = Logger.getLogger(S3DirectoryWatcher.class.getName());
    private Publisher publisher;
    private Map<WatchKey,Path> keys = new HashMap<WatchKey, Path>();

    private final Config config;

    public S3DirectoryWatcher(Config config, Publisher publisher) {
        this.publisher = publisher;
        this.config = config;
    }

    private WatchService getWatcher() throws IOException {
        if (watcher == null) {
            watcher = FileSystems.getDefault().newWatchService();
        }
        return watcher;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(getWatcher(), ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        Path prev = keys.get(key);
        if (prev == null) {
            logger.info("Watching " + dir);
        } else {
            if (!dir.equals(prev)) {
                logger.info("Update " + prev + " -> " + dir);
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void registerDirectory(final Path p) {
        try {
            registerAll(p);
        } catch (IOException e) {
            logger.severe("Error when registering directory: " + e.getMessage());
        }
        startWatching();
    }


    private void startWatching() {
        try {
            NBS3SyncThreadFactory.newThread("Directory watcher", new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        WatchKey key;
                        try {
                            key = watcher.take();
                        } catch (InterruptedException x) {
                            return;
                        }

                        Path dir = keys.get(key);
                        if (dir == null) {
                            logger.warning("Watchkey not recognized");
                            continue;
                        }

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind kind = event.kind();
                            if (kind == OVERFLOW) {
                                continue;
                            }

                            WatchEvent<Path> ev = (WatchEvent<Path>) event; // TODO: fix unchecked cast
                            Path path = dir.resolve(ev.context());
                            FileOperation op = S3FileEvent.getFileOperation(kind);

                            logger.finest(kind.name() + ": " + path.toString());

                            if (kind == ENTRY_CREATE) {
                                try {
                                    if (Files.isDirectory(path, NOFOLLOW_LINKS)) {
                                        registerAll(path);
                                    }
                                } catch (IOException e) {
                                    logger.warning("Could not read directory " + path.toString());
                                }
                            }

                            File f = dir.resolve(path).toFile();
                            if (!FileUtils.shouldIgnore(f)) {
                                publisher.publish(new S3FileEvent(op, f, config.getQueueName()));
                            }

                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            keys.remove(key);
                            if (keys.isEmpty()) {
                                //TODO: Do something useful here
                            }

                        }
                    }
                }
            }).start();
        } catch (Exception e) {
            logger.severe("Something bad happened " + e.getMessage());
        }
    }
}
