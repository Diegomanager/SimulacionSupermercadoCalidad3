package com.supermercado.regression;

import com.supermercado.application.dto.ConfiguracionDTO;
import com.supermercado.application.usecase.IniciarSimulacionUseCase;
import com.supermercado.infrastructure.service.LogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("regression")
class SimulacionRegressionTest {

    private ConfiguracionDTO config;
    private IniciarSimulacionUseCase useCase;

    @BeforeEach
    void setUp() {
        config = new ConfiguracionDTO.Builder()
            .numCajasNormales(2)
            .numCajasRapidas(1)
            .horasSimuladas(1)
            .duracionRealSegundos(3)
            .probabilidadLlegadaCliente(30)
            .limiteClientes(20)
            .articulosClienteMin(1)
            .articulosClienteMax(10)
            .tiempoCajaNormalMin(1)
            .tiempoCajaNormalMax(2)
            .tiempoCajaRapidaMin(1)
            .tiempoCajaRapidaMax(2)
            .build();
        
        useCase = new IniciarSimulacionUseCase(new LogServiceImpl());
    }

    @Test
    @Tag("regression")
    void testRegression_InicioSimulacion() {
        assertDoesNotThrow(() -> {
            useCase.ejecutar(config);
        }, "La simulacion deberia iniciar sin errores");
    }

    @Test
    @Tag("regression")
    void testRegression_PausaYReanudacion() throws InterruptedException {
        Thread simThread = new Thread(() -> {
            try {
                useCase.ejecutar(config);
            } catch (Exception e) {
                // Ignorar
            }
        });
        simThread.start();
        
        Thread.sleep(500);
        
        assertDoesNotThrow(() -> useCase.pausar());
        assertTrue(useCase.isPausado());
        
        assertDoesNotThrow(() -> useCase.reanudar());
        assertFalse(useCase.isPausado());
        
        useCase.detener();
        simThread.join(1000);
    }

    @Test
    @Tag("regression")
    void testRegression_DetencionSimulacion() throws InterruptedException {
        Thread simThread = new Thread(() -> {
            try {
                useCase.ejecutar(config);
            } catch (Exception e) {
                // Ignorar
            }
        });
        simThread.start();
        
        Thread.sleep(500);
        
        assertDoesNotThrow(() -> useCase.detener());
        assertFalse(useCase.isEjecutando());
        
        simThread.join(1000);
    }

    @Test
    @Tag("regression")
    void testRegression_ConfiguracionValida() {
        assertTrue(config.getNumCajasNormales() > 0);
        assertTrue(config.getNumCajasRapidas() >= 0);
        assertTrue(config.getHorasSimuladas() > 0);
        assertTrue(config.getProbabilidadLlegadaCliente() >= 0 && config.getProbabilidadLlegadaCliente() <= 100);
        assertTrue(config.getArticulosClienteMin() <= config.getArticulosClienteMax());
        assertTrue(config.getTiempoCajaNormalMin() <= config.getTiempoCajaNormalMax());
        assertTrue(config.getTiempoCajaRapidaMin() <= config.getTiempoCajaRapidaMax());
    }

    @Test
    @Tag("regression")
    void testRegression_SimulacionCompleta() throws InterruptedException {
        ConfiguracionDTO configLarga = new ConfiguracionDTO.Builder()
            .numCajasNormales(2)
            .numCajasRapidas(1)
            .horasSimuladas(1)
            .duracionRealSegundos(4)
            .probabilidadLlegadaCliente(30)
            .limiteClientes(10)
            .articulosClienteMin(1)
            .articulosClienteMax(10)
            .tiempoCajaNormalMin(1)
            .tiempoCajaNormalMax(2)
            .tiempoCajaRapidaMin(1)
            .tiempoCajaRapidaMax(2)
            .build();
        
        IniciarSimulacionUseCase useCaseLarga = new IniciarSimulacionUseCase(new LogServiceImpl());
        
        Thread simThread = new Thread(() -> {
            try {
                useCaseLarga.ejecutar(configLarga);
            } catch (Exception e) {
                // Ignorar
            }
        });
        simThread.start();
        simThread.join(10000);
        
        assertFalse(useCaseLarga.isEjecutando());
    }
}