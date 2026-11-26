
package com.javadai.agent.workspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class WorkspaceManager {

    private volatile Path workspace;

    public WorkspaceManager(
            @Value("${javadai.workspace:D:/JavadAI/workspace}")
            String initialWorkspace
    ) throws IOException {
        this.workspace = initialize(initialWorkspace);
    }

    private Path initialize(String value) throws IOException {
        Path path = Path.of(value).toAbsolutePath().normalize();

        Files.createDirectories(path);

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Workspace must be a directory");
        }

        return path.toRealPath();
    }

    public synchronized Map<String, String> getWorkspace() {
        return Map.of("path", workspace.toString());
    }

    public synchronized Map<String, String> setWorkspace(String value)
            throws IOException {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Workspace path is required");
        }

        Path newPath = Path.of(value).toAbsolutePath().normalize();

        if (!Files.exists(newPath) || !Files.isDirectory(newPath)) {
            throw new IllegalArgumentException(
                    "Workspace directory does not exist"
            );
        }

        workspace = newPath.toRealPath();

        return getWorkspace();
    }

    public Path getRoot() {
        return workspace;
    }

    public Path resolve(String relativePath) throws IOException {
        Path root = workspace;
        Path resolved = root.resolve(relativePath).normalize();

        if (!resolved.startsWith(root)) {
            throw new SecurityException("Access outside workspace is forbidden");
        }

        Path real = resolved.toRealPath();

        if (!real.startsWith(root)) {
            throw new SecurityException("Symbolic link outside workspace");
        }

        return real;
    }
}
