package com.supermercado.presentation.controller;

import com.supermercado.application.dto.ConfiguracionDTO;
import com.supermercado.application.dto.EstadisticasDTO;
import com.supermercado.application.usecase.IniciarSimulacionUseCase;
import com.supermercado.application.port.ILogService;
import com.supermercado.infrastructure.config.AppConfig;
import com.supermercado.presentation.view.SimuladorFrame;

public class SimulacionController {
    
    private final ILogService logService;
    private final IniciarSimulacionUseCase iniciarSimulacionUseCase;
    private final SimuladorFrame view;
    private Thread hiloSimulacion;
    private boolean simulacionActiva;
    private boolean pausado = false;
    private ConfiguracionDTO configuracionActual;

    public SimulacionController(SimuladorFrame view) {
        this.view = view;
        this.logService = AppConfig.getInstance().getLogService();
        this.iniciarSimulacionUseCase = AppConfig.getInstance().getIniciarSimulacionUseCase();
        this.simulacionActiva = false;
        logService.info("Controlador de simulacion inicializado");
        
        this.iniciarSimulacionUseCase.setOnEstadisticasUpdate(this::actualizarEstadisticas);
        // INYECCIÃ“N DIRECTA - Pasar el frame directamente
        this.iniciarSimulacionUseCase.setOnEstadisticasDirect(view::actualizarEstadisticasDirecto);
        System.out.println("CONTROLLER - InyecciÃ³n directa configurada");
        this.iniciarSimulacionUseCase.setOnCajaUpdate(this::actualizarCaja);
        this.iniciarSimulacionUseCase.setOnLogUpdate(this::agregarLog);
    }

    public void iniciarSimulacion(ConfiguracionDTO config) {
        if (simulacionActiva) {
            logService.warn("Intento de iniciar simulacion cuando ya esta activa");
            return;
        }
        
        if (config == null) {
            logService.error("Configuracion nula");
            view.mostrarError("Error", "La configuracion es nula");
            return;
        }
        
        this.configuracionActual = config;
        this.pausado = false;
        logService.info("Iniciando simulacion con configuracion");
        
        try {
            simulacionActiva = true;
            view.habilitarBotonIniciar(false);
            view.habilitarBotonDetener(true);
            view.limpiarLog();
            
            hiloSimulacion = new Thread(() -> {
                try {
                    iniciarSimulacionUseCase.ejecutar(config);
                    view.agregarLog("=== SIMULACION COMPLETADA ===");
                    view.mostrarMensaje("Simulacion completada exitosamente");
                    view.habilitarBotonIniciar(true);
                    view.habilitarBotonDetener(false);
                    simulacionActiva = false;
                    pausado = false;
                    view.cambiarEstadoPausa(false);
                } catch (InterruptedException e) {
                    view.agregarLog("=== SIMULACION INTERRUMPIDA ===");
                    view.habilitarBotonIniciar(true);
                    view.habilitarBotonDetener(false);
                    simulacionActiva = false;
                    pausado = false;
                    view.cambiarEstadoPausa(false);
                } catch (Exception e) {
                    logService.error("Error en la simulacion", e);
                    view.mostrarError("Error", "Error en la simulacion: " + e.getMessage());
                    view.habilitarBotonIniciar(true);
                    view.habilitarBotonDetener(false);
                    simulacionActiva = false;
                    pausado = false;
                    view.cambiarEstadoPausa(false);
                }
            });
            hiloSimulacion.start();
            
        } catch (Exception e) {
            logService.error("Error al iniciar la simulacion", e);
            view.mostrarError("Error", "No se pudo iniciar la simulacion");
            simulacionActiva = false;
            pausado = false;
            view.habilitarBotonIniciar(true);
            view.habilitarBotonDetener(false);
        }
    }
    
    private void actualizarEstadisticas(EstadisticasDTO estadisticas) {
        if (estadisticas != null) {
            view.actualizarEstadisticas(estadisticas);
        }
    }
    
    private void actualizarCaja(String[] data) {
        if (data.length >= 5) {
            try {
                int numCaja = Integer.parseInt(data[0]);
                String estado = data[1];
                int cola = Integer.parseInt(data[2]);
                String clienteInfo = data[3];
                String tipo = data[4];
                view.actualizarCajaConTipo(numCaja, estado, cola, clienteInfo, tipo);
            } catch (NumberFormatException e) {
                // Ignorar
            }
        }
    }
    
    private void agregarLog(String mensaje) {
        view.agregarLog(mensaje);
    }
    
    public void detenerSimulacion() {
        if (!simulacionActiva) {
            logService.warn("Intento de detener simulacion cuando no esta activa");
            return;
        }
        
        logService.info("Deteniendo simulacion");
        iniciarSimulacionUseCase.detener();
        view.agregarLog("=== SIMULACION DETENIDA ===");
        
        if (hiloSimulacion != null && hiloSimulacion.isAlive()) {
            hiloSimulacion.interrupt();
        }
        
        simulacionActiva = false;
        pausado = false;
        view.habilitarBotonIniciar(true);
        view.habilitarBotonDetener(false);
        view.cambiarEstadoPausa(false);
        view.mostrarMensaje("Simulacion detenida");
    }
    
    public void pausarSimulacion() {
        if (!simulacionActiva) {
            logService.warn("Intento de pausar simulacion cuando no esta activa");
            return;
        }
        
        if (!pausado) {
            // Pausar
            logService.info("Pausando simulacion");
            iniciarSimulacionUseCase.pausar();
            pausado = true;
            view.agregarLog("=== SIMULACION PAUSADA ===");
            view.cambiarEstadoPausa(true);
        } else {
            // Reanudar
            logService.info("Reanudando simulacion");
            iniciarSimulacionUseCase.reanudar();
            pausado = false;
            view.agregarLog("=== SIMULACION REANUDADA ===");
            view.cambiarEstadoPausa(false);
        }
    }
    
    public void reanudarSimulacion() {
        if (!simulacionActiva || !pausado) {
            logService.warn("Intento de reanudar simulacion cuando no esta pausada");
            return;
        }
        
        logService.info("Reanudando simulacion");
        iniciarSimulacionUseCase.reanudar();
        pausado = false;
        view.agregarLog("=== SIMULACION REANUDADA ===");
        view.cambiarEstadoPausa(false);
    }
    
    public boolean isSimulacionActiva() {
        return simulacionActiva;
    }
    
    public boolean isPausado() {
        return pausado;
    }
}
