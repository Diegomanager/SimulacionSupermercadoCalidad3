package com.supermercado.application.usecase;

import com.supermercado.application.dto.ConfiguracionDTO;
import com.supermercado.application.dto.EstadisticasDTO;
import com.supermercado.application.port.ILogService;
import com.supermercado.domain.model.Caja;
import com.supermercado.domain.model.Cliente;
import com.supermercado.domain.model.EstadoCaja;
import com.supermercado.domain.service.EstadisticasService;
import com.supermercado.domain.service.SimuladorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * UseCase que ejecuta la simulaci?n con escala de tiempo.
 * Mantiene la l?gica original de SimulacionThread pero con mejoras de logging y UI.
 */
public class SimulacionConEscalaUseCase {
    
    private final ILogService logService;
    private final SimuladorService simuladorService;
    private final EstadisticasService estadisticasService;
    
    private ConfiguracionDTO config;
    private List<Caja> cajas;
    private List<Thread> hilosCajas;
    private Random random;
    
    private volatile boolean ejecutando;
    private boolean pausado;
    private int clientesGenerados;
    private long tiempoInicio;
    private long msPerMinutoSimulado;
    private int minSimulados;
    private long duracionRealMs;
    private long tickMs;
    private int tickActual = 0;
    
    private Consumer<EstadisticasDTO> onEstadisticasUpdate;
    private Consumer<String[]> onCajaUpdate;
    private Consumer<String> onLogUpdate;

    public SimulacionConEscalaUseCase(ILogService logService) {
        this.logService = logService;
        this.simuladorService = new SimuladorService(null);
        this.estadisticasService = new EstadisticasService();
        this.cajas = new ArrayList<>();
        this.hilosCajas = new ArrayList<>();
        this.random = new Random();
        this.ejecutando = false;
        this.pausado = false;
        this.clientesGenerados = 0;
    }
    
    public void setOnEstadisticasUpdate(Consumer<EstadisticasDTO> callback) {
        this.onEstadisticasUpdate = callback;
    }
    
    public void setOnCajaUpdate(Consumer<String[]> callback) {
        this.onCajaUpdate = callback;
    }
    
    public void setOnLogUpdate(Consumer<String> callback) {
        this.onLogUpdate = callback;
    }

