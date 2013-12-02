package com.example.services;

import com.example.config.Config;
import com.example.mocks.MockFileManager;
import com.example.services.impl.S3FileEvent;
import com.example.services.impl.S3Publisher;
import com.example.utils.TestingUtils;
import com.amazonaws.services.sns.model.PublishRequest;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import static org.junit.Assert.*;

public class TestS3Publisher {

    Stack<PublishRequest> publishRequests = new Stack<PublishRequest>();

    MockFileManager mockFileManager = new MockFileManager();
    private Config config = TestingUtils.getMockConfig();

    S3Publisher publisher = new S3Publisher(config, mockFileManager) {

        @Override
        public String createTopic() {
            return "";
        }

        protected void publishToSNS(PublishRequest publishRequest) {
            publishRequests.push(publishRequest);
        }
    };

    @Test
    public void testPublishFile() {
        File f = new File("/foo/bar");
        Event event = new S3FileEvent(FileOperation.CREATE, f, "me");

        publisher.publish(event);

        assertEquals("event is not published", 1, publishRequests.size());
        PublishRequest request = publishRequests.pop();
        File uploadedFile = mockFileManager.uploaded.pop();
        assertEquals("publish request ", request.getMessage(), ((S3FileEvent)event).toJson(config.getBaseDir()));
        assertEquals("correct file uploaded", "/foo/bar", uploadedFile.getPath());
    }

    @Test
    public void testPublishFileModify() {
        File f = new File("/foo/bar");
        Event event = new S3FileEvent(FileOperation.MODIFY, f, "me", true);

        publisher.publish(event);

        assertEquals("folder should not be published", 1, publishRequests.size());
        PublishRequest request = publishRequests.pop();
        File uploadedFile = mockFileManager.uploaded.pop();
        assertEquals("publish request ", request.getMessage(), ((S3FileEvent)event).toJson(config.getBaseDir()));
        assertEquals("correct file uploaded", "/foo/bar", uploadedFile.getPath());
    }

    @Test
    public void testPublishDelete() {
        File f = new File("/foo/bar");
        Event event = new S3FileEvent(FileOperation.DELETE, f, "me");

        publisher.publish(event);

        assertEquals("folder should not be published", 1, publishRequests.size());
        PublishRequest request = publishRequests.pop();
        File uploadedFile = mockFileManager.deleted.pop();
        assertEquals("publish request ", request.getMessage(), ((S3FileEvent)event).toJson(config.getBaseDir()));
        assertEquals("correct file uploaded", "/foo/bar", uploadedFile.getPath());
    }

    @Test
    public void testPublishDirectory() throws IOException {
        File f = TestingUtils.createTempDirectory("tmp").toFile();
        Event event = new S3FileEvent(FileOperation.MODIFY, f, "me", true);

        publisher.publish(event);

        assertEquals("modify dir should not be published", 0, publishRequests.size());
    }


}
