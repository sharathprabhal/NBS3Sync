package com.example.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestNBS3SyncThreadFactory {

    private Runnable testRunnable = new Runnable() {
        @Override
        public void run() {
            // Do nothing
        }
    };

    @Test
    public void testNewThreadWithName() {
        Thread t = NBS3SyncThreadFactory.newThread("namedThread", testRunnable);
        assertEquals("thread name is incorrect", "namedThread", t.getName());
    }

    @Test
    public void testNewThreadWithoutName() {
        Thread t = NBS3SyncThreadFactory.newThread(testRunnable);
        assertTrue("thread name is incorrect", t.getName().startsWith(NBS3SyncThreadFactory.THREAD_NAME_PREFIX));
    }
}
