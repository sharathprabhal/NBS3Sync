package com.example.utils;

import com.example.config.Config;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.*;

public class TestConfig {

    private static File tempFile;

    private static String prefix = "temp";
    private static String suffix = ".properties";

    private static String sampleProperties = "prop1=prop1value\nprop2=prop2value";

    private Config config;


    @BeforeClass
    public static void setupOnce() {
        try {
            tempFile = TestingUtils.createTempFile(prefix, suffix).toFile();
            tempFile.deleteOnExit();

            PrintWriter printWriter = new PrintWriter(tempFile);
            printWriter.write(sampleProperties);
            printWriter.close();

        } catch (Exception e) {}

    }

    @Before
    public void setup() {
        config = new Config(tempFile.toPath());
    }

    @Test
    public void testPropertiesGet() throws IOException{
        assertEquals("prop1 is invalid", "prop1value", config.getProperty("prop1"));
        assertEquals("prop2 is invalid", "prop2value", config.getProperty("prop2"));
        assertEquals("foo is invalid, should be null", null, config.getProperty("foo"));
    }

}
