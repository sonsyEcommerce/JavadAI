
package com.javadai.agent.workflow;

public record WorkflowStep(
        WorkflowAction action,
        String path,
        String content,
        String description
) {
}
