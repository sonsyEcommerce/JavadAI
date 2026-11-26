package com.javadai.agent;

import com.javadai.agent.intent.IntentDetector;
import com.javadai.agent.intent.IntentType;
import com.javadai.agent.llm.LlmAgent;
import com.javadai.agent.project.JavaProjectGenerator;
import com.javadai.agent.skills.SkillRegistry;
import com.javadai.agent.task.AgentTask;
import com.javadai.agent.task.TaskManager;
import com.javadai.agent.tools.ToolExecutor;
import com.javadai.agent.workflow.WorkflowExecutor;
import com.javadai.agent.workflow.WorkflowPlanner;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Agent {

    private final IntentDetector intentDetector;
    private final ToolExecutor toolExecutor;
    private final TaskManager taskManager;
    private final WorkflowExecutor workflowExecutor;
    private final WorkflowPlanner workflowPlanner;
    private final SkillRegistry skillRegistry;
    private final LlmAgent llmAgent;

    public Agent(
            IntentDetector intentDetector,
            ToolExecutor toolExecutor,
            TaskManager taskManager,
            WorkflowPlanner workflowPlanner,
            WorkflowExecutor workflowExecutor,
            SkillRegistry skillRegistry,
            LlmAgent llmAgent) {

        this.intentDetector = intentDetector;
        this.toolExecutor = toolExecutor;
        this.taskManager = taskManager;
        this.workflowPlanner = workflowPlanner;
        this.workflowExecutor = workflowExecutor;
        this.skillRegistry = skillRegistry;
        this.llmAgent = llmAgent;
    }

    public AgentTask think(String instruction) {
        return think(instruction, null, null);
    }

    public AgentTask think(
            String instruction,
            String activeFilePath,
            String activeFileContent
    ) {

        AgentTask task = taskManager.createTask(instruction);

        try {
            taskManager.startTask(task.getId());

            IntentType intent = intentDetector.detect(instruction);

            // Workspace and project-understanding requests must bypass deterministic skills/workflows.
            // Otherwise they can be answered without consulting the live workspace or coding tools.
            if (isWorkspaceRequest(instruction) || isProjectUnderstandingRequest(instruction)) {
                String result = llmAgent.run(task, instruction, activeFilePath, activeFileContent);
                if (result.startsWith("ERROR:")) {
                    taskManager.failTask(task.getId(), result);
                } else {
                    taskManager.completeTask(task.getId(), result);
                }
                return task;
            }

            var skill = skillRegistry.findSkill(instruction);

            if (skill.isPresent()) {

                String result = skill.get().execute(
                        task,
                        instruction
                );

                taskManager.completeTask(task.getId(), result);

                return task;
            }

            if (workflowPlanner.supports(instruction)) {

                var plan = workflowPlanner.plan(instruction);

                String workflowResult =
                        workflowExecutor.execute(task, plan);

                taskManager.completeTask(task.getId(), workflowResult);
                return task;
            }

            String result = switch (intent) {

                case GREETING ->
                        "سلام! من JavadAI هستم.";

                case WINDOWS_COMMAND ->
                        executeWindowsCommand(instruction);

                case LIST_FILES ->
                        toolExecutor.executeListFiles("");

                case READ_FILE ->
                        toolExecutor.executeReadFile(
                                extractPath(instruction));

                case WRITE_FILE ->
                        executeWrite(instruction);

                case SEARCH_FILES ->
                        toolExecutor.executeSearchFiles(
                                extractSearchKeyword(instruction));

                case CREATE_PROJECT ->
                        llmAgent.run(task, instruction, activeFilePath, activeFileContent);

                case OPEN_APPLICATION ->
                        executeOpenApplication(instruction);


                case CAPABILITIES ->
                        "من JavadAI هستم. در حال حاضر می‌توانم در محدوده قابلیت‌های پیاده‌سازی‌شده به شما کمک کنم:\n"
                                + "- نمایش و جستجوی فایل‌ها\n"
                                + "- خواندن و نوشتن فایل‌ها\n"
                                + "- اجرای تعدادی از دستورات مجاز ویندوز\n"
                                + "- باز کردن برنامه‌هایی مانند Notepad و Calculator\n"
                                + "- اجرای Workflowها و Skillهای ثبت‌شده\n"
                                + "- مدیریت وظایف و حافظه";

                case DATE_TIME ->
                        "تاریخ و ساعت فعلی سیستم: "
                                + java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                case HELP ->
                        "برای شروع می‌توانی از من بخواهی:\n"
                                + "- فایل‌های Workspace را نمایش دهم\n"
                                + "- فایلی را بخوانم یا ایجاد کنم\n"
                                + "- نام کامپیوتر یا کاربر را نمایش دهم\n"
                                + "- یک برنامه مجاز را باز کنم\n"
                                + "- درباره قابلیت‌هایم توضیح بدهم";


                case ANALYZE_CODE, UNKNOWN ->
                        llmAgent.run(
                                task,
                                instruction,
                                activeFilePath,
                                activeFileContent
                        );

            };

            if (result.startsWith("ERROR:")) {
                taskManager.failTask(task.getId(), result);
                return task;
            }

            taskManager.completeTask(task.getId(), result);

        } catch (Exception exception) {
            taskManager.failTask(
                    task.getId(),
                    exception.getMessage() == null
                            ? "Unknown error"
                            : exception.getMessage()
            );
        }

        return task;
    }


    private boolean isWorkspaceRequest(String instruction) {
        String text = instruction == null ? "" : instruction.toLowerCase(java.util.Locale.ROOT);
        return text.contains("workspace") || text.contains("فضای کاری");
    }

    private boolean isProjectUnderstandingRequest(String instruction) {
        String text = instruction == null ? "" : instruction.toLowerCase(java.util.Locale.ROOT);
        boolean mentionsProject = text.contains("backend") || text.contains("frontend")
                || text.contains("angular") || text.contains("spring boot")
                || text.contains("معماری") || text.contains("ساختار پروژه");
        boolean asksToUnderstand = text.contains("تحلیل") || text.contains("بررسی")
                || text.contains("بخوان") || text.contains("بخون") || text.contains("توضیح")
                || text.contains("ارتباط") || text.contains("معماری") || text.contains("analyze")
                || text.contains("inspect") || text.contains("explain") || text.contains("read");
        return mentionsProject && asksToUnderstand;
    }

    private String extractPath(String instruction) {
        Matcher matcher = Pattern.compile(
                "(?i)(?:فایل|file)\\s+`?([^\\s`]+)`?"
        ).matcher(instruction);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException(
                "مسیر فایل برای خواندن پیدا نشد."
        );
    }

    private String extractSearchKeyword(String instruction) {

        // حالت اول: «فایل Student.java را پیدا کن»
        Matcher fileMatcher = Pattern.compile(
                "(?:فایل|file)\\s+([\\w.\\-]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(instruction);

        if (fileMatcher.find()) {
            return fileMatcher.group(1);
        }

        // حالت دوم: الگوهای قبلی
        Matcher matcher = Pattern.compile(
                "(?:با نام|شامل|به نام)\\s+([\\w.\\-]+)"
        ).matcher(instruction);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException(
                "نام فایل برای جستجو مشخص نیست."
        );
    }

    private String executeWrite(String instruction) {
        Matcher matcher = Pattern.compile(
                "فایل\\s+([\\w.\\-]+)\\s+با محتوا\\s+(.+)",
                Pattern.DOTALL
        ).matcher(instruction);

        if (matcher.find()) {
            return toolExecutor.executeWriteFile(
                    matcher.group(1),
                    matcher.group(2)
            );
        }

        return "فرمت درخواست ایجاد فایل صحیح نیست.";
    }

    private String executeWindowsCommand(String instruction) {

        String text = instruction.toLowerCase();

        if (text.contains("نام کامپیوتر")
                || text.contains("نام سیستم")
                || text.contains("hostname")) {

            return toolExecutor.executeWindowsCommand("hostname");
        }

        if (text.contains("نام کاربر")
                || text.contains("whoami")) {

            return toolExecutor.executeWindowsCommand("whoami");
        }

        if (text.contains("اطلاعات شبکه")
                || text.contains("ipconfig")) {

            return toolExecutor.executeWindowsCommand("ipconfig");
        }

        if (text.contains("فرایندهای در حال اجرا")
                || text.contains("پردازش های در حال اجرا")
                || text.contains("tasklist")) {

            return toolExecutor.executeWindowsCommand("tasklist");
        }

        if (text.contains("systeminfo")) {
            return toolExecutor.executeWindowsCommand("systeminfo");
        }

        return "Windows command not recognized.";
    }

    private String executeOpenApplication(String instruction) {

        String text = instruction.toLowerCase();

        if (text.contains("کروم") || text.contains("chrome")) {
            return toolExecutor.executeOpenApplication("chrome");
        }

        if (text.contains("نوت پد") || text.contains("notepad")) {
            return toolExecutor.executeOpenApplication("notepad");
        }

        if (text.contains("ماشین حساب") || text.contains("calculator")) {
            return toolExecutor.executeOpenApplication("calculator");
        }

        if (text.contains("اکسپلورر") || text.contains("explorer")) {
            return toolExecutor.executeOpenApplication("explorer");
        }

        return "Application not recognized.";
    }
}
