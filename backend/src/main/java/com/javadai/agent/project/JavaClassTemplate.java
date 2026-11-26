
package com.javadai.agent.project;

public final class JavaClassTemplate {

    private JavaClassTemplate() {
    }

    public static String generate(String packageName, String className) {

        StringBuilder code = new StringBuilder();

        code.append("package ")
                .append(packageName)
                .append(";\n\n");

        code.append("public class ")
                .append(className)
                .append(" {\n");

        if ("Main".equals(className)) {
            code.append("\n")
                    .append("    public static void main(String[] args) {\n")
                    .append("        System.out.println(\"Hello from JavadAI!\");\n")
                    .append("    }\n");
        }

        code.append("}\n");

        return code.toString();
    }
}
