package com.example.utils;

import com.example.services.Event;
import com.example.services.FileOperation;
import com.example.services.impl.S3FileEvent;
import com.amazonaws.services.sqs.model.Message;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.file.Path;

public class SQSUtils {

    private final static JsonParser parser = new JsonParser();

    public static Event convertMessageToEvent(Path baseDir, Message message) {
        JsonObject msgAsJson = parser.parse(parser.parse(message.getBody()).getAsJsonObject().get("Message").getAsString()).getAsJsonObject();

        String path = baseDir + File.separator + msgAsJson.get("file").getAsString();
        File file = new File(path);
        FileOperation operation = S3FileEvent.getFileOperation(msgAsJson.get("operation").getAsString());
        String originator = msgAsJson.get("originator").getAsString();
        Event event;
        if (path.endsWith("/")) {
            event = new S3FileEvent(operation, file, originator, true);
        } else {
            event = new S3FileEvent(operation, file, originator);
        }
        return event;
    }

    public static String getQueueNameFromQueueUrl(String queueUrl) {
        return queueUrl.substring(queueUrl.lastIndexOf("/") + 1);
    }
}

