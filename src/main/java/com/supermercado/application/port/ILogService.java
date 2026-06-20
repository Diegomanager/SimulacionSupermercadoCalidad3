package com.supermercado.application.port;

public interface ILogService {
    void info(String mensaje);
    void debug(String mensaje);
    void warn(String mensaje);
    void error(String mensaje);
    void error(String mensaje, Throwable throwable);
}
