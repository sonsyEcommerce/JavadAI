
package com.javadai.agent.task;

import java.time.Instant;
import java.util.UUID;

public class TaskStep {

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    private final String id;
    private final String description;

    private Status status;
    private String result;
    private String error;

    private Instant startedAt;
    private Instant completedAt;

    public TaskStep(String description) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.status = Status.PENDING;
    }

    public void start() {
        this.status = Status.RUNNING;
        this.startedAt = Instant.now();
    }

    public void complete(String result) {
        this.status = Status.COMPLETED;
        this.result = result;
        this.completedAt = Instant.now();
    }

    public void fail(String error) {
        this.status = Status.FAILED;
        this.error = error;
        this.completedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
