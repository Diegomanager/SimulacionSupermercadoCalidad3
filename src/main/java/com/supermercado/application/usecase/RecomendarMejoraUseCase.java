package com.supermercado.application.usecase;

import com.supermercado.domain.model.Caja;
import com.supermercado.domain.model.Configuracion;
import com.supermercado.domain.model.Recomendacion;
import com.supermercado.domain.service.DiagnosticoService;
import com.supermercado.domain.service.OptimizadorService;
import com.supermercado.application.dto.RecomendacionDTO;
import java.util.List;

public class RecomendarMejoraUseCase {
    
    private final DiagnosticoService diagnosticoService;
    private final OptimizadorService optimizadorService;
    
    public RecomendarMejoraUseCase() {
        this.diagnosticoService = new DiagnosticoService();
        this.optimizadorService = new OptimizadorService();
    }
    
    public RecomendacionDTO ejecutar(List<Caja> cajas, Configuracion configuracion) {
        // Primero diagnosticar
        List<DiagnosticoService.Diagnostico> diagnosticos = 
            diagnosticoService.diagnosticar(cajas);
        
        // Generar recomendación general
        Recomendacion recomendacionGeneral = 
            diagnosticoService.generarRecomendacionGeneral(diagnosticos);
        
        // Si hay problemas críticos, optimizar
        long criticos = diagnosticos.stream()
            .filter(d -> d.nivel == DiagnosticoService.NivelAlerta.CRITICO).count();
        
        if (criticos > 0) {
            // Obtener mejor configuración
            List<OptimizadorService.ResultadoOptimizacion> resultados = 
                optimizadorService.optimizar(configuracion, 10, 5);
            
            if (!resultados.isEmpty()) {
                OptimizadorService.ResultadoOptimizacion mejor = resultados.get(0);
                String mensaje = recomendacionGeneral.getMensaje() + "\n\n" +
                    "CONFIGURACION RECOMENDADA:\n" +
                    "  Normales: " + mejor.numCajasNormales + "\n" +
                    "  Rapidas: " + mejor.numCajasRapidas + "\n" +
                    "  Mejora estimada: " + String.format("%.1f", mejor.mejoraPorcentaje) + "%";
                
                return new RecomendacionDTO(
                    mensaje,
                    "CRITICO",
                    mejor.numCajasNormales + " normales + " + 
                    mejor.numCajasRapidas + " rapidas",
                    mejor.mejoraPorcentaje,
                    false
                );
            }
        }
        
        return new RecomendacionDTO(
            recomendacionGeneral.getMensaje(),
            recomendacionGeneral.getPrioridad(),
            recomendacionGeneral.getConfiguracionSugerida(),
            recomendacionGeneral.getMejoraEstimada(),
            false
        );
    }
}