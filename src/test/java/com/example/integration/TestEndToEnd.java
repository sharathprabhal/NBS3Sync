package com.example.integration;

import com.example.NBS3Sync;
import com.example.utils.TestingUtils;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

// Comment out @Ignore to run end to end. You need valid AWS creds for this. A free tier account will do.
@Ignore
public class TestEndToEnd {

    private static final Logger logger = Logger.getLogger(TestEndToEnd.class.getName());

    private static Path base1;
    private static Path base2;

    private static final String AWS_ACCESS_KEY = "";
    private static final String AWS_SECRET = "";
    // make sure this directory is created before running this..
    private static final String BUCKET = "";

    @BeforeClass
    public static void setupOnce() throws IOException {
        base1 = TestingUtils.createTempDirectory("base1");
        base1.toFile().deleteOnExit();
        base2 = TestingUtils.createTempDirectory("base2");
        base2.toFile().deleteOnExit();

        Path config1 = TestingUtils.getConfigFile(AWS_ACCESS_KEY, AWS_SECRET, base1.toAbsolutePath().toString(), BUCKET, "q1");
        Path config2 = TestingUtils.getConfigFile(AWS_ACCESS_KEY, AWS_SECRET, base2.toAbsolutePath().toString(), BUCKET, "q2");

        new NBS3Sync(config1);
        new NBS3Sync(config2);
    }

    @Test
    public void testCreateModifyFile() throws IOException {

        Path tmpFileInA = TestingUtils.createTempFile(base1, "temp", ".tmp");

        modifyFile(tmpFileInA);
        assertFalse(compareDirs());

        pause(60);
        assertTrue(compareDirs());
    }

    @Test
    public void testCreateModifyFileInsideFolder() throws IOException {

        Path tmpFolderInA = TestingUtils.createTempDirectory(base1, "tmpA");
        pause(5);
        Path tmpFileInsideA =  TestingUtils.createTempFile(tmpFolderInA, "temp", ".tmp");
        pause(5);
        modifyFile(tmpFileInsideA, "new content for folder inside file");
        assertFalse(compareDirs());

        pause(60);
        assertTrue(compareDirs());
    }

    @Test
    public void testMultipleModifyFile() throws IOException {

        Path tmpFileInA =  TestingUtils.createTempFile(base1, "temp", ".tmp");
        pause(5);
        modifyFile(tmpFileInA, "first");
        pause(5);
        modifyFile(tmpFileInA, "second");
        pause(5);
        modifyFile(tmpFileInA, "new third content");
        assertFalse(compareDirs());
        pause(100);
        assertTrue(compareDirs());
    }

    @Test
    public void testBothDirections() throws IOException {
        Path tmpFileInA =  TestingUtils.createTempFile(base1, "temp1_", ".tmp");
        pause(5);
        modifyFile(tmpFileInA);

        Path tmpFileInB =  TestingUtils.createTempFile(base2, "temp2_", ".tmp");
        pause(5);
        modifyFile(tmpFileInB);

        pause(100);
        assertTrue(compareDirs());
    }

    @Test
    public void testDelete() throws IOException {
        Path tmpFileInA =  TestingUtils.createTempFile(base1, "temp1_", ".tmp");
        pause(5);
        modifyFile(tmpFileInA);
        pause(5);
        assertTrue(tmpFileInA.toFile().delete());
        pause(45);
        assertTrue(compareDirs());
    }

    private void modifyFile(Path p) throws IOException{
       modifyFile(p, "randomString");

    }

    private void modifyFile(Path p, final String str) throws IOException{
        File f = p.toFile();
        List<String> lines = new ArrayList<String>() {
            {
                add(str);
            }
        };
        FileUtils.writeLines(f, lines);

    }

    private void pause(int timeInSeconds) {
        try {
            Thread.sleep(timeInSeconds * 1000);
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    private long getChecksumDir(File dir) throws IOException{
        long checksum = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for(File file: files) {
                if (file.isDirectory()) {
                    return getChecksumDir(file);
                } else {
                    checksum += FileUtils.checksumCRC32(file);
                }
            }
        }
        return checksum;
    }

    private boolean compareDirs() {
        try {
            long c1 = getChecksumDir(base1.toFile());
            long c2 = getChecksumDir(base2.toFile());
            return c1 == c2;
        } catch (Exception e) {
            logger.severe(e.toString());
            return false;
        }
    }
}
