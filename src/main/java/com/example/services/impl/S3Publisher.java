package com.example.services.impl;

import com.example.config.Config;
import com.example.services.*;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.*;

import java.io.File;
import java.util.logging.Logger;

public class S3Publisher implements Publisher {

    AmazonSNSAsyncClient client;
    private final FileManager fileManager;
    private final String topic;
    private final Config config;
    private String topicArn;
    private final static Logger logger = Logger.getLogger(S3Publisher.class.getName());

    public S3Publisher(Config config,  FileManager fileManager) {
        this.config = config;
        this.fileManager = fileManager;
        client = new AmazonSNSAsyncClient(config.getAWSCredentials());
        topic = "S3Sync_" + config.getBucketName() + "_topic";
        createTopic();
    }

    public String createTopic() {
        CreateTopicRequest request = new CreateTopicRequest();
        request.setName(topic);
        CreateTopicResult result = client.createTopic(request);
        topicArn = result.getTopicArn();
        return topicArn;
    }

    public void subscribeQueue(String queueId) {
        SubscribeRequest subscribeRequest = new SubscribeRequest();
        subscribeRequest.setTopicArn(topicArn);
        subscribeRequest.setProtocol("sqs");
        subscribeRequest.setEndpoint(queueId);
        client.subscribe(subscribeRequest);
    }

    @Override
    public void publish(Event event) {

        if (!shouldPublish(event)) {
            return;
        }

        if (fileManager instanceof S3FileManager) {
            if (!((S3FileManager) fileManager).shouldDownload(event.getFile())) {
                return;
            }
        }
        if (event.getOperation() == FileOperation.DELETE) {
            fileManager.delete(event.getFile());
        } else if (event.getOperation() == FileOperation.CREATE || event.getOperation() == FileOperation.MODIFY) {
            fileManager.upload(event.getFile());
        }

        PublishRequest request = new PublishRequest();
        request.setTopicArn(topicArn);
        request.setSubject("Operation");
        request.setMessage(((S3FileEvent)event).toJson(config.getBaseDir()));


        publishToSNS(request);
    }

    protected void publishToSNS(PublishRequest publishRequest) {
        client.publishAsync(publishRequest, new AsyncHandler<PublishRequest, PublishResult>() {
            @Override
            public void onError(Exception e) {
                logger.severe(e.getMessage());
            }

            @Override
            public void onSuccess(PublishRequest publishRequest, PublishResult publishResult) {
                logger.info("Published " + publishRequest.getMessage());
            }
        });
    }

    private boolean shouldPublish(Event event) {
        File file = event.getFile();
        FileOperation operation = event.getOperation();
        boolean shouldPublish = true;

        // Directory modifies don't have to be sent. They are taken taken care of
        // individual file updates
        if (file.isDirectory() && operation.equals(FileOperation.MODIFY)) {
            shouldPublish = false;
        }
        return shouldPublish;
    }
}
