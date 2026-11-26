
package com.javadai.agent.project;

import com.javadai.agent.task.AgentTask;
import com.javadai.agent.task.TaskStep;
import com.javadai.agent.tools.FileTool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavaProjectGenerator {

    private final FileTool fileTool;

    private static final Pattern PROJECT_PATTERN =
            Pattern.compile(
                    "پروژه\\s+Java\\s+به نام\\s+([A-Za-z][A-Za-z0-9_]*)"
            );

    private static final Pattern CLASS_PATTERN =
            Pattern.compile(
                    "کلاس\\s+([A-Z][A-Za-z0-9_]*)"
            );

    public JavaProjectGenerator(FileTool fileTool) {
        this.fileTool = fileTool;
    }

    public String generate(AgentTask task, String instruction)
            throws Exception {

        JavaProjectRequest request = parseRequest(instruction);

        String projectName = request.projectName();

        String packageName = projectName.toLowerCase(Locale.ROOT);

        String sourcePath = projectName
                + "/src/main/java/"
                + packageName;

        executeStep(
                task,
                "ساخت پوشه اصلی پروژه " + projectName,
                () -> fileTool.createDirectory(projectName)
        );

        executeStep(
                task,
                "ساخت ساختار سورس Java",
                () -> fileTool.createDirectory(sourcePath)
        );

        for (String className : request.classNames()) {

            String filePath = sourcePath + "/" + className + ".java";

            String content = JavaClassTemplate.generate(
                    packageName,
                    className
            );

            executeStep(
                    task,
                    "ساخت کلاس " + className,
                    () -> {
                        try {
                            return fileTool.writeNewFile(filePath, content);
                        } catch (java.nio.file.FileAlreadyExistsException exception) {

                            String existingContent = fileTool.readFile(filePath);

                            if (existingContent.equals(content)) {
                                return "File already exists with identical content: "
                                        + filePath;
                            }

                            throw new IllegalStateException(
                                    "File already exists with different content. "
                                            + "Overwrite requires approval: "
                                            + filePath
                            );
                        }
                    }
            );
        }

        return "Java project created successfully.\n"
                + "Project: " + projectName + "\n"
                + "Package: " + packageName + "\n"
                + "Classes: " + String.join(", ", request.classNames());
    }

    private JavaProjectRequest parseRequest(String instruction) {

        Matcher projectMatcher =
                PROJECT_PATTERN.matcher(instruction);

        if (!projectMatcher.find()) {
            throw new IllegalArgumentException(
                    "Project name not found. Example: پروژه Java به نام StudentManager بساز"
            );
        }

        String projectName = projectMatcher.group(1);

        Matcher classMatcher = CLASS_PATTERN.matcher(instruction);

        LinkedHashSet<String> classNames = new LinkedHashSet<>();

        while (classMatcher.find()) {
            classNames.add(classMatcher.group(1));
        }

        if (classNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "No Java classes found in instruction."
            );
        }

        return new JavaProjectRequest(
                projectName,
                new ArrayList<>(classNames)
        );
    }

    private void executeStep(
            AgentTask task,
            String description,
            StepAction action
    ) throws Exception {

        TaskStep step = new TaskStep(description);
        task.addStep(step);
        step.start();

        try {
            step.complete(action.execute());
        } catch (Exception exception) {
            step.fail(
                    exception.getMessage() == null
                            ? "Unknown error"
                            : exception.getMessage()
            );
            throw exception;
        }
    }

    @FunctionalInterface
    private interface StepAction {
        String execute() throws Exception;
    }
}
