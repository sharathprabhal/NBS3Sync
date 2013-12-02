package com.example.services.impl;

import com.example.utils.SQSUtils;
import com.example.config.Config;
import com.example.services.Event;
import com.example.services.FileManager;
import com.example.services.FileOperation;
import com.example.services.Receiver;
import com.amazonaws.auth.policy.*;
import com.amazonaws.auth.policy.conditions.ArnCondition;
import com.amazonaws.auth.policy.conditions.ConditionFactory;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.*;

import java.util.*;
import java.util.logging.Logger;

public class S3Receiver implements Receiver {

    private final AmazonSQSAsyncClient client;
    private final FileManager fileManager;
    private final Config config;

    private String queueUrl;
    private final static Logger logger = Logger.getLogger(S3Receiver.class.getName());

    private final static int RECEIVE_PERIOD = 5000;

    private String queueArn;

    public S3Receiver(Config config, FileManager fileManager) {
        this.config = config;
        this.fileManager = fileManager;
        client = new AmazonSQSAsyncClient(config.getAWSCredentials());
    }

    public String createQueueForTopic(String topicArn) {
        CreateQueueRequest request = new CreateQueueRequest();
        request.setQueueName(config.getQueueName());

        CreateQueueResult result = client.createQueue(request);
        queueUrl = result.getQueueUrl();
        logger.finest("Created queue " + config.getQueueName());

        // Set message retention period
        SetQueueAttributesRequest setAttributesRequest = new SetQueueAttributesRequest();
        setAttributesRequest.setQueueUrl(queueUrl);
        setAttributesRequest.addAttributesEntry("MessageRetentionPeriod", "1209600"); // 14 days, max is better
        client.setQueueAttributes(setAttributesRequest);

        // Get queueArn
        List<String> attributeNames = new ArrayList<String>();
        attributeNames.add("QueueArn");
        GetQueueAttributesRequest attributesRequest = new GetQueueAttributesRequest();
        attributesRequest.setAttributeNames(attributeNames);
        attributesRequest.setQueueUrl(queueUrl);
        GetQueueAttributesResult attributesResult = client.getQueueAttributes(attributesRequest);
        queueArn = attributesResult.getAttributes().get("QueueArn");
        logger.finest("Created QueueArn " + queueArn);

        // Add permission
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(QueueAttributeName.Policy.toString(), makePolicy(topicArn).toJson());
        client.setQueueAttributes(new SetQueueAttributesRequest(queueUrl, attributes));
        logger.finest("Added permissions for queue");
        return queueArn;

    }

    public void startListening() {
        initializeTimer();
    }

    public void initializeTimer() {
        ReceiveMessage receiveMessage = new ReceiveMessage();
        Timer timer = new Timer();
        timer.schedule(receiveMessage, 0, RECEIVE_PERIOD);
    }
    /**
     * Generate a policy that will allow messages published to an SNS topic
     * to be sent to all queues subscribed to that topic
     * @param topicArn the topic to create policy for
     * @return The policy
     */
    private Policy makePolicy(String topicArn) {
        //SQSActions.SendMessage does not work!!
        Action sendMessageAction = new Action() {
            @Override
            public String getActionName() {
                return "SQS:SendMessage";
            }
        };
        return new Policy().withId("sns2sqs").withStatements(
                new Statement(Statement.Effect.Allow)
                        .withPrincipals(Principal.AllUsers)
                        .withActions(sendMessageAction)
                        .withResources(new Resource(queueArn))
                        .withConditions(new ArnCondition(ArnCondition.ArnComparisonType.ArnEquals, ConditionFactory.SOURCE_ARN_CONDITION_KEY, topicArn)));
    }



    @Override
    public void receive(Event event) {

        if (event.getOriginator().equals(config.getQueueName())) {
            logger.finest("Message from me, ignoring..");
            return;
        }

        logger.info("Received in " + config.getQueueName() + " - " + event.getOperation() + " " + event.getFile().getAbsoluteFile());

        if (event.isDirectory()) {
            fileManager.createLocalDirectory(event.getFile());
        } else {
            if (event.getOperation().equals(FileOperation.DELETE)) {
                fileManager.deleteLocalFile(event.getFile());
            } else {
                logger.finest("Downloading " + event.getFile().toString());
                fileManager.download(event.getFile());
            }
        }
    }

    public void deleteMessage(String handle) {
        DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest();
        deleteMessageRequest.setQueueUrl(queueUrl);
        deleteMessageRequest.setReceiptHandle(handle);
        client.deleteMessageAsync(deleteMessageRequest);
    }

    private class ReceiveMessage extends TimerTask {

        @Override
        public void run() {
            try {
                ReceiveMessageRequest request = new ReceiveMessageRequest();
                request.withAttributeNames("SenderId");
                request.setQueueUrl(queueUrl);
                client.receiveMessageAsync(request, new AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult>() {
                    @Override
                    public void onError(Exception e) {
                        logger.severe(e.getMessage());
                    }

                    @Override
                    public void onSuccess(ReceiveMessageRequest receiveMessageRequest, ReceiveMessageResult receiveMessageResult) {
                        if (receiveMessageResult.getMessages().size() > 0) {
                            for (Message message: receiveMessageResult.getMessages()) {
                                Event event = SQSUtils.convertMessageToEvent(config.getBaseDir(), message);
                                // Receive the message and delete it
                                receive(event);
                                deleteMessage(message.getReceiptHandle());
                            }
                        }
                    }
                });
            } catch (Exception e) {
                logger.severe("Lost connection while receiving message " + e.getMessage());
            }
        }
    }
}

