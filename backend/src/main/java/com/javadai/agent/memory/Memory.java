package com.javadai.agent.memory;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memories")
public class Memory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 40)
    private MemoryType memoryType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String source;

    @Column(nullable = false)
    private Integer importance = 3;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    protected Memory() {
    }

    public Memory(
            MemoryType memoryType,
            String title,
            String content,
            String source,
            Integer importance
    ) {
        if (memoryType == null) {
            throw new IllegalArgumentException("Memory type is required");
        }

        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("Invalid memory title");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Memory content is required");
        }

        int priority = importance == null ? 3 : importance;

        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("Importance must be between 1 and 5");
        }

        this.memoryType = memoryType;
        this.title = title;
        this.content = content;
        this.source = source;
        this.importance = priority;
        this.active = true;
        this.accessCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void registerAccess() {
        if (!Boolean.TRUE.equals(active)) {
            throw new IllegalStateException("Inactive memory cannot be accessed");
        }

        this.accessCount++;
        this.lastAccessedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public MemoryType getMemoryType() {
        return memoryType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }

    public Integer getImportance() {
        return importance;
    }

    public Boolean getActive() {
        return active;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
}