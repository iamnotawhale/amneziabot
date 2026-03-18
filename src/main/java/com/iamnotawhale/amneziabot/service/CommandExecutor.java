package com.iamnotawhale.amneziabot.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CommandExecutor {

    public String run(List<String> command) {
        return run(command, null);
    }

    public String run(List<String> command, String stdinPayload) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            if (stdinPayload != null) {
                try (OutputStream outputStream = process.getOutputStream()) {
                    outputStream.write(stdinPayload.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Command failed: " + String.join(" ", command) + "\n" + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute command: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Command interrupted: " + String.join(" ", command), e);
        }
    }
}
