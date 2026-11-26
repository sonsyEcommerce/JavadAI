
package com.javadai.agent.tools;

import com.javadai.agent.workspace.WorkspaceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class FileTool {


    private final WorkspaceManager workspaceManager;

    public FileTool(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }


    private Path resolveSafePath(String relativePath, boolean mustExist)
            throws IOException {

        Path target = workspaceManager.getRoot()
        .resolve(relativePath)
        .normalize();

        if (!target.startsWith(workspaceManager.getRoot())) {
            throw new SecurityException("Access outside workspace is forbidden.");
        }

        if (mustExist) {
            target = target.toRealPath();

            if (!target.startsWith(workspaceManager.getRoot().toRealPath())) {
                throw new SecurityException("Access outside workspace is forbidden.");
            }
        } else {
            Path parent = target.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
                Path realParent = parent.toRealPath();

                if (!realParent.startsWith(workspaceManager.getRoot().toRealPath())) {
                    throw new SecurityException("Access outside workspace is forbidden.");
                }
            }
        }

        return target;
    }

    public List<String> listFiles(String relativePath) throws IOException {
        Path directory = resolveSafePath(relativePath, true);

        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Path is not a directory.");
        }

        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .map(path -> (Files.isDirectory(path) ? "[DIR] " : "[FILE] ")
                            + path.getFileName())
                    .sorted()
                    .toList();
        }
    }

    public String readFile(String relativePath) throws IOException {
        Path root = workspaceManager.getRoot();

        Path file = root.resolve(relativePath)
                .normalize();

        System.out.println("===== READ FILE DEBUG =====");
        System.out.println("Workspace root: " + root);
        System.out.println("Received path: " + relativePath);
        System.out.println("Resolved path: " + file);
        System.out.println("File exists: " + Files.exists(file));
        System.out.println("===========================");

        file = resolveSafePath(relativePath, true);

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Path is not a file.");
        }

        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public String createDirectory(String relativePath) throws IOException {
        Path directory = resolveSafePath(relativePath, false);

        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                    "Path exists and is not a directory: " + relativePath
            );
        }

        boolean alreadyExists = Files.isDirectory(directory);

        Files.createDirectories(directory);

        return alreadyExists
                ? "Directory already exists: " + relativePath
                : "Directory created successfully: " + relativePath;
    }

    public String writeFile(String relativePath, String content)
            throws IOException {

        Path file = resolveSafePath(relativePath, false);

        if (Files.exists(file)) {
            Path realFile = file.toRealPath();

            if (!realFile.startsWith(workspaceManager.getRoot().toRealPath())
                    || Files.isSymbolicLink(file)) {
                throw new SecurityException("Unsafe file path.");
            }
        }

        Files.writeString(
                file,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        return "File saved successfully: " + relativePath;
    }

    public List<String> searchFiles(String keyword) throws IOException {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword is required.");
        }

        try (Stream<Path> stream = Files.walk(workspaceManager.getRoot())) {
            return stream
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString()
                            .toLowerCase()
                            .contains(keyword.toLowerCase()))
                    .limit(500)
                    .map(path -> workspaceManager.getRoot().relativize(path).toString())
                    .toList();
        }
    }

    public String writeNewFile(String relativePath, String content)
            throws IOException {

        Path file = resolveSafePath(relativePath, false);

        Files.writeString(
                file,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        return "File created successfully: " + relativePath;
    }

}
