package com.github.mrmks.mc.status.adapt;

public interface ILogger {
    void info(String msg);
    void warn(String msg);
    void severe(String msg);

    void debug(String msg);
}
