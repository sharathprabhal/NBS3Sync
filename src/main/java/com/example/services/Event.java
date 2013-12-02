package com.example.services;

import java.io.File;

/**
 * An event that is passed between different parts of the system.
 * Holds information about the file and operation
 */
public interface Event {

    /**
     * Returns the <code>FileOperation</code>
     * @return the file operation
     */
    public FileOperation getOperation();

    /**
     * Returns the file
     * @return the file
     */
    public File getFile();

    /**
     * Returns whether the file is a directory
     * @return
     */
    public boolean isDirectory();

    /**
     * Returns who originated the event
     * @return String representation of the originator
     */
    public String getOriginator();
}
