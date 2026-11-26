
package com.javadai.agent.workspace;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {

    private final WorkspaceManager workspaceManager;

    public WorkspaceController(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @GetMapping
    public Map<String, String> getWorkspace() {
        return workspaceManager.getWorkspace();
    }

    @PutMapping
    public Map<String, String> setWorkspace(
            @RequestBody Map<String, String> request
    ) throws IOException {
        return workspaceManager.setWorkspace(request.get("path"));
    }

    @GetMapping("/files")
    public Map<String, Object> getFiles(
            @RequestParam(name = "path", defaultValue = "") String path
    ) throws IOException {

        Path root = workspaceManager.getRoot();
        Path target = root.resolve(path).normalize();

        if (!target.startsWith(root)) {
            throw new SecurityException("Access outside workspace is forbidden");
        }

        if (!Files.isDirectory(target)) {
            throw new IllegalArgumentException("Directory not found");
        }

        Path realTarget = target.toRealPath();

        if (!realTarget.startsWith(root)) {
            throw new SecurityException("Access outside workspace is forbidden");
        }

        List<Map<String, Object>> files;

        try (var stream = Files.list(realTarget)) {
            files = stream
                    .filter(file -> file.toAbsolutePath().normalize()
                            .startsWith(root))
                    .sorted(Comparator
                            .comparing((Path file) -> !Files.isDirectory(file))
                            .thenComparing(file ->
                                    file.getFileName().toString(),
                                    String.CASE_INSENSITIVE_ORDER))
                    .map(file -> Map.<String, Object>of(
                            "name", file.getFileName().toString(),
                            "path", root.relativize(file.toAbsolutePath()
                                    .normalize()).toString().replace("\\", "/"),
                            "directory", Files.isDirectory(file)
                    ))
                    .toList();
        }

        return Map.of(
                "path", root.relativize(realTarget)
                        .toString().replace("\\", "/"),
                "files", files
        );
    }

    @GetMapping("/file")
    public Map<String, String> readFile(
            @RequestParam(name = "path") String path
    ) throws IOException {

        Path file = workspaceManager.resolve(path);

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File not found");
        }

        if (Files.size(file) > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("File is too large");
        }

        return Map.of(
                "path", path,
                "content", Files.readString(file)
        );
    }

    @PutMapping("/file")
    public Map<String, String> saveFile(
            @RequestBody Map<String, String> request
    ) throws IOException {

        String path = request.get("path");
        String content = request.get("content");

        if (path == null || path.isBlank() || content == null) {
            throw new IllegalArgumentException("Path and content are required");
        }

        Path file = workspaceManager.resolve(path);

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File not found");
        }

        Files.writeString(file, content);

        return Map.of("message", "File saved successfully");
    }
}
