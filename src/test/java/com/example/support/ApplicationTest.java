package com.example.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ApplicationTest {

    @Test
    void demoRunsEndToEndAndShowsTheMergeScenario() throws Exception {
        String output = runApplication();

        assertTrue(output.contains("Merge duplicate tickets"), output);
        assertTrue(output.contains("--- Merged from"), output);
        assertTrue(output.contains("MERGED"), output);
    }

    @Test
    void demoShowsThatMergingATicketWithItselfIsRejected() throws Exception {
        String output = runApplication();

        assertTrue(output.contains("Self merge"), output);
        assertTrue(output.contains("409"), output);
    }

    @Test
    void demoDoesNotFailWithAnUnhandledError() throws Exception {
        String output = runApplication();

        assertFalse(output.contains("Exception"), output);
    }

    private static String runApplication() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            Application.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}
