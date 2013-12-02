package com.example.utils;

import com.example.services.Event;
import com.example.services.FileOperation;
import com.amazonaws.services.sqs.model.Message;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class TestSQSUtils {

    private Path baseDir = new File("/foo/bar").toPath();

    @Test
    public void testConvertMessageToEvent() {
        Message message = new Message();
        message.setBody("{\n" +
                "  \"Type\" : \"Notification\",\n" +
                "  \"MessageId\" : \"942c624a-5f4f-5fe4-a2d7-28e60ff48e15\",\n" +
                "  \"TopicArn\" : \"t\",\n" +
                "  \"Subject\" : \"Operation\",\n" +
                "  \"Message\" : \"{\\\"operation\\\":\\\"CREATE\\\",\\\"file\\\":\\\"f4\\\",\\\"originator\\\":\\\"me\\\"}\",\n" +
                "  \"Timestamp\" : \"2013-09-12T23:30:03.434Z\",\n" +
                "  \"SignatureVersion\" : \"1\",\n" +
                "  \"Signature\" : \"n4tdSNZBDlaOgCS2ILxbhCGq3Cl/5LlG2SAh1OKUbat3qWzgtlD5PvgjJUq/heCb5Eo0KnnDYAJbeuiw7X9HmotCtq50OiqCtz6uq6EuApy1LhiBzOhyC5S4yarmQqGV0PBZCGLrvPn/So1HiVsFoBujJZNQuw0ysQQ/ILi6TFA=\",\n" +
                "  \"SigningCertURL\" : \"cert\",\n" +
                "  \"UnsubscribeURL\" : \"foo\"\n" +
                "}");
        message.setReceiptHandle("receiptHandle");
        message.setMessageId("messageId");



        Event event = SQSUtils.convertMessageToEvent(baseDir, message);
        assertEquals("operation is incorrect", FileOperation.CREATE, event.getOperation());
        assertEquals("originator is correct", "me", event.getOriginator());
        assertEquals("file path is correct", "/foo/bar/f4", event.getFile().toPath().toString());
        assertEquals("isDirectory is correct", false, event.isDirectory());
    }

    @Test
    public void testConvertMessageToEventIsDirPositive() {
        Message message = new Message();
        message.setBody("{\n" +
                "  \"Type\" : \"Notification\",\n" +
                "  \"MessageId\" : \"942c624a-5f4f-5fe4-a2d7-28e60ff48e15\",\n" +
                "  \"TopicArn\" : \"t\",\n" +
                "  \"Subject\" : \"Operation\",\n" +
                "  \"Message\" : \"{\\\"operation\\\":\\\"CREATE\\\",\\\"file\\\":\\\"dir/\\\",\\\"originator\\\":\\\"me\\\"}\",\n" +
                "  \"Timestamp\" : \"2013-09-12T23:30:03.434Z\",\n" +
                "  \"SignatureVersion\" : \"1\",\n" +
                "  \"Signature\" : \"sig/sig=\",\n" +
                "  \"SigningCertURL\" : \"cert\",\n" +
                "  \"UnsubscribeURL\" : \"foo\"\n" +
                "}");
        message.setReceiptHandle("receiptHandle");
        message.setMessageId("messageId");

        Event event = SQSUtils.convertMessageToEvent(baseDir, message);
        assertEquals("operation is incorrect", FileOperation.CREATE, event.getOperation());
        assertEquals("originator is correct", "me", event.getOriginator());
        assertEquals("file path is correct", "/foo/bar/dir", event.getFile().toPath().toString());
        assertEquals("isDirectory is correct", true, event.isDirectory());
    }

    @Test
    public void testGetQueueName() {
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/1234/fooQ";
        assertEquals("queueName is incorrect", "fooQ", SQSUtils.getQueueNameFromQueueUrl(queueUrl));
    }
}
