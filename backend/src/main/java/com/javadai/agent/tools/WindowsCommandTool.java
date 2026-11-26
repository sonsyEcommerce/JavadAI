
package com.javadai.agent.tools;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class WindowsCommandTool {

    private static final List<String> ALLOWED_COMMANDS = List.of(
            "whoami",
            "hostname",
            "date",
            "time",
            "ipconfig",
            "systeminfo",
            "tasklist"
    );

    public String execute(String command)
            throws IOException, InterruptedException {

        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command is required.");
        }

        String normalized = command.trim().toLowerCase();

        if (!ALLOWED_COMMANDS.contains(normalized)) {
            throw new SecurityException("Command is not allowed: " + command);
        }

        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                normalized
        );

        builder.redirectErrorStream(true);

        Process process = builder.start();

        boolean completed = process.waitFor(15, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Command execution timed out.");
        }

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        if (process.exitValue() != 0) {
            throw new IOException(output);
        }

        return output.isBlank() ? "Command executed successfully." : output;
    }
}
