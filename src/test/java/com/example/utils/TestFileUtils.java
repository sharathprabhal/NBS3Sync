package com.example.utils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

public class TestFileUtils {

    @Test
    public void testIfTempFileIsIgnored() throws IOException {
        File f = File.createTempFile(".hiddenTemp", ".tmp");
        f.deleteOnExit();
        assertTrue("should ignore", FileUtils.shouldIgnore(f));
    }
}
