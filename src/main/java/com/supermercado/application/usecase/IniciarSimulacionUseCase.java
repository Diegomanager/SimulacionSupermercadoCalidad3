package com.supermercado.application.usecase;

import com.supermercado.application.dto.ConfiguracionDTO;
import com.supermercado.application.dto.EstadisticasDTO;
import com.supermercado.application.port.ILogService;
import com.supermercado.domain.model.Caja;
import com.supermercado.domain.model.Cliente;
import com.supermercado.domain.service.EstadisticasService;
import com.supermercado.domain.service.RelojSimulacionService;
import com.supermercado.domain.service.SimuladorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class IniciarSimulacionUseCase {

    private static final int HORA_APERTURA = 8;
    private final ILogService logService;
    private final RelojSimulacionService reloj;
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
    private int totalClientesAtendidos = 0;
    private long minutosSimuladosTranscurridos = 0;

    private Consumer<EstadisticasDTO> onEstadisticasUpdate;
    private Consumer<String[]> onCajaUpdate;
    private Consumer<String> onLogUpdate;
    private Consumer<EstadisticasDTO> onEstadisticasDirect;  // Para inyección directa

    public IniciarSimulacionUseCase(ILogService logService) {
        this.logService = logService;
        this.reloj = new RelojSimulacionService();
        this.simuladorService = new SimuladorService(reloj);
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

    public void setOnEstadisticasDirect(Consumer<EstadisticasDTO> callback) {
        this.onEstadisticasDirect = callback;
    }

    public void setOnLogUpdate(Consumer<String> callback) {
        this.onLogUpdate = callback;
    }

    private String calcularHoraSimulada(int minutos) {
        int totalMinutos = config.getHorasSimuladas() * 60;
        if (minutos > totalMinutos) {
            minutos = totalMinutos;
        }
        if (minutos < 0) {
            minutos = 0;
        }
        int hora = HORA_APERTURA + (minutos / 60);
        int minuto = minutos % 60;
        int horaFinal = HORA_APERTURA + config.getHorasSimuladas();
        if (hora >= horaFinal) {
            return String.format("%02d:%02d", horaFinal, 0);
        }
        return String.format("%02d:%02d", hora, minuto);
    }

    private boolean estaCerrado() {
        int totalMinutos = config.getHorasSimuladas() * 60;
        return minutosSimuladosTranscurridos >= totalMinutos;
    }

    private int getClientesEnCola() {
        int total = 0;
        for (Caja caja : cajas) {
            total += caja.getClientesEnCola();
        }
        return total;
    }

    private void log(String mensaje) {
        if (onLogUpdate != null) {
            onLogUpdate.accept(mensaje);
        }
        logService.info(mensaje);
    }

    private void actualizarUI() {
        if (onEstadisticasUpdate != null) {
            EstadisticasDTO stats = estadisticasService.calcularEstadisticas(cajas);
            
            // Calcular la hora correcta usando los minutos transcurridos
            String horaSimulada = calcularHoraSimulada((int) minutosSimuladosTranscurridos);
            
            EstadisticasDTO statsCompleto = new EstadisticasDTO.Builder()
                .totalClientesAtendidos(stats.getTotalClientesAtendidos())
                .totalArticulosVendidos(stats.getTotalArticulosVendidos())
                .totalMinutosAtencion(stats.getTotalMinutosAtencion())
                .clientesEnCola(stats.getClientesEnCola())
                .cajeroEstrella(stats.getCajeroEstrella())
                .tiempoPromedioAtencion(stats.getTiempoPromedioAtencion())
                .articulosPromedio(stats.getArticulosPromedio())
                .clientesGenerados(clientesGenerados)
                .horaSimulada(horaSimulada)
                .build();
            
            System.out.println("USE CASE - Hora enviada: " + horaSimulada);
            System.out.println("USE CASE - onEstadisticasUpdate es " + (onEstadisticasUpdate != null ? "NO NULL" : "NULL"));
                        // FORZAR ACTUALIZACIÃ“N DIRECTA
            if (onEstadisticasUpdate != null) {
                onEstadisticasUpdate.accept(statsCompleto);
                System.out.println("USE CASE - onEstadisticasUpdate ACCEPT llamado");
            } else {
                System.err.println("USE CASE - onEstadisticasUpdate es NULL!");
            }
        }

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

    private void generarCliente() {
        int min = config.getArticulosClienteMin();
        int max = config.getArticulosClienteMax();
        int articulos = min + random.nextInt(max - min + 1);
        Cliente cliente = new Cliente(++clientesGenerados, articulos);
        cliente.setTiempoLlegada(reloj.getTiempoActual());
        String horaActual = calcularHoraSimulada((int) minutosSimuladosTranscurridos);
        log("[" + horaActual + "] Cliente-" + cliente.getId() + " generado | Articulos: " + articulos);
        simuladorService.asignarCliente(cajas, cliente);
    }

    private void ejecutarCaja(Caja caja) {
        while (caja.estaActiva()) {
            try {
                while (pausado && ejecutando) {
                    Thread.sleep(50);
                }
                Cliente cliente = caja.prepararSiguienteCliente();
                if (cliente == null) {
                    Thread.sleep(msPerMinutoSimulado / 2);
                    continue;
                }
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
                String horaActual = calcularHoraSimulada((int) minutosSimuladosTranscurridos);
                log("[" + horaActual + "] Atendiendo: " + cliente.getId() + " en " + caja.getId() +
                    " (" + tiempoAtencionMinutos + " min)");
                long sleepMs = Math.max(50, tiempoAtencionMinutos * msPerMinutoSimulado);
                Thread.sleep(sleepMs);
                cliente.setTiempoSalida(reloj.getTiempoActual());
                caja.finalizarAtencion();
                totalClientesAtendidos++;
                log("Finalizada atencion de " + cliente.getId() + " en " + caja.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
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
        EstadisticasDTO estadisticas = estadisticasService.calcularEstadisticas(cajas);
        log("----------------------------------------------");
        log("  Total clientes atendidos: " + estadisticas.getTotalClientesAtendidos());
        log("  Total articulos vendidos: " + estadisticas.getTotalArticulosVendidos());
        log("  Total minutos de atencion: " + estadisticas.getTotalMinutosAtencion());
        log("  Clientes generados: " + clientesGenerados);
        log(String.format("  Prom. articulos/cliente: %.2f", estadisticas.getArticulosPromedio()));
        log(String.format("  Prom. minutos/cliente: %.2f", estadisticas.getTiempoPromedioAtencion()));
        log("  Cajero Estrella: " + estadisticas.getCajeroEstrella());
        log("==============================================\n");
    }

    public void ejecutar(ConfiguracionDTO config) throws InterruptedException {
        if (ejecutando) throw new IllegalStateException("La simulacion ya esta en ejecucion");
        this.config = config;
        this.ejecutando = true;
        this.pausado = false;
        this.clientesGenerados = 0;
        this.totalClientesAtendidos = 0;
        this.minutosSimuladosTranscurridos = 0;
        this.cajas.clear();
        this.hilosCajas.clear();

        log("=== INICIANDO SIMULACION ===");
        log("Horario: 08:00 - " + (HORA_APERTURA + config.getHorasSimuladas()) + ":00 (" + config.getHorasSimuladas() + " horas)");
        log("Duracion real: " + config.getDuracionRealSegundos() + "s | Tiempo simulado: " + config.getHorasSimuladas() + "h");
        log("Cajas Normales: " + config.getNumCajasNormales() + " | Cajas Rapidas: " + config.getNumCajasRapidas());

        int numRapidas = config.getNumCajasRapidas();
        int numNormales = config.getNumCajasNormales();
        for (int i = 1; i <= numRapidas; i++) cajas.add(new Caja(i, true));
        for (int i = 1; i <= numNormales; i++) cajas.add(new Caja(numRapidas + i, false));
        log("Total cajas creadas: " + cajas.size());

        for (Caja caja : cajas) {
            Thread hilo = new Thread(() -> ejecutarCaja(caja));
            hilo.start();
            hilosCajas.add(hilo);
        }

        minSimulados = config.getHorasSimuladas() * 60;
        int duracionRealMs = config.getDuracionRealSegundos() * 1000;
        msPerMinutoSimulado = Math.max(1, duracionRealMs / minSimulados);
        long tickMs = Math.max(10, msPerMinutoSimulado / 2);
        log("Escala: 1 minuto simulado = " + msPerMinutoSimulado + "ms reales");

        reloj.iniciar();
        tiempoInicio = System.currentTimeMillis();
        int limiteClientes = config.getLimiteClientes();
        long duracionMs = config.getDuracionRealSegundos() * 1000L;
        long tiempoTranscurrido = 0;
        int ticks = 0;

        while (ejecutando && tiempoTranscurrido < duracionMs) {
            while (pausado && ejecutando) {
                Thread.sleep(50);
                tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio;
            }
            tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio;
            ticks++;
            minutosSimuladosTranscurridos = (tiempoTranscurrido * minSimulados) / duracionMs;
            reloj.setTiempoSimulado(minutosSimuladosTranscurridos);

            if (estaCerrado()) {
                log("CIERRE DEL SUPERMERCADO (" + (HORA_APERTURA + config.getHorasSimuladas()) + ":00)");
                break;
            }

            if (limiteClientes > 0 && clientesGenerados >= limiteClientes) {
                log("Limite de " + limiteClientes + " clientes alcanzado.");
                break;
            }

            if (random.nextInt(100) < config.getProbabilidadLlegadaCliente()) {
                generarCliente();
            }

            if (ticks % 3 == 0) actualizarUI();
            Thread.sleep(tickMs);
        }

        log("Esperando que terminen las atenciones...");
        boolean hayPendientes;
        int intentos = 0;
        do {
            hayPendientes = false;
            for (Caja caja : cajas) {
                if (caja.tieneClientesPendientes() || caja.estaOcupada()) {
                    hayPendientes = true;
                    break;
                }
            }
            if (hayPendientes) {
                Thread.sleep(msPerMinutoSimulado / 2);
                intentos++;
                if (intentos > 200) break;
            }
        } while (hayPendientes && ejecutando);

        for (Caja caja : cajas) caja.detener();
        reloj.detener();
        ejecutando = false;
        log("=== SIMULACION COMPLETADA ===");
        mostrarResultados();
    }

    public void detener() {
        ejecutando = false;
        pausado = false;
        for (Caja caja : cajas) caja.detener();
        log("Simulacion detenida");
    }

    public void pausar() {
        if (ejecutando && !pausado) {
            pausado = true;
            reloj.pausar();
            for (Caja caja : cajas) {
                caja.pausar();
            }
            log("Simulacion pausada");
            actualizarUI();
        }
    }

    public void reanudar() {
        if (ejecutando && pausado) {
            pausado = false;
            reloj.reanudar();
            for (Caja caja : cajas) {
                caja.reanudar();
            }
            log("Simulacion reanudada");
            actualizarUI();
        }
    }

    public EstadisticasDTO obtenerEstadisticas() {
        return estadisticasService.calcularEstadisticas(cajas);
    }

    public boolean isEjecutando() { return ejecutando; }
    public boolean isPausado() { return pausado; }
}