package com.javadai.agent.tools;

import com.javadai.agent.workspace.WorkspaceManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Runs a deliberately small allow-list of project verification commands.
 * Commands always execute from the configured workspace root; arbitrary shell
 * commands and shell operators are rejected.
 */
@Component
public class WorkspaceCommandTool {

    private static final List<Pattern> ALLOWED = List.of(
            Pattern.compile("mvn( -f backend/pom\\.xml)? (clean )?(compile|test|package|verify)( -DskipTests)?"),
            Pattern.compile("npm (run )?(build|test)"),
            Pattern.compile("npm --prefix frontend run (build|test)"),
            Pattern.compile("npx ng (build|test)"),
            Pattern.compile("git (status|diff --check)")
    );

    private final WorkspaceManager workspaceManager;

    public WorkspaceCommandTool(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public String run(String command) throws IOException, InterruptedException {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command is required.");
        }

        String normalized = command.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 240 || ALLOWED.stream().noneMatch(p -> p.matcher(normalized).matches())) {
            throw new SecurityException("Command is not in the allowed project-check list.");
        }

        Path root = workspaceManager.getRoot().toRealPath();
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder builder = windows
                ? new ProcessBuilder("cmd.exe", "/d", "/s", "/c", normalized)
                : new ProcessBuilder(normalized.split(" "));
        builder.directory(root.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();
        CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> {
            try {
                return process.getInputStream().readAllBytes();
            } catch (IOException e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        });

        if (!process.waitFor(3, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            output.cancel(true);
            throw new IOException("Command timed out after 3 minutes.");
        }

        String result;
        try {
            result = new String(output.get(10, TimeUnit.SECONDS), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IOException("Could not collect command output.", e);
        }
        if (result.length() > 12_000) result = result.substring(0, 12_000) + "\\n...(output truncated)";
        if (process.exitValue() != 0) {
            return "ERROR: Command exited with code " + process.exitValue() + "\\n" + result;
        }
        return result.isBlank() ? "Command completed successfully (exit code 0)." : result;
    }
}
