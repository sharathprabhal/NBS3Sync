package com.example.services;


import com.example.config.Config;
import com.example.services.impl.S3FileManager;
import com.example.utils.TestingUtils;
import com.amazonaws.services.s3.model.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;


public class TestS3FileManager {

    private Stack<PutObjectRequest> putObjectRequests = new Stack<PutObjectRequest>();
    private Stack<GetObjectRequest> getObjectRequests = new Stack<GetObjectRequest>();
    private Stack<DeleteObjectRequest> deleteObjectRequests = new Stack<DeleteObjectRequest>();
    private Config config = TestingUtils.getMockConfig();

    S3FileManager fileManager = new S3FileManager(config) {

        protected boolean isFirstTime() {
            return false;
        }

        protected boolean uploadToS3(PutObjectRequest putObjectRequest) {
            putObjectRequests.add(putObjectRequest);
            return true;
        }

        protected boolean downloadFromS3(GetObjectRequest getObjectRequest, File file) {
            getObjectRequests.add(getObjectRequest);
            return true;
        }

        protected boolean deleteInS3(DeleteObjectRequest deleteObjectRequest) {
            deleteObjectRequests.add(deleteObjectRequest);
            return true;
        }
    };

    @Test
    public void testUploadFile() {
        File f = new File("/foo/bar/dir/filename");
        fileManager.upload(f);

        assertEquals("file was not uploaded", 1, putObjectRequests.size());

        PutObjectRequest request = putObjectRequests.pop();
        assertEquals("incorrect bucket name", "foo", request.getBucketName());
        assertEquals("incorrect filepath", "dir/filename", request.getKey());
        assertEquals("incorrect file", f, request.getFile());

    }

    @Test
    public void testUploadDirectory() throws IOException {
        File dir = TestingUtils.createTempDirectory("tmp").toFile();
        fileManager.upload(dir);

        assertEquals("folder was not uploaded", 1, putObjectRequests.size());

        PutObjectRequest request = putObjectRequests.pop();
        assertEquals("incorrect bucket name", "foo", request.getBucketName());
        assertTrue("incorrect filepath", request.getKey().startsWith("../../"));
        assertTrue("incorrect filepath", request.getKey().endsWith("/"));
        assertEquals("dir content isnt 0", 0, request.getMetadata().getContentLength());
    }

    @Test
    public void testDownloadFile() {
        File f = new File("/foo/bar/dir/filename");
        fileManager.download(f);

        assertEquals("file was not downloaded", 1, getObjectRequests.size());
        GetObjectRequest request = getObjectRequests.pop();
        assertEquals("incorrect bucket name", "foo", request.getBucketName());
        assertEquals("incorrect filepath", "dir/filename", request.getKey());
    }

    @Test
    public void testDownloadFolder() throws IOException {
        File dir = TestingUtils.createTempDirectory("tmp").toFile();
        fileManager.download(dir);

        assertEquals("folder should not be downloaded", 0, getObjectRequests.size());
    }

    @Test
    public void testDeleteFile() {
        File f = new File("/foo/bar/dir/filename");
        fileManager.delete(f);

        assertEquals("file was not deleted", 1, deleteObjectRequests.size());
        DeleteObjectRequest request = deleteObjectRequests.pop();
        assertEquals("incorrect bucket name", "foo", request.getBucketName());
        assertEquals("incorrect filepath", "dir/filename", request.getKey());
    }

    @Test
    public void testDeleteDirectory() throws IOException {
        File dir = TestingUtils.createTempDirectory("tmp").toFile();
        fileManager.delete(dir);

        assertEquals("file was not deleted", 1, deleteObjectRequests.size());
        DeleteObjectRequest request = deleteObjectRequests.pop();
        assertEquals("incorrect bucket name", "foo", request.getBucketName());
        assertTrue("incorrect filepath", request.getKey().startsWith("../../"));
        assertTrue("incorrect filepath", request.getKey().endsWith("/"));
    }

    @Test
    public void testNotUploadDownloadedFile() {
        File f = new File("/foo/bar/dir/filename");
        fileManager.download(f);

        assertFalse("should not upload as it was just downloaded", fileManager.shouldDownload(f));
        fileManager.upload(f);
        assertTrue("should not upload as it was just downloaded", fileManager.shouldDownload(f));

    }
}
