package com.example.services.impl;

import com.example.services.Event;
import com.example.services.FileOperation;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class S3FileEvent implements Event {

    private FileOperation operation;
    private File file;
    private boolean isDirectory;
    private String originator;

    private static Map<String, FileOperation> operationAsStringMap = new HashMap<String, FileOperation>();
    private static Map<WatchEvent.Kind, FileOperation> operationAsKindMap = new HashMap<WatchEvent.Kind, FileOperation>();

    static {
        operationAsKindMap.put(ENTRY_CREATE, FileOperation.CREATE);
        operationAsKindMap.put(ENTRY_DELETE, FileOperation.DELETE);
        operationAsKindMap.put(ENTRY_MODIFY, FileOperation.MODIFY);

        operationAsStringMap.put("CREATE", FileOperation.CREATE);
        operationAsStringMap.put("DELETE", FileOperation.DELETE);
        operationAsStringMap.put("MODIFY", FileOperation.MODIFY);
    }

    public S3FileEvent(FileOperation operation, File file, String originator) {
        this.operation = operation;
        this.file = file;
        this.isDirectory = file.isDirectory();
        this.originator = originator;
    }

    public S3FileEvent(FileOperation operation, File file,  String originator, boolean isDirectory) {
        this(operation, file, originator);
        this.isDirectory = isDirectory;
    }

    @Override
    public FileOperation getOperation() {
        return operation;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public String getOriginator() {
        return originator;
    }

    public String toJson(Path baseDir) {
        JsonObject o = new JsonObject();
        o.addProperty("operation", this.getOperation().toString());
        Path path = baseDir.relativize(file.toPath());
        if (file.isDirectory()) {
            o.addProperty("file", path.toString() + "/");
        } else {
            o.addProperty("file", path.toString());
        }
        o.addProperty("originator", originator);

        return o.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof S3FileEvent)) {
            return false;
        }
        S3FileEvent o = (S3FileEvent)obj;
        return this.operation == o.operation && this.file.getAbsolutePath().equals(o.getFile().getAbsolutePath());
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + operation.ordinal();
        hash = hash * 17 + (isDirectory ? 1 : 0);
        hash = hash * 17 + file.getAbsolutePath().hashCode();
        return hash;
    }

    public static FileOperation getFileOperation(String op) {
        return operationAsStringMap.get(op);
    }

    public static FileOperation getFileOperation(WatchEvent.Kind op) {
        return operationAsKindMap.get(op);
    }
}
