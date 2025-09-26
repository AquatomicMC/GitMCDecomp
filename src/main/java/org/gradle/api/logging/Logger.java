package org.gradle.api.logging;

public interface Logger {
    void debug(String msg);

    void info(String msg);

    void warn(String s);

    void error(String msg);
}
