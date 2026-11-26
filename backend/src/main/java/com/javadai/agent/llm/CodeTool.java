package com.javadai.agent.llm;

import com.javadai.agent.workspace.WorkspaceManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Workspace-contained project inspection helpers. */
@Component
public class CodeTool {

    private static final Set<String> SKIP = Set.of(
            "node_modules", ".git", ".angular", "target", "dist", "build",
            ".idea", ".vscode", ".gradle", "coverage", "vendor"
    );
    private static final Set<String> TEXT_EXT = Set.of(
            "java", "ts", "tsx", "html", "scss", "css", "json", "xml", "yaml", "yml",
            "md", "txt", "sql", "properties", "py", "js", "jsx", "kt", "gradle", "sh", "bat"
    );
    private static final int MAX_TREE_ENTRIES = 500;
    private static final int MAX_SEARCH_CHARS = 12_000;

    private final WorkspaceManager workspaceManager;

    public CodeTool(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    private Path root() {
        return workspaceManager.getRoot();
    }

    private Path safe(String relative) throws IOException {
        Path base = root().toRealPath();
        Path path = base.resolve(relative == null ? "" : relative).normalize();
        if (!path.startsWith(base)) {
            throw new SecurityException("Access outside workspace is forbidden.");
        }
        Path real = path.toRealPath();
        if (!real.startsWith(base)) {
            throw new SecurityException("Symbolic link outside workspace is forbidden.");
        }
        return real;
    }

    private boolean skipped(Path path) {
        Path relative = root().relativize(path);
        for (Path part : relative) {
            if (SKIP.contains(part.toString())) return true;
        }
        return false;
    }

    /** Project tree deep enough to expose typical Java/Angular source roots. */
    public String tree(String relative) throws IOException {
        Path start = safe(relative);
        if (!Files.isDirectory(start)) {
            throw new IllegalArgumentException("Path is not a directory: " + relative);
        }
        List<String> lines = new ArrayList<>();
        Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), 10, new SimpleFileVisitor<>() {
            private FileVisitResult add(Path path, boolean directory) {
                if (lines.size() >= MAX_TREE_ENTRIES) return FileVisitResult.TERMINATE;
                if (!path.equals(start)) {
                    int depth = start.relativize(path).getNameCount();
                    lines.add("  ".repeat(Math.max(0, depth - 1))
                            + path.getFileName() + (directory ? "/" : ""));
                }
                return lines.size() >= MAX_TREE_ENTRIES ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(start) && skipped(dir)) return FileVisitResult.SKIP_SUBTREE;
                return add(dir, true);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (Files.isSymbolicLink(file) || skipped(file)) return FileVisitResult.CONTINUE;
                return add(file, false);
            }
        });
        if (lines.size() >= MAX_TREE_ENTRIES) lines.add("...(tree truncated)");
        return String.join("\n", lines);
    }

    /** Search source/config files without traversing generated or dependency folders. */
    public String grep(String text) throws IOException {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        String needle = text.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        Path base = root().toRealPath();

        Files.walkFileTree(base, EnumSet.noneOf(FileVisitOption.class), 20, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(base) && skipped(dir)) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (Files.isSymbolicLink(file) || !attrs.isRegularFile() || !TEXT_EXT.contains(ext(file))) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    if (attrs.size() > 500_000) return FileVisitResult.CONTINUE;
                    List<String> lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.toLowerCase(Locale.ROOT).contains(needle)) {
                            String trimmed = line.trim();
                            out.append(base.relativize(file)).append(':').append(i + 1).append(": ")
                                    .append(trimmed, 0, Math.min(200, trimmed.length())).append('\n');
                            if (out.length() >= MAX_SEARCH_CHARS) {
                                out.append("...(search output truncated)");
                                return FileVisitResult.TERMINATE;
                            }
                        }
                    }
                } catch (MalformedInputException ignored) {
                    // Skip binary/non-UTF8 files.
                } catch (IOException ignored) {
                    // A single unreadable file should not abort project search.
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return out.isEmpty() ? "No matches." : out.toString();
    }

    private static String ext(Path path) {
        String name = path.getFileName().toString();
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
