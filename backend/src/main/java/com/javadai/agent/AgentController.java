package com.javadai.agent;

import com.javadai.agent.task.AgentTask;
import com.javadai.agent.task.TaskManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final Agent agent;
    private final TaskManager taskManager;

    public AgentController(Agent agent, TaskManager taskManager) {
        this.agent = agent;
        this.taskManager = taskManager;
    }

    @PostMapping(
            value = "/ask",
            consumes = "text/plain; charset=UTF-8"
    )
    public AgentTask ask(@RequestBody String instruction) {
        return agent.think(instruction);
    }

    @PostMapping("/ask-with-context")
    public AgentTask askWithContext(@RequestBody AgentAskRequest request) {
        return agent.think(
                request.instruction(),
                request.activeFilePath(),
                request.activeFileContent()
        );
    }

    @GetMapping("/tasks/{id}")
    public AgentTask getTask(@PathVariable String id) {
        return taskManager.findTask(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Task not found: " + id)
                );
    }
}
