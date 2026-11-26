
package com.javadai.agent.tools;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ToolExecutor {

    private final FileTool fileTool;
    private final WindowsCommandTool windowsCommandTool;
    private final ApplicationTool applicationTool;

    public ToolExecutor(
            FileTool fileTool,
            WindowsCommandTool windowsCommandTool,
            ApplicationTool applicationTool) {

        this.fileTool = fileTool;
        this.windowsCommandTool = windowsCommandTool;
        this.applicationTool = applicationTool;
    }

    public String executeListFiles(String path) {
        try {
            List<String> files = fileTool.listFiles(path);
            return files.isEmpty()
                    ? "Directory is empty."
                    : String.join("\n", files);
        } catch (IOException | RuntimeException e) {
            return "File operation failed: " + e.getMessage();
        }
    }

    public String executeReadFile(String path) {
        try {
            return fileTool.readFile(path);
        } catch (IOException | RuntimeException e) {
            return "Read failed: " + e.getMessage();
        }
    }

    public String executeWriteFile(String path, String content) {
        try {
            return fileTool.writeFile(path, content);
        } catch (IOException | RuntimeException e) {
            return "Write failed: " + e.getMessage();
        }
    }

    public String executeSearchFiles(String keyword) {
        try {
            List<String> files = fileTool.searchFiles(keyword);
            return files.isEmpty()
                    ? "No matching files found."
                    : String.join("\n", files);
        } catch (IOException | RuntimeException e) {
            return "Search failed: " + e.getMessage();
        }
    }

    public String executeWindowsCommand(String command) {
        try {
            return windowsCommandTool.execute(command);
        } catch (Exception e) {
            return "Command failed: " + e.getMessage();
        }
    }

    public String executeOpenApplication(String application) {
        try {
            return applicationTool.open(application);
        } catch (Exception e) {
            return "Application launch failed: " + e.getMessage();
        }
    }
}
