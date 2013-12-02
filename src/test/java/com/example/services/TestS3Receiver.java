package com.example.services;

import com.example.config.Config;
import com.example.mocks.MockFileManager;
import com.example.services.impl.S3FileEvent;
import com.example.services.impl.S3Receiver;
import com.example.utils.TestingUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class TestS3Receiver {

    private MockFileManager fileManager = new MockFileManager();
    private Config config = TestingUtils.getMockConfig();

    S3Receiver receiver = new S3Receiver(config, fileManager);

    @Test
    public void testReceiveFileCreate() {
        Event event = new S3FileEvent(FileOperation.CREATE, new File("/foo/bar"), "me");
        receiver.receive(event);

        assertEquals("File is downloaded locally", 1, fileManager.downloaded.size());
    }

    @Test
    public void testReceiveFileDelete() {
        Event event = new S3FileEvent(FileOperation.DELETE, new File("/foo/bar"), "me");
        receiver.receive(event);

        assertEquals("File is downloaded locally", 1, fileManager.deletedLocalFiles.size());
    }

    @Test
    public void testReceiveDirectoryCreate() {
        Event event = new S3FileEvent(FileOperation.CREATE, new File("/foo/bar"), "me", true);
        receiver.receive(event);

        assertEquals("Folder is not created locally", 0, fileManager.downloaded.size());
    }

    @Test
    public void testReceiveToSelf() {
        Event event = new S3FileEvent(FileOperation.CREATE, new File("/foo/bar"), config.getQueueName(), true);
        receiver.receive(event);

        assertEquals("Directory should not be created", 0, fileManager.createDirectories.size());
    }



}
