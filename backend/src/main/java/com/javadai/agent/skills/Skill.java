
package com.javadai.agent.skills;

import com.javadai.agent.task.AgentTask;

public interface Skill {

    String getName();

    boolean supports(String instruction);

    String execute(
            AgentTask task,
            String instruction
    ) throws Exception;
}
