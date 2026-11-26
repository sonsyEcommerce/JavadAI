
package com.javadai.agent.tools;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ApplicationTool {

    private static final Map<String, String> APPLICATIONS = Map.of(
            "notepad", "notepad.exe",
            "calculator", "calc.exe",
            "chrome", "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "explorer", "explorer.exe"
    );

    public String open(String application)
            throws IOException {

        String executable = APPLICATIONS.get(
                application.toLowerCase().trim()
        );

        if (executable == null) {
            throw new SecurityException(
                    "Application is not allowed: " + application
            );
        }

        new ProcessBuilder(executable).start();

        return "Application started: " + application;
    }
}
