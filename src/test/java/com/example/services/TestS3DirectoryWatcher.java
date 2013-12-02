package com.example.services;

import com.example.config.Config;
import com.example.mocks.MockPublisher;
import com.example.services.impl.S3DirectoryWatcher;
import com.example.utils.TestingUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class TestS3DirectoryWatcher {

    private static final int PAUSE_TIME_IN_SECONDS = 10;
    private static final long MAX_WAIT = 10;
    private static final Logger logger = Logger.getLogger(TestS3DirectoryWatcher.class.getName());

    private MockPublisher publisher;
    private S3DirectoryWatcher watcher;
    private Path tempBaseDir;
    private Config config;

    @Before
    public void setup() throws IOException {
        publisher = new MockPublisher();

        watcher = new S3DirectoryWatcher(TestingUtils.getMockConfig(), publisher);
        tempBaseDir = Files.createTempDirectory("tempS3");
        tempBaseDir.toFile().deleteOnExit();

        watcher.registerDirectory(tempBaseDir);
    }

    @Test
    public void testFileCreation() throws IOException {
        Path tempFile = TestingUtils.createTempFile(tempBaseDir, "tmp", ".tmp");
        waitUntil(1);
        assertEquals("Create file event not received by publisher", 1, publisher.events.size());
        Event event = publisher.events.get(0);
        assertEquals("incorrect queue name", "testQueue", event.getOriginator());
        assertEquals("incorrect file path", tempFile, event.getFile().toPath());
        assertEquals("incorrect operation", FileOperation.CREATE, event.getOperation());
        assertEquals("incorrect isDirectory", false, event.isDirectory());
    }

    @Test
    public void testDirectoryCreation() throws IOException {
        Path tempDir = TestingUtils.createTempDirectory(tempBaseDir, "tmp");
        waitUntil(1);
        assertEquals("Create dir event not received by publisher", 1, publisher.events.size());
        Event event = publisher.events.get(0);
        assertEquals("incorrect queue name", "testQueue", event.getOriginator());
        assertEquals("incorrect file path", tempDir, event.getFile().toPath());
        assertEquals("incorrect operation", FileOperation.CREATE, event.getOperation());
        assertEquals("incorrect isDirectory", true, event.isDirectory());
    }

    @Test
    public void testFileModify() throws IOException {
        Path tempFile = TestingUtils.createTempFile(tempBaseDir, "tmp", ".tmp");
        waitUntil(1);
        modifyFile(tempFile);
        waitUntil(2);
        assertEquals("Create, modify file event not received by publisher", 2, publisher.events.size());
        Event event = publisher.events.get(1);
        assertEquals("incorrect queue name", "testQueue", event.getOriginator());
        assertEquals("incorrect file path", tempFile, event.getFile().toPath());
        assertEquals("incorrect operation", FileOperation.MODIFY, event.getOperation());
        assertEquals("incorrect isDirectory", false, event.isDirectory());
    }

    @Test
    public void testFilCreationHidden() throws IOException {
        TestingUtils.createTempFile(tempBaseDir, ".Hiddentmp", ".tmp");
        waitUntil(0);
        assertEquals("Hidden file create should not be published", 0, publisher.events.size());
    }

    @Test
    public void testFileInsideFolder() throws IOException {
        Path tempDir = TestingUtils.createTempDirectory(tempBaseDir, "tmp");
        waitUntil(1);
        Path tempFile = Files.createTempFile(tempDir, "tmp", ".tmp");
        waitUntil(2);
        assertEquals("Create, modify dir, create file event not received by publisher", 3, publisher.events.size());

        Event event = publisher.events.get(2);
        assertEquals("create event not received", FileOperation.CREATE, event.getOperation());
        assertEquals("isDirectory should be false", false, event.isDirectory());
        assertEquals("path is incorrect", tempFile.toString(), event.getFile().toPath().toString());
    }

    @Test
    public void testDeleteFile() throws IOException {
        Path tempFile = TestingUtils.createTempFile(tempBaseDir, "tmp", ".tmp");
        waitUntil(1);
        tempFile.toFile().delete();
        waitUntil(2);
        assertEquals("Create, delete file event not received by publisher", 2, publisher.events.size());
        Event deleteEvent = publisher.events.get(1);
        assertEquals("incorrect file path", tempFile, deleteEvent.getFile().toPath());
        assertEquals("incorrect operation", FileOperation.DELETE, deleteEvent.getOperation());
        assertEquals("incorrect isDirectory", false, deleteEvent.isDirectory());
    }

    @Test
    public void testDeleteDirectory() throws IOException {
        Path tempDir = TestingUtils.createTempDirectory(tempBaseDir, "tmp");
        waitUntil(1);
        tempDir.toFile().delete();
        waitUntil(2);
        assertEquals("Create, delete folder event not received by publisher", 2, publisher.events.size());
        Event deleteEvent = publisher.events.get(1);
        assertEquals("incorrect file path", tempDir, deleteEvent.getFile().toPath());
        assertEquals("incorrect operation", FileOperation.DELETE, deleteEvent.getOperation());
        assertEquals("incorrect isDirectory", false, deleteEvent.isDirectory());
    }

    private void modifyFile(Path path) {
        if (path != null) {
            try {
                PrintWriter pw = new PrintWriter(new FileWriter(path.toFile()));
                pw.println("some random string");
                pw.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    private void waitUntil(int requiredSize) {
        Long currentTime = System.currentTimeMillis();
        Long threshold = currentTime + (MAX_WAIT * 1000);

        while ((publisher.events.size() < requiredSize) || (threshold > System.currentTimeMillis())) {
            pause(1);
        }
    }

    private void pause(int timeInSeconds) {
        try {
            Thread.sleep(timeInSeconds * 1000);
        } catch (InterruptedException e) {
            // Do nothing
        }
    }
}
