package org.gradle.api.logging;

public class TemplateLogger implements Logger {
    public void debug(String s) {
        System.out.println(s);
    }

    public void info(String s) {
        System.out.println(s);
    }

    public void warn(String s) {
        System.out.println(s);
    }

    public void error(String s) {
        System.err.println(s);
    }
}
