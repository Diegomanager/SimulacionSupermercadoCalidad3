package com.supermercado.infrastructure.service;

import com.supermercado.application.port.ILogService;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class LogServiceImpl implements ILogService {

    private final PrintStream out;
    private final PrintStream err;

    public LogServiceImpl() {
        this.out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        this.err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
    }

    @Override
    public void info(String mensaje) {
        out.println(mensaje);
        out.flush();
    }

    @Override
    public void debug(String mensaje) {
        out.println("[DEBUG] " + mensaje);
        out.flush();
    }

    @Override
    public void warn(String mensaje) {
        out.println("ADVERTENCIA: " + mensaje);
        out.flush();
    }

    @Override
    public void error(String mensaje) {
        err.println("ERROR: " + mensaje);
        err.flush();
    }

    @Override
    public void error(String mensaje, Throwable throwable) {
        err.println("ERROR: " + mensaje);
        throwable.printStackTrace(err);
        err.flush();
    }
}