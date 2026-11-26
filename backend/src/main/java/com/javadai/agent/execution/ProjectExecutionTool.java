package com.javadai.agent.execution;

import com.javadai.agent.workspace.WorkspaceManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs only fixed Maven/Angular build and test commands in a project directory
 * contained by the active workspace. No model-provided shell command is accepted.
 */
@Component
public class ProjectExecutionTool {
    private static final Duration TIMEOUT = Duration.ofMinutes(5);
    private static final int OUTPUT_LIMIT = 30_000;

    private final WorkspaceManager workspace;

    public ProjectExecutionTool(WorkspaceManager workspace) {
        this.workspace = workspace;
    }

    public ExecutionResult execute(String projectPath, String check) {
        long started = System.nanoTime();
        Process process = null;
        try {
            Path project = resolveProject(projectPath);
            String kind = detect(project);
            String action = check == null || check.isBlank()
                    ? "build" : check.trim().toLowerCase(Locale.ROOT);
            List<String> command = command(project, kind, action);

            process = new ProcessBuilder(command)
                    .directory(project.toFile())
                    .redirectErrorStream(true)
                    .start();

            Process running = process;
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(running));
            if (!process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.descendants().forEach(ProcessHandle::destroy);
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                }
                String text = output.get(5, TimeUnit.SECONDS);
                return result(false, true, null, text, started);
            }

            String text = output.get(10, TimeUnit.SECONDS);
            int exitCode = process.exitValue();
            return result(exitCode == 0, false, exitCode, text, started);
        } catch (Exception e) {
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
            return result(false, false, -1,
                    "Execution failed: " + e.getMessage(), started);
        }
    }

    private Path resolveProject(String relative) throws IOException {
        Path root = workspace.getRoot().toRealPath();
        Path candidate = root.resolve(relative == null ? "" : relative).normalize();
        if (!candidate.startsWith(root)) {
            throw new SecurityException("Project path is outside workspace.");
        }

        Path real = candidate.toRealPath();
        if (!real.startsWith(root) || !Files.isDirectory(real)) {
            throw new SecurityException("Invalid project directory.");
        }
        return real;
    }

    private String detect(Path project) {
        if (Files.isRegularFile(project.resolve("pom.xml"))) {
            return "maven";
        }
        if (Files.isRegularFile(project.resolve("package.json"))
                && Files.isRegularFile(project.resolve("angular.json"))) {
            return "angular";
        }
        throw new IllegalArgumentException(
                "No supported project found. Expected pom.xml or Angular package.json + angular.json.");
    }

    private List<String> command(Path project, String kind, String action) throws IOException {
        if (!action.equals("build") && !action.equals("test")) {
            throw new IllegalArgumentException("Supported checks are build and test.");
        }

        boolean windows = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT).contains("win");
        List<String> args = new ArrayList<>();
        Path wrapper = project.resolve(windows ? "mvnw.cmd" : "mvnw");

        if (kind.equals("maven")) {
            if (Files.isRegularFile(wrapper)) {
                Path realWrapper = wrapper.toRealPath();
                if (!realWrapper.startsWith(workspace.getRoot().toRealPath())) {
                    throw new SecurityException("Maven wrapper resolves outside workspace.");
                }
                if (windows) {
                    return windowsCommand(realWrapper.toString(),
                            action.equals("test") ? "test" : "-DskipTests package");
                }
                args.add("sh");
                args.add(realWrapper.toString());
            } else if (windows) {
                return windowsCommand("mvn.cmd",
                        action.equals("test") ? "test" : "-DskipTests package");
            } else {
                args.add("mvn");
            }
            args.add(action.equals("test") ? "test" : "-DskipTests");
            if (action.equals("build")) {
                args.add("package");
            }
            return args;
        }

        if (windows) {
            return windowsCommand("npm.cmd",
                    action.equals("build") ? "run build" : "exec ng test -- --watch=false");
        }
        args.add("npm");
        if (action.equals("build")) {
            args.add("run");
            args.add("build");
        } else {
            args.add("exec");
            args.add("ng");
            args.add("test");
            args.add("--");
            args.add("--watch=false");
        }
        return args;
    }

    private List<String> windowsCommand(String executable, String arguments) {
        return List.of("cmd.exe", "/d", "/s", "/c",
                "\"" + executable + "\" " + arguments);
    }

    private String readOutput(Process process) {
        try (var input = process.getInputStream();
             var output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int stored = 0;
            boolean truncated = false;
            int count;
            while ((count = input.read(buffer)) != -1) {
                int keep = Math.min(count, OUTPUT_LIMIT - stored);
                if (keep > 0) {
                    output.write(buffer, 0, keep);
                    stored += keep;
                }
                if (keep < count) {
                    truncated = true;
                }
            }
            String value = output.toString(StandardCharsets.UTF_8);
            return truncated ? value + "\n...[output truncated]" : value;
        } catch (IOException e) {
            return "Unable to read process output: " + e.getMessage();
        }
    }

    private ExecutionResult result(
            boolean successful, boolean timedOut, Integer exitCode, String output, long started) {
        return new ExecutionResult(successful, timedOut, exitCode, output, "",
                Duration.ofNanos(System.nanoTime() - started));
    }
}
