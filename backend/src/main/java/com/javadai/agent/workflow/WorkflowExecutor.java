
package com.javadai.agent.workflow;

import com.javadai.agent.task.AgentTask;
import com.javadai.agent.task.TaskStep;
import com.javadai.agent.tools.FileTool;
import org.springframework.stereotype.Component;

@Component
public class WorkflowExecutor {

    private final FileTool fileTool;

    public WorkflowExecutor(FileTool fileTool) {
        this.fileTool = fileTool;
    }

    public String execute(AgentTask task, WorkflowPlan plan)
            throws Exception {

        StringBuilder results = new StringBuilder();

        for (WorkflowStep workflowStep : plan.steps()) {

            TaskStep step = new TaskStep(
                    workflowStep.description()
            );

            task.addStep(step);
            step.start();

            try {
                String result = switch (workflowStep.action()) {

                    case CREATE_DIRECTORY ->
                            fileTool.createDirectory(
                                    workflowStep.path()
                            );

                    case WRITE_FILE ->
                            fileTool.writeFile(
                                    workflowStep.path(),
                                    workflowStep.content()
                            );

                    case READ_FILE ->
                            fileTool.readFile(
                                    workflowStep.path()
                            );
                };

                step.complete(result);

                results.append(workflowStep.description())
                        .append(": ")
                        .append(result)
                        .append("\n");

            } catch (Exception exception) {

                step.fail(
                        exception.getMessage() == null
                                ? "Unknown error"
                                : exception.getMessage()
                );

                throw exception;
            }
        }

        return results.toString();
    }
}
