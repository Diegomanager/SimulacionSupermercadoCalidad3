package com.supermercado.domain.service;

import com.supermercado.domain.model.Configuracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptimizadorServiceTest {
    
    private OptimizadorService service;
    private Configuracion configuracion;
    
    @BeforeEach
    void setUp() {
        service = new OptimizadorService();
        configuracion = new Configuracion.Builder()
            .numCajasNormales(6)
            .numCajasRapidas(2)
            .build();
    }
    
    @Test
    void testOptimizar_DevuelveResultados() {
        List<OptimizadorService.ResultadoOptimizacion> resultados = 
            service.optimizar(configuracion, 10, 5);
        
        assertNotNull(resultados);
        assertFalse(resultados.isEmpty());
    }
    
    @Test
    void testOptimizar_OrdenaPorClientesAtendidos() {
        List<OptimizadorService.ResultadoOptimizacion> resultados = 
            service.optimizar(configuracion, 10, 5);
        
        assertTrue(resultados.get(0).clientesAtendidos >= 
                   resultados.get(resultados.size() - 1).clientesAtendidos);
    }
    
    @Test
    void testOptimizar_NoExcedeMaximos() {
        int maxNormales = 8;
        int maxRapidas = 4;
        
        List<OptimizadorService.ResultadoOptimizacion> resultados = 
            service.optimizar(configuracion, maxNormales, maxRapidas);
        
        for (OptimizadorService.ResultadoOptimizacion r : resultados) {
            assertTrue(r.numCajasNormales <= maxNormales);
            assertTrue(r.numCajasRapidas <= maxRapidas);
        }
    }
    
    @Test
    void testOptimizar_CalculaFactorUtilizacion() {
        List<OptimizadorService.ResultadoOptimizacion> resultados = 
            service.optimizar(configuracion, 10, 5);
        
        for (OptimizadorService.ResultadoOptimizacion r : resultados) {
            assertTrue(r.factorUtilizacion > 0);
            assertTrue(r.factorUtilizacion < 2);
        }
    }
}