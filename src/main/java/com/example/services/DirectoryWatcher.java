package com.example.services;

import java.nio.file.Path;

/**
 * Watches a directory for changes and tells the <code>Publisher</code> when something changes
 */
public interface DirectoryWatcher {
    /**
     * A directory to register. This is a recursive operation
     * @param path The directory to watch
     */
    public void registerDirectory(Path path);
}
