
package com.javadai.agent.workflow;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkflowPlanner {

    private static final Pattern DIRECTORY_PATTERN =
            Pattern.compile(
                    "پوشه(?:‌های|های)\\s+(.+?)\\s+را\\s+(?:ایجاد کن|بساز)"
            );

    private static final Pattern NAMED_DIRECTORY_PATTERN =
            Pattern.compile(
                    "پوشه\\s+به نام\\s+([\\w.-]+)\\s+بساز"
            );

    private static final Pattern NESTED_DIRECTORY_PATTERN =
            Pattern.compile(
                    "پوشه\\s+([\\w.-]+)\\s+را\\s+داخل\\s+([\\w./-]+)\\s+(?:ایجاد کن|بساز)"
            );

    private static final Pattern FILE_PATTERN =
            Pattern.compile(
                    "داخل\\s+([\\w./-]+)\\s+فایل\\s+([\\w.-]+)\\s+بساز"
            );

    private static final Pattern CONTENT_PATTERN =
            Pattern.compile(
                    "(?s)متن\\s+(.+?)\\s+را\\s+داخل\\s+فایل\\s+([\\w.-]+)\\s+بنویس"
            );

    private static final Pattern READ_PATTERN =
            Pattern.compile(
                    "(?:فایل|file)\\s+([\\w./\\\\-]+)\\s+را بخوان"
            );

    public boolean supports(String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return false;
        }

        return DIRECTORY_PATTERN.matcher(instruction).find()
                || NAMED_DIRECTORY_PATTERN.matcher(instruction).find()
                || NESTED_DIRECTORY_PATTERN.matcher(instruction).find()
                || FILE_PATTERN.matcher(instruction).find()
                || CONTENT_PATTERN.matcher(instruction).find()
                || READ_PATTERN.matcher(instruction).find();
    }

    public WorkflowPlan plan(String instruction) {

        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException(
                    "Instruction is required."
            );
        }

        List<WorkflowStep> steps = new ArrayList<>();
        LinkedHashSet<String> directories = new LinkedHashSet<>();

        // 1. استخراج چند پوشه
        Matcher directoryMatcher =
                DIRECTORY_PATTERN.matcher(instruction);

        while (directoryMatcher.find()) {
            String[] names = directoryMatcher.group(1).split("\\s+و\\s+");

            for (String name : names) {
                String directory = name.trim();

                if (!directory.isBlank()) {
                    directories.add(directory);
                }
            }
        }

        // پشتیبانی از الگوی قدیمی ساخت پوشه
        Matcher namedMatcher =
                NAMED_DIRECTORY_PATTERN.matcher(instruction);

        while (namedMatcher.find()) {
            directories.add(namedMatcher.group(1));
        }

        // 2. استخراج پوشه‌های تو در تو
        Matcher nestedMatcher =
                NESTED_DIRECTORY_PATTERN.matcher(instruction);

        while (nestedMatcher.find()) {
            String child = nestedMatcher.group(1);
            String parent = nestedMatcher.group(2);

            directories.add(parent + "/" + child);
        }

        // 3. ثبت مراحل ساخت پوشه
        for (String directory : directories) {
            steps.add(new WorkflowStep(
                    WorkflowAction.CREATE_DIRECTORY,
                    directory,
                    null,
                    "ساخت پوشه " + directory
            ));
        }

        // 4. استخراج فایل‌ها و مسیرشان
        Map<String, String> files = new LinkedHashMap<>();

        Matcher fileMatcher = FILE_PATTERN.matcher(instruction);

        while (fileMatcher.find()) {
            String directory = fileMatcher.group(1);
            String fileName = fileMatcher.group(2);

            String path = directory + "/" + fileName;

            files.put(fileName, path);
        }

        // 5. استخراج محتوای هر فایل
        Map<String, String> contents = new LinkedHashMap<>();

        Matcher contentMatcher =
                CONTENT_PATTERN.matcher(instruction);

        while (contentMatcher.find()) {
            String content = contentMatcher.group(1).trim();
            String fileName = contentMatcher.group(2);

            contents.put(fileName, content);
        }

        // 6. ساخت مرحله نوشتن فایل
        for (Map.Entry<String, String> file : files.entrySet()) {

            String fileName = file.getKey();
            String path = file.getValue();

            String content = contents.get(fileName);

            if (content == null) {
                throw new IllegalArgumentException(
                        "Content not specified for file: " + fileName
                );
            }

            steps.add(new WorkflowStep(
                    WorkflowAction.WRITE_FILE,
                    path,
                    content,
                    "ایجاد و نوشتن فایل " + path
            ));
        }

        // 7. پشتیبانی از خواندن فایل
        if (steps.isEmpty()) {
            Matcher readMatcher = READ_PATTERN.matcher(instruction);

            if (readMatcher.find()) {
                String path = readMatcher.group(1);

                steps.add(new WorkflowStep(
                        WorkflowAction.READ_FILE,
                        path,
                        null,
                        "خواندن فایل " + path
                ));
            }
        }

        return new WorkflowPlan(steps);
    }
}
