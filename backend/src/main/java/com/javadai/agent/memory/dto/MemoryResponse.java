
package com.javadai.agent.memory.dto;

import com.javadai.agent.memory.Memory;
import com.javadai.agent.memory.MemoryType;

import java.time.Instant;
import java.util.UUID;

public record MemoryResponse(
        UUID id,
        MemoryType memoryType,
        String title,
        String content,
        String source,
        Integer importance,
        Boolean active,
        Integer accessCount,
        Instant createdAt,
        Instant updatedAt,
        Instant lastAccessedAt
) {
    public static MemoryResponse from(Memory memory) {
        return new MemoryResponse(
                memory.getId(),
                memory.getMemoryType(),
                memory.getTitle(),
                memory.getContent(),
                memory.getSource(),
                memory.getImportance(),
                memory.getActive(),
                memory.getAccessCount(),
                memory.getCreatedAt(),
                memory.getUpdatedAt(),
                memory.getLastAccessedAt()
        );
    }
}
