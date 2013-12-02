package com.example.services.impl;

import com.example.utils.SQSUtils;
import com.example.config.Config;
import com.example.services.FileManager;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.sqs.AmazonSQSClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class S3FileManager implements FileManager {

    private final AmazonS3Client client;
    private final Config config;
    private final String bucket;
    private final static Logger logger = Logger.getLogger(S3FileManager.class.getName());
    // Keeps track of files are that just downloaded, so we don't enter a feedback loop
    private volatile List<File>  justDownloaded = new ArrayList<File>();

    public S3FileManager(Config config) {
        this.config = config;
        client = new AmazonS3Client(config.getAWSCredentials());
        bucket = config.getBucketName();
        populateLocalRepoIfFirstTime();
    }

    protected void populateLocalRepoIfFirstTime() {
        if (isFirstTime()) {
            syncEntireBucket();
        } else {
            logger.info("Queue already present");
        }
    }

    protected void syncEntireBucket() {
        logger.info("Syncing entire bucket...");
        TransferManager manager = new TransferManager(config.getAWSCredentials());
        manager.downloadDirectory(config.getBucketName(), null, config.getBaseDir().toFile());
    }

    /**
     * Loops through every queue and checks if queue is already present.
     * If present, it means this is not the first time.
     * @return is first time
     */
    protected boolean isFirstTime() {
        boolean firstTime = true;
        AmazonSQSClient sqsClient = new AmazonSQSClient(config.getAWSCredentials());
        for(String queueUrl: sqsClient.listQueues().getQueueUrls()) {
            queueUrl = SQSUtils.getQueueNameFromQueueUrl(queueUrl);
            if (queueUrl.equals(config.getQueueName())) {
                firstTime = false;
                break;
            }
        }
        return firstTime;
    }

    /**
     * Checks if a file should be downloaded. If a file has just been downloaded, the DirectoryWatcher
     * will still generate an event when the file get created/modified. In that case, we should not send an
     * update, else it should be downloaded
     * @param file the file to check
     * @return whether to download the file
     */
    public boolean shouldDownload(File file) {
        if (justDownloaded.contains(file)) {
            logger.finest("Already downloaded - " + config.getQueueName() + " for " + file.getAbsolutePath());
            justDownloaded.remove(file);
            return false;
        }
        return true;
    }

    @Override
    public boolean upload(File file) {
        PutObjectRequest request;

        if (file.isDirectory()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(0);
            InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
            request = new PutObjectRequest(bucket, getFilePath(file), emptyContent, metadata);

        } else {
            request = new PutObjectRequest(bucket, getFilePath(file), file);
        }
        return uploadToS3(request);
    }

    protected boolean uploadToS3(PutObjectRequest putObjectRequest) {
        boolean uploadSuccessful = false;
        try {
            client.putObject(putObjectRequest);
            logger.fine("Uploading - " + config.getQueueName() + " for " + putObjectRequest.getKey());
            uploadSuccessful = true;
        } catch (AmazonS3Exception e) {
            logger.warning("Unable to upload file " + e);
        }
        return uploadSuccessful;
    }

    @Override
    public boolean download(File file) {
        if (!file.isDirectory()) {
            GetObjectRequest request = new GetObjectRequest(bucket, getFilePath(file));
            boolean successful = downloadFromS3(request, file);
            if (successful) {
                justDownloaded.add(file);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected boolean downloadFromS3(GetObjectRequest getObjectRequest, File file) {
        boolean downloadSuccessful = false;
        try {
            client.getObject(getObjectRequest, file);
            logger.info("Downloading - " + config.getQueueName() + " for " + getObjectRequest.getKey());
            downloadSuccessful = true;
        } catch (AmazonS3Exception e) {
            logger.warning(" Unable to download file, deleted maybe? " + e);
        }
        return downloadSuccessful;
    }

    @Override
    public boolean delete(File file) {
        DeleteObjectRequest request = new DeleteObjectRequest(bucket, getFilePath(file));
        return deleteInS3(request);
    }

    @Override
    public boolean deleteLocalFile(File file) {
        return file.delete();
    }

    @Override
    public boolean createLocalDirectory(File file) {
        justDownloaded.add(file);
        return file.mkdir();
    }

    protected boolean deleteInS3(DeleteObjectRequest deleteObjectRequest) {
        client.deleteObject(deleteObjectRequest);
        return true;
    }

    private String getFilePath(File file) {
        String path = config.getBaseDir().relativize(file.toPath()).toString();
        if (file.isDirectory()) {
            // The trailing "/" is kinda important for S3 to
            // know that it is a directory
            path += "/";
        }
        return path;
    }
}
