package com.supermercado.application.usecase;

import com.supermercado.domain.model.Configuracion;
import com.supermercado.domain.service.OptimizadorService;
import com.supermercado.application.dto.OptimizacionDTO;
import java.util.List;
import java.util.stream.Collectors;

public class OptimizarCajasUseCase {
    
    private final OptimizadorService optimizadorService;
    
    public OptimizarCajasUseCase() {
        this.optimizadorService = new OptimizadorService();
    }
    
    public OptimizacionDTO ejecutar(Configuracion configuracionBase, 
                                    int maxNormales, 
                                    int maxRapidas) {
        List<OptimizadorService.ResultadoOptimizacion> resultados = 
            optimizadorService.optimizar(configuracionBase, maxNormales, maxRapidas);
        
        List<OptimizacionDTO.ResultadoOptimizacion> items = resultados.stream()
            .map(r -> new OptimizacionDTO.ResultadoOptimizacion(
                r.numCajasNormales,
                r.numCajasRapidas,
                r.totalCajas,
                r.clientesAtendidos,
                r.colaMaxima,
                r.tiempoPromedio,
                r.factorUtilizacion,
                r.mejoraPorcentaje,
                r.recomendacion
            ))
            .collect(Collectors.toList());
        
        String mejorConfig = "";
        double mejora = 0.0;
        String recomendacion = "";
        
        if (!resultados.isEmpty()) {
            OptimizadorService.ResultadoOptimizacion mejor = resultados.get(0);
            mejorConfig = mejor.numCajasNormales + " normales + " + 
                         mejor.numCajasRapidas + " rapidas";
            mejora = mejor.mejoraPorcentaje;
            recomendacion = mejor.recomendacion;
        }
        
        return new OptimizacionDTO(items, mejorConfig, mejora, recomendacion);
    }
}