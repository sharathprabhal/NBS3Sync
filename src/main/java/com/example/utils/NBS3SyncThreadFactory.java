package com.example.utils;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class NBS3SyncThreadFactory {

    public static String THREAD_NAME_PREFIX = "NBS3_SYNC_";

    private static ThreadFactory tf = Executors.defaultThreadFactory();

    public static Thread newThread(String name, Runnable runnable) {
        Thread t = tf.newThread(runnable);
        t.setName(name);
        return t;
    }

    public static Thread newThread(Runnable r) {
        return newThread(THREAD_NAME_PREFIX + Integer.toString(new Random().nextInt()), r);
    }
}
