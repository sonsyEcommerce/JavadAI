
package com.javadai.agent.memory.dto;

import com.javadai.agent.memory.MemoryType;

public record CreateMemoryRequest(
        MemoryType memoryType,
        String title,
        String content,
        String source,
        Integer importance
) {
}
