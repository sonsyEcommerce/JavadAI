
package com.javadai.agent.intent;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class IntentDetector {

    public IntentType detect(String instruction) {

        if (instruction == null || instruction.isBlank()) {
            return IntentType.UNKNOWN;
        }

        String text = normalize(instruction);

        // Create Java project
        if (text.matches("(?s).*پروژه\\s+java\\s+به\\s+نام\\s+[a-z][a-z0-9_]*.*")) {
            return IntentType.CREATE_PROJECT;
        }

        // File operations
        if (containsAny(text,
                "با محتوا",
                "فایل ایجاد کن",
                "فایل بساز",
                "در فایل بنویس",
                "write file")) {
            return IntentType.WRITE_FILE;
        }

        if (containsAny(text,
                "را بخوان",
                "محتوای فایل",
                "read file")) {
            return IntentType.READ_FILE;
        }

        // List workspace and files
        if (containsAny(text,
                "فایل ها را نمایش",
                "فایل ها نمایش",
                "نمایش فایل",
                "فایل های workspace",
                "فایلهای workspace",
                "لیست فایل",
                "فهرست فایل",
                "محتویات workspace",
                "محتویات پوشه workspace",
                "لیست workspace",
                "فهرست workspace",
                "list files",
                "show files",
                "list workspace",
                "show workspace")) {

            return IntentType.LIST_FILES;
        }

        // Search files
        if (containsAny(text,
                "جستجوی فایل",
                "جستجو کن",
                "فایل هایی که شامل",
                "فایل هایی شامل",
                "فایلی که شامل",
                "فایل با نام",
                "فایل هایی با نام",
                "فایل های حاوی",
                "فایل حاوی",
                "پیدا کن",
                "پیدا کن که شامل",
                "search files",
                "find file",
                "find files")) {

            return IntentType.SEARCH_FILES;
        }

        // Code analysis
        if (containsAny(text,
                "بررسی کد",
                "تحلیل کد",
                "analyze code")) {
            return IntentType.ANALYZE_CODE;
        }

        // Capabilities
        if (containsAny(text,
                "چه قابلیت هایی داری",
                "قابلیت های تو",
                "چه کارهایی می توانی انجام دهی",
                "چه کارهایی بلدی",
                "توانایی های تو",
                "امکانات تو",
                "قابلیت هات چیه",
                "what can you do",
                "your capabilities")) {
            return IntentType.CAPABILITIES;
        }

        // Date and time
        if (containsAny(text,
                "ساعت چنده",
                "ساعت الان",
                "زمان فعلی",
                "تاریخ امروز",
                "امروز چندمه",
                "تاریخ و ساعت",
                "what time is it",
                "current date")) {
            return IntentType.DATE_TIME;
        }

        // Help
        if (containsAny(text,
                "راهنمایی کن",
                "راهنمای استفاده",
                "چطور ازت استفاده کنم",
                "چگونه از تو استفاده کنم",
                "کمک می خواهم",
                "help",
                "راهنما")) {
            return IntentType.HELP;
        }

        // Windows commands
        if (containsAny(text,
                "نام کامپیوتر",
                "نام سیستم",
                "نام کاربر",
                "اطلاعات شبکه",
                "فرایندهای در حال اجرا",
                "پردازش های در حال اجرا",
                "ipconfig",
                "systeminfo",
                "tasklist",
                "hostname",
                "whoami")) {
            return IntentType.WINDOWS_COMMAND;
        }

        // Open applications
        if (containsAny(text,
                "کروم را باز کن",
                "گوگل کروم را باز کن",
                "نوت پد را باز کن",
                "notepad را باز کن",
                "ماشین حساب را باز کن",
                "calculator را باز کن",
                "فایل اکسپلورر را باز کن",
                "explorer را باز کن")) {
            return IntentType.OPEN_APPLICATION;
        }

        // Greeting only
        if (text.matches("^(سلام|سلام javad ai|hello|hi)[!.؟\\s]*$")) {
            return IntentType.GREETING;
        }

        return IntentType.UNKNOWN;
    }


    private String normalize(String input) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            // Persian digits to English digits
            if (ch >= '۰' && ch <= '۹') {
                ch = (char) ('0' + ch - '۰');
            }
            // Arabic digits to English digits
            else if (ch >= '٠' && ch <= '٩') {
                ch = (char) ('0' + ch - '٠');
            }

            result.append(ch);
        }

        return result.toString()
                .toLowerCase(Locale.ROOT)
                .replace('ي', 'ی')
                .replace('ك', 'ک')
                .replace('\u200c', ' ')
                .replace('\u200f', ' ')
                .replace('\u200e', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsAny(String text, String... keywords) {

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
