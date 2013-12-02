package com.example.services;

import java.io.File;

/**
 * The <code>FileManager</code> is responsible to file related
 * operation, both local and remote
 */
public interface FileManager {

    /**
     * Uploads the file to remote location
     * @param file The file to upload
     * @return whether the upload was successful
     */
    public boolean upload(File file);

    /**
     * Downloads the file from remote location to local
     * @param file The file to download
     * @return whether the download was successful
     */
    public boolean download(File file);

    /**
     * Delete the file at the remote location
     * @param file The file to delete
     * @return whether the file was deleted successfully
     */
    public boolean delete(File file);

    /**
     * Delete a file in the local file system
     * @param file The file to delete
     * @return whether the file was deleted successfully
     */
    public boolean deleteLocalFile(File file);

    /**
     * Creates a directory in the local file system
     * @param file The directory to create
     * @return whether directory was created successfully
     */
    public boolean createLocalDirectory(File file);
}