    public void ejecutar(ConfiguracionDTO config) throws InterruptedException {
        if (ejecutando) {
            throw new IllegalStateException("La simulacion ya esta en ejecucion");
        }
        
        this.config = config;
        this.ejecutando = true;
        this.pausado = false;
        this.clientesGenerados = 0;
        this.tickActual = 0;
        this.cajas.clear();
        this.hilosCajas.clear();
        
        log("=== INICIANDO SIMULACION ===");
        log("Duracion real: " + config.getDuracionRealSegundos() + "s  |  Tiempo simulado: " + config.getHorasSimuladas() + "h");
        log("Cajas Normales: " + config.getNumCajasNormales() + "  |  Cajas Rapidas: " + config.getNumCajasRapidas());
        
        // ============================================================
        // C?LCULO DE ESCALA DE TIEMPO (manteniendo la l?gica original)
        // ============================================================
        this.duracionRealMs = config.getDuracionRealSegundos() * 1000L;
        this.minSimulados = config.getHorasSimuladas() * 60;
        this.msPerMinutoSimulado = Math.max(1, duracionRealMs / minSimulados);
        this.tickMs = msPerMinutoSimulado;
        
        log("Escala: 1 minuto simulado = " + msPerMinutoSimulado + "ms reales");
        
        // ============================================================
        // CREAR CAJAS
        // ============================================================
        int numRapidas = config.getNumCajasRapidas();
        int numNormales = config.getNumCajasNormales();
        for (int i = 1; i <= numRapidas; i++) {
            cajas.add(new Caja(i, true));
        }
        for (int i = 1; i <= numNormales; i++) {
            cajas.add(new Caja(numRapidas + i, false));
        }
        
        log("Total cajas creadas: " + cajas.size() + " (" + numRapidas + " rapidas, " + numNormales + " normales)");
        
        // ============================================================
        // INICIAR HILOS DE CAJAS
        // ============================================================
        for (Caja caja : cajas) {
            Thread hilo = new Thread(() -> ejecutarCaja(caja));
            hilo.start();
            hilosCajas.add(hilo);
        }
        
        // ============================================================
        // BUCLE PRINCIPAL (1 iteraci?n = 1 tick = 1 minuto simulado)
        // ============================================================
        tiempoInicio = System.currentTimeMillis();
        int limiteClientes = config.getLimiteClientes();
        
        while (ejecutando) {
            long transcurrido = System.currentTimeMillis() - tiempoInicio;
            if (transcurrido >= duracionRealMs) {
                log("Tiempo de simulacion completado.");
                break;
            }
            
            // Pausa
            while (pausado && ejecutando) {
                Thread.sleep(50);
                transcurrido = System.currentTimeMillis() - tiempoInicio;
            }
            
            // L?mite de clientes
            if (limiteClientes > 0 && clientesGenerados >= limiteClientes) {
                log("Limite de " + limiteClientes + " clientes alcanzado.");
                break;
            }
            
            // Generar cliente seg?n probabilidad
            if (random.nextInt(100) < config.getProbabilidadLlegadaCliente()) {
                generarCliente();
            }
            
            // Actualizar UI
            tickActual++;
            actualizarUI();
            
            // Esperar hasta el pr?ximo tick
            Thread.sleep(tickMs);
        }
        
        // ============================================================
        // ESPERAR QUE TERMINEN LAS ATENCIONES
        // ============================================================
        log("Esperando que terminen las atenciones...");
        boolean todasLibres = false;
        int esperas = 0;
        while (!todasLibres && ejecutando) {
            todasLibres = true;
            for (Caja c : cajas) {
                if (c.getClienteActual() != null || c.getClientesEnCola() > 0) {
                    todasLibres = false;
                    break;
                }
            }
            if (!todasLibres) {
                Thread.sleep(tickMs);
                esperas++;
                if (esperas % 10 == 0) {
                    log("Esperando finalizacion... (" + esperas + ")");
                }
            }
        }
        
        // ============================================================
        // FINALIZAR
        // ============================================================
        for (Caja caja : cajas) {
            caja.detener();
        }
        ejecutando = false;
        
        log("=== SIMULACION COMPLETADA ===");
        mostrarResultados();
    }
    
    private void log(String mensaje) {
        if (onLogUpdate != null) {
            onLogUpdate.accept(mensaje);
        }
        logService.info(mensaje);
    }
    
    private void generarCliente() {
        int min = config.getArticulosClienteMin();
        int max = config.getArticulosClienteMax();
        int articulos = min + random.nextInt(max - min + 1);
        
        Cliente cliente = new Cliente(++clientesGenerados, articulos);
        cliente.setTiempoLlegada(tickActual); // tick actual como tiempo simulado
        
        // Asignar a caja con menos cola
        Caja mejorCaja = null;
        int minCola = Integer.MAX_VALUE;
        for (Caja caja : cajas) {
            if (caja.getEstado() == EstadoCaja.DETENIDA) continue;
            if (!caja.esRapida() || cliente.esRapido()) {
                int colaSize = caja.getClientesEnCola();
                if (colaSize < minCola) {
                    minCola = colaSize;
                    mejorCaja = caja;
                }
            }
        }
        if (mejorCaja != null) {
            mejorCaja.agregarCliente(cliente);
        }
    }
    
