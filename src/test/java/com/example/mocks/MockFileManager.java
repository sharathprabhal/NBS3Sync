package com.example.mocks;

import com.example.services.FileManager;

import java.io.File;
import java.util.Stack;

public class MockFileManager implements FileManager {

    public Stack<File> uploaded = new Stack<File>();
    public Stack<File> downloaded = new Stack<File>();
    public Stack<File> deleted = new Stack<File>();
    public Stack<File> deletedLocalFiles = new Stack<File>();
    public Stack<File> createDirectories = new Stack<File>();

    @Override
    public boolean upload(File file) {
        uploaded.push(file);
        return true;
    }

    @Override
    public boolean download(File file) {
        downloaded.push(file);
        return true;
    }

    @Override
    public boolean delete(File file) {
        deleted.push(file);
        return true;
    }

    @Override
    public boolean deleteLocalFile(File file) {
        deletedLocalFiles.push(file);
        return true;
    }

    @Override
    public boolean createLocalDirectory(File file) {
        createDirectories.push(file);
        return true;
    }
}
