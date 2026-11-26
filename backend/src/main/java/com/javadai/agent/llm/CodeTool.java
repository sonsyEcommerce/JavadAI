package com.javadai.agent.llm;

import com.javadai.agent.workspace.WorkspaceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Read-only code understanding helpers: project tree and content search. Stays inside the workspace. */
@Component
public class CodeTool {

    private static final Set<String> SKIP = Set.of("node_modules", ".git", ".angular", "target", "dist", "build", ".idea", ".vscode");
    private static final Set<String> TEXT_EXT = Set.of("java", "ts", "html", "scss", "css", "json", "xml", "yaml", "yml",
            "md", "txt", "sql", "properties", "py", "js", "kt", "gradle");

    private final WorkspaceManager workspaceManager;

    public CodeTool(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    private Path root() {
        return workspaceManager.getRoot();
    }


    private Path safe(String rel) throws IOException {
        Path p = root().resolve(rel == null ? "" : rel).normalize();
        if (!p.startsWith(root())) throw new SecurityException("Access outside workspace is forbidden.");
        return p;
    }

    private boolean skipped(Path p) {
        for (Path part : root().relativize(p)) if (SKIP.contains(part.toString())) return true;
        return false;
    }

    /** Indented tree (max depth 4, max 300 entries) so the model can see the project layout. */
    public String tree(String rel) throws IOException {
        Path start = safe(rel);
        try (Stream<Path> s = Files.walk(start, 4)) {
            return s.filter(p -> !p.equals(start) && !skipped(p)).limit(300)
                    .map(p -> "  ".repeat(start.relativize(p).getNameCount() - 1)
                            + (Files.isDirectory(p) ? p.getFileName() + "/" : p.getFileName()))
                    .collect(Collectors.joining("\n"));
        }
    }

    /** Case-insensitive text search inside source files; returns "path:line: text". */
    public String grep(String text) throws IOException {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text is required");
        String needle = text.toLowerCase();
        StringBuilder out = new StringBuilder();
        try (Stream<Path> s = Files.walk(root())) {
            for (Path p : (Iterable<Path>) s.filter(Files::isRegularFile).filter(x -> !skipped(x))
                    .filter(x -> TEXT_EXT.contains(ext(x)))::iterator) {
                if (Files.size(p) > 500_000) continue;
                var lines = Files.readAllLines(p);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).toLowerCase().contains(needle)) {
                        out.append(root().relativize(p)).append(':').append(i + 1).append(": ")
                           .append(lines.get(i).trim(), 0, Math.min(160, lines.get(i).trim().length())).append('\n');
                        if (out.length() > 6000) return out + "...(truncated)";
                    }
                }
            }
        } catch (java.nio.charset.MalformedInputException ignored) { }
        return out.isEmpty() ? "No matches." : out.toString();
    }

    private static String ext(Path p) {
        String n = p.getFileName().toString(); int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1).toLowerCase();
    }
}
