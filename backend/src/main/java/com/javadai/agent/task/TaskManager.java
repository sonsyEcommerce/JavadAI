
package com.javadai.agent.task;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskManager {

    private final Map<String, AgentTask> tasks =
            new ConcurrentHashMap<>();

    public AgentTask createTask(String instruction) {

        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException(
                    "Task instruction cannot be empty"
            );
        }

        AgentTask task = new AgentTask(instruction);
        tasks.put(task.getId(), task);

        return task;
    }

    public Optional<AgentTask> findTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public void startTask(String taskId) {
        getTaskOrThrow(taskId).start();
    }

    public void completeTask(String taskId, String result) {
        getTaskOrThrow(taskId).complete(result);
    }

    public void failTask(String taskId, String error) {
        getTaskOrThrow(taskId).fail(error);
    }

    private AgentTask getTaskOrThrow(String taskId) {

        AgentTask task = tasks.get(taskId);

        if (task == null) {
            throw new IllegalArgumentException(
                    "Task not found: " + taskId
            );
        }

        return task;
    }
}
