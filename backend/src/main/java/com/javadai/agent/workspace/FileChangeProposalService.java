
package com.javadai.agent.workspace;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileChangeProposalService {

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    public record Proposal(
            UUID id,
            String path,
            String originalContent,
            String proposedContent,
            Status status,
            Instant createdAt
    ) {}

    private static final int MAX_FILE_SIZE = 1_000_000;

    private final WorkspaceManager workspaceManager;

    private final Map<UUID, Proposal> proposals =
            new ConcurrentHashMap<>();

    public FileChangeProposalService(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public Proposal propose(String relativePath, String content)
            throws IOException {

        if (relativePath == null || relativePath.isBlank()
                || content == null) {
            throw new IllegalArgumentException("Path and content are required");
        }

        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Proposed file is too large");
        }

        Path target = resolveSafe(relativePath);
        String original = null;

        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(target)
                    || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new SecurityException("Unsafe target file");
            }

            if (Files.size(target) > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Original file is too large");
            }

            original = Files.readString(target, StandardCharsets.UTF_8);
        }

        Proposal proposal = new Proposal(
                UUID.randomUUID(),
                normalize(relativePath),
                original,
                content,
                Status.PENDING,
                Instant.now()
        );

        proposals.put(proposal.id(), proposal);
        return proposal;
    }

    public List<Proposal> getPending() {
        return proposals.values().stream()
                .filter(p -> p.status() == Status.PENDING)
                .sorted(Comparator.comparing(Proposal::createdAt).reversed())
                .toList();
    }

    public Proposal get(UUID id) {
        Proposal proposal = proposals.get(id);

        if (proposal == null) {
            throw new IllegalArgumentException("Proposal not found");
        }

        return proposal;
    }

    public Proposal approve(UUID id) throws IOException {
        Proposal proposal = get(id);

        if (proposal.status() != Status.PENDING) {
            throw new IllegalStateException("Proposal is no longer pending");
        }

        Path target = resolveSafe(proposal.path());
        boolean exists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);

        if (proposal.originalContent() == null) {
            if (exists) {
                throw new IllegalStateException(
                        "File was created after the proposal"
                );
            }
        } else {
            if (!exists
                    || Files.isSymbolicLink(target)
                    || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("Original file changed");
            }

            String current = Files.readString(target, StandardCharsets.UTF_8);

            if (!current.equals(proposal.originalContent())) {
                throw new IllegalStateException(
                        "File changed after proposal. Create a new proposal."
                );
            }
        }

        Path parent = target.getParent();
        Path temp = Files.createTempFile(parent, ".javadai-", ".tmp");

        try {
            Files.writeString(
                    temp,
                    proposal.proposedContent(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            if (proposal.originalContent() == null) {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } else {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (AtomicMoveNotSupportedException ex) {
            if (proposal.originalContent() == null) {
                Files.move(temp, target);
            } else {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }

        Proposal approved = new Proposal(
                proposal.id(),
                proposal.path(),
                proposal.originalContent(),
                proposal.proposedContent(),
                Status.APPROVED,
                proposal.createdAt()
        );

        proposals.put(id, approved);
        return approved;
    }

    public Proposal reject(UUID id) {
        Proposal proposal = get(id);

        if (proposal.status() != Status.PENDING) {
            throw new IllegalStateException("Proposal is no longer pending");
        }

        Proposal rejected = new Proposal(
                proposal.id(),
                proposal.path(),
                proposal.originalContent(),
                proposal.proposedContent(),
                Status.REJECTED,
                proposal.createdAt()
        );

        proposals.put(id, rejected);
        return rejected;
    }

    private Path resolveSafe(String relativePath) throws IOException {
        Path root = workspaceManager.getRoot().toRealPath();
        Path relative = Path.of(relativePath);

        if (relative.isAbsolute()) {
            throw new SecurityException("Absolute paths are forbidden");
        }

        for (Path part : relative) {
            if (part.toString().equals("..")) {
                throw new SecurityException("Path traversal is forbidden");
            }
        }

        Path target = root.resolve(relative).normalize();

        if (!target.startsWith(root) || target.equals(root)) {
            throw new SecurityException("Access outside workspace is forbidden");
        }

        Path parent = target.getParent();

        if (parent == null || !Files.isDirectory(parent)) {
            throw new IllegalArgumentException("Parent directory does not exist");
        }

        Path realParent = parent.toRealPath();

        if (!realParent.startsWith(root)) {
            throw new SecurityException("Parent directory is outside workspace");
        }

        Path current = root;

        for (Path part : root.relativize(target)) {
            current = current.resolve(part);

            if (Files.isSymbolicLink(current)) {
                throw new SecurityException("Symbolic links are not allowed");
            }
        }

        return target;
    }

    private String normalize(String path) {
        return path.replace('\\', '/');
    }
}
