package com.supermercado.infrastructure.config;

import com.supermercado.application.port.IConfiguracionRepositorio;
import com.supermercado.application.port.ILogService;
import com.supermercado.application.port.IReporteExportador;
import com.supermercado.application.usecase.IniciarSimulacionUseCase;
import com.supermercado.infrastructure.service.LogServiceImpl;
import com.supermercado.infrastructure.repository.ConfiguracionRepositorioImpl;
import com.supermercado.infrastructure.service.ReporteExcelImpl;

public class AppConfig {
    
    private static AppConfig instance;
    
    private final ILogService logService;
    private final IConfiguracionRepositorio configuracionRepositorio;
    private final IReporteExportador reporteExportador;
    private final IniciarSimulacionUseCase iniciarSimulacionUseCase;
    
    private AppConfig() {
        this.logService = new LogServiceImpl();
        this.configuracionRepositorio = new ConfiguracionRepositorioImpl(logService);
        this.reporteExportador = new ReporteExcelImpl(logService);
        this.iniciarSimulacionUseCase = new IniciarSimulacionUseCase(logService);
    }
    
    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    public ILogService getLogService() { return logService; }
    public IConfiguracionRepositorio getConfiguracionRepositorio() { return configuracionRepositorio; }
    public IReporteExportador getReporteExportador() { return reporteExportador; }
    public IniciarSimulacionUseCase getIniciarSimulacionUseCase() { return iniciarSimulacionUseCase; }
}
