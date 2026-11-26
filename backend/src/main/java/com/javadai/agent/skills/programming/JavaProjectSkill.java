
package com.javadai.agent.skills.programming;

import com.javadai.agent.project.JavaProjectGenerator;
import com.javadai.agent.skills.Skill;
import com.javadai.agent.task.AgentTask;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class JavaProjectSkill implements Skill {

    private final JavaProjectGenerator generator;

    private static final Pattern REQUEST_PATTERN =
            Pattern.compile(
                    "(?s).*پروژه\\s+java\\s+به\\s+نام\\s+[a-z][a-z0-9_]*.*",
                    Pattern.CASE_INSENSITIVE
            );

    public JavaProjectSkill(JavaProjectGenerator generator) {
        this.generator = generator;
    }

    @Override
    public String getName() {
        return "java-project-generator";
    }

    @Override
    public boolean supports(String instruction) {
        return instruction != null
                && REQUEST_PATTERN.matcher(instruction).matches();
    }

    @Override
    public String execute(
            AgentTask task,
            String instruction
    ) throws Exception {

        return generator.generate(task, instruction);
    }
}
