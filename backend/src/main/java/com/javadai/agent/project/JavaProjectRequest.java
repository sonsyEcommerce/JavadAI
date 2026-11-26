
package com.javadai.agent.project;

import java.util.List;

public record JavaProjectRequest(
        String projectName,
        List<String> classNames
) {
    public JavaProjectRequest {
        classNames = List.copyOf(classNames);

        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }

        if (classNames.isEmpty()) {
            throw new IllegalArgumentException("At least one Java class is required.");
        }
    }
}
