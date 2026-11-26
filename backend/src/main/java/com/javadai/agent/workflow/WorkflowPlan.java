
package com.javadai.agent.workflow;

import java.util.List;

public record WorkflowPlan(List<WorkflowStep> steps) {

    public WorkflowPlan {
        steps = List.copyOf(steps);

        if (steps.isEmpty()) {
            throw new IllegalArgumentException(
                    "Workflow must contain at least one step."
            );
        }
    }
}