    private void ejecutarCaja(Caja caja) {
        while (caja.estaActiva()) {
            try {
                while (pausado && ejecutando) {
                    Thread.sleep(50);
                }
                
                Cliente cliente = caja.prepararSiguienteCliente();
                if (cliente == null) {
                    Thread.sleep(tickMs / 2);
                    continue;
                }
                
                // Calcular tiempo de atenci?n
                int tMin, tMax;
                if (caja.esRapida()) {
                    tMin = config.getTiempoCajaRapidaMin();
                    tMax = config.getTiempoCajaRapidaMax();
                } else {
                    tMin = config.getTiempoCajaNormalMin();
                    tMax = config.getTiempoCajaNormalMax();
                }
                int tiempoAtencionMinutos = tMin + random.nextInt(Math.max(1, tMax - tMin + 1));
                cliente.setTiempoAtencionReal(tiempoAtencionMinutos);
                cliente.setTiempoSalida(tickActual + tiempoAtencionMinutos);
                
                // Dormir seg?n escala
                long sleepMs = Math.max(50, tiempoAtencionMinutos * msPerMinutoSimulado);
                Thread.sleep(sleepMs);
                
                caja.finalizarAtencion();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void actualizarUI() {
        // Estad?sticas
        if (onEstadisticasUpdate != null) {
            EstadisticasDTO stats = estadisticasService.calcularEstadisticas(cajas);
            onEstadisticasUpdate.accept(stats);
        }
        
        // Cajas
        if (onCajaUpdate != null) {
            for (Caja caja : cajas) {
                String id = caja.getId();
                String numStr = id.replaceAll("\\D+", "");
                if (numStr.isEmpty()) continue;
                int num = Integer.parseInt(numStr);
                String estado = caja.estaOcupada() ? "ATENDIENDO" : "LIBRE";
                if (caja.getEstado().toString().equals("PAUSADA")) {
                    estado = "PAUSADA";
                }
                String clienteInfo = caja.getClienteActual() != null ? 
                    caja.getClienteActual().getId() : "";
                String tipo = caja.esRapida() ? "R" : "N";
                onCajaUpdate.accept(new String[]{String.valueOf(num), estado, 
                    String.valueOf(caja.getClientesEnCola()), clienteInfo, tipo});
            }
        }
    }
    
    private void mostrarResultados() {
        log("\n============ ESTADISTICAS FINALES ============");
        
        for (Caja caja : cajas) {
            log(String.format("  %s: %d clientes | Cola maxima: %d | %s", 
                    caja.getId(), caja.getTotalAtendidos(), caja.getColaMaxima(),
                    caja.esRapida() ? "RAPIDA" : "NORMAL"));
        }
        
        EstadisticasDTO stats = estadisticasService.calcularEstadisticas(cajas);
        log("----------------------------------------------");
        log("  Total clientes atendidos: " + stats.getTotalClientesAtendidos());
        log("  Total articulos vendidos: " + stats.getTotalArticulosVendidos());
        log("  Total minutos de atencion: " + stats.getTotalMinutosAtencion());
        log("  Clientes generados: " + clientesGenerados);
        log(String.format("  Prom. articulos/cliente: %.2f", stats.getArticulosPromedio()));
        log(String.format("  Prom. minutos/cliente: %.2f", stats.getTiempoPromedioAtencion()));
        log("  Cajero Estrella: " + stats.getCajeroEstrella());
        log("==============================================\n");
    }
    
    public void detener() {
        ejecutando = false;
        pausado = false;
        for (Caja caja : cajas) {
            caja.detener();
        }
        log("Simulacion detenida");
    }
    
    public void pausar() {
        if (ejecutando && !pausado) {
            pausado = true;
            for (Caja caja : cajas) {
                caja.pausar();
            }
            log("Simulacion pausada");
        }
    }
    
    public void reanudar() {
        if (ejecutando && pausado) {
            pausado = false;
            for (Caja caja : cajas) {
                caja.reanudar();
            }
            log("Simulacion reanudada");
        }
    }
    
    public boolean isEjecutando() { return ejecutando; }
    public boolean isPausado() { return pausado; }
}
