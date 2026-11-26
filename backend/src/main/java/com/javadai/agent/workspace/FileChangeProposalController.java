
package com.javadai.agent.workspace;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspace/proposals")
public class FileChangeProposalController {

    private final FileChangeProposalService service;

    public FileChangeProposalController(FileChangeProposalService service) {
        this.service = service;
    }

    @GetMapping
    public List<FileChangeProposalService.Proposal> getPending() {
        return service.getPending();
    }

    @GetMapping("/{id}")
    public FileChangeProposalService.Proposal get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/approve")
    public FileChangeProposalService.Proposal approve(
            @PathVariable UUID id
    ) throws IOException {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public FileChangeProposalService.Proposal reject(
            @PathVariable UUID id
    ) {
        return service.reject(id);
    }
}
