
package com.javadai.agent.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentTask {

    private final String id;
    private final String instruction;

    private TaskStatus status;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String result;
    private String error;

    private final List<TaskStep> steps = new ArrayList<>();

    public AgentTask(String instruction) {
        this.id = UUID.randomUUID().toString();
        this.instruction = instruction;
        this.status = TaskStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void start() {
        this.status = TaskStatus.RUNNING;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String result) {
        this.status = TaskStatus.COMPLETED;
        this.result = result;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String error) {
        this.status = TaskStatus.FAILED;
        this.error = error;
        this.updatedAt = LocalDateTime.now();
    }

    public void addStep(TaskStep step) {
        this.steps.add(step);
        this.updatedAt = LocalDateTime.now();
    }

    public List<TaskStep> getSteps() {
        return List.copyOf(steps);
    }

    public String getId() {
        return id;
    }

    public String getInstruction() {
        return instruction;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getResult() {
        return result;
    }

    public String getError() {
        return error;
    }
}
