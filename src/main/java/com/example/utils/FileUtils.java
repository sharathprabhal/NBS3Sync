package com.example.utils;

import java.io.File;

public class FileUtils {

    /**
     * Conditions for files that don't have to be synced
     * @param file The file to check
     * @return Should the file be ignored?
     */
    public static boolean shouldIgnore(File file) {
        boolean shouldIgnore = false;
        // Ignore hidden files
        if (file.isHidden()) {
            shouldIgnore = true;
        }
        return shouldIgnore;
    }
}
