
package com.javadai.agent.memory;

import com.javadai.agent.memory.dto.CreateMemoryRequest;
import com.javadai.agent.memory.dto.MemoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemoryResponse create(
            @RequestBody CreateMemoryRequest request
    ) {
        Memory memory = memoryService.remember(
                request.memoryType(),
                request.title(),
                request.content(),
                request.source(),
                request.importance()
        );

        return MemoryResponse.from(memory);
    }

    @GetMapping
    public List<MemoryResponse> getRecent(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return memoryService.getRecentMemories(limit)
                .stream()
                .map(MemoryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MemoryResponse getById(@PathVariable UUID id) {
        return MemoryResponse.from(memoryService.getById(id));
    }

    @GetMapping("/search")
    public Page<MemoryResponse> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return memoryService.search(keyword, page, size)
                .map(MemoryResponse::from);
    }

    @GetMapping("/type/{type}")
    public Page<MemoryResponse> getByType(
            @PathVariable MemoryType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return memoryService.getByType(type, page, size)
                .map(MemoryResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        memoryService.deactivate(id);
    }
}
