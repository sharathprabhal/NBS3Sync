package com.example.services;

import com.example.config.Config;
import com.example.services.impl.S3FileEvent;
import com.example.utils.TestingUtils;
import org.junit.Test;

import java.io.File;
import static org.junit.Assert.*;

public class TestS3FileEvent {

    private Config config = TestingUtils.getMockConfig();

    @Test
    public void testConstructor() {
        File f = new File("/path/to/a/file");
        S3FileEvent fileEvent = new S3FileEvent(FileOperation.CREATE, f, "me");
        assertEquals("incorrect operation", FileOperation.CREATE, fileEvent.getOperation());
        assertEquals("incorrect file", "/path/to/a/file", fileEvent.getFile().getPath());
        assertEquals("incorrect originator", "me", fileEvent.getOriginator());
        assertEquals("incorrect isDir", false, fileEvent.isDirectory());

        fileEvent = new S3FileEvent(FileOperation.CREATE, f, "me", true);
        assertEquals("incorrect isDir", true, fileEvent.isDirectory());
    }

    @Test
    public void testToJson() {
        File f = new File("/foo/bar/to/a/file");
        S3FileEvent fileEvent = new S3FileEvent(FileOperation.CREATE, f, "me");
        assertEquals("toString does not match", "{\"operation\":\"CREATE\",\"file\":\"to/a/file\",\"originator\":\"me\"}", fileEvent.toJson(config.getBaseDir()));
    }

    @Test
    public void testEqualPositive() {
        File f = new File("/path/to/a/file");
        S3FileEvent e1 = new S3FileEvent(FileOperation.CREATE, f, "me");
        S3FileEvent e2 = new S3FileEvent(FileOperation.CREATE, f, "me");
        assertTrue("hascode should match", e1.hashCode() == e2.hashCode());
        assertTrue("should be equal", e1.equals(e2));
    }

    @Test
    public void testEqualNegative() {
        File f1 = new File("/path/to/a/file");
        File f2 = new File("/path/to/a/differentFile");
        S3FileEvent e1 = new S3FileEvent(FileOperation.CREATE, f1, "me");
        S3FileEvent e2 = new S3FileEvent(FileOperation.CREATE, f2, "me");
        assertFalse("hascode should not match", e1.hashCode() == e2.hashCode());
        assertFalse("should not be equal", e1.equals(e2));
    }
}
