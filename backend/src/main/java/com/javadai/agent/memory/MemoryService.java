package com.javadai.agent.memory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MemoryService {

    private final MemoryRepository memoryRepository;

    public MemoryService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    public Memory remember(
            MemoryType type,
            String title,
            String content,
            String source,
            Integer importance
    ) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "Memory type cannot be null"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Memory title cannot be empty"
            );
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Memory content cannot be empty"
            );
        }

        int priority = importance == null ? 3 : importance;

        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException(
                    "Importance must be between 1 and 5"
            );
        }

        Memory memory = new Memory(
                type,
                title,
                content,
                source,
                priority
        );

        return memoryRepository.save(memory);
    }

    @Transactional(readOnly = true)
    public List<Memory> getRecentMemories(int limit) {

        validateLimit(limit);

        return memoryRepository
                .findAllByActiveTrueOrderByUpdatedAtDesc(
                        PageRequest.of(0, limit)
                )
                .getContent();
    }

    @Transactional(readOnly = true)
    public Page<Memory> search(String keyword, int page, int size) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                    "Search keyword cannot be empty"
            );
        }

        validatePage(page, size);

        return memoryRepository.search(
                keyword.trim(),
                PageRequest.of(page, size)
        );
    }

    @Transactional(readOnly = true)
    public Page<Memory> getByType(
            MemoryType type,
            int page,
            int size
    ) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "Memory type cannot be null"
            );
        }

        validatePage(page, size);

        return memoryRepository
                .findAllByMemoryTypeAndActiveTrueOrderByUpdatedAtDesc(
                        type,
                        PageRequest.of(page, size)
                );
    }

    @Transactional
    public void registerAccess(UUID id) {

        Memory memory = memoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Memory not found: " + id
                ));

        memory.registerAccess();
    }

    @Transactional(readOnly = true)
    public Memory getById(UUID id) {
        return memoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Memory not found: " + id
                ));
    }

    @Transactional
    public void deactivate(UUID id) {
        Memory memory = memoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Memory not found: " + id
                ));

        memory.deactivate();
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 100"
            );
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters"
            );
        }
    }
}