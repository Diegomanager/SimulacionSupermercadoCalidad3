package com.supermercado.domain.service;

import com.supermercado.domain.model.Caja;
import com.supermercado.domain.model.Configuracion;
import com.supermercado.domain.model.Recomendacion;
import java.util.*;

public class OptimizadorService {
    
    public static class ResultadoOptimizacion {
        public int numCajasNormales;
        public int numCajasRapidas;
        public int clientesAtendidos;
        public int colaMaxima;
        public double tiempoPromedio;
        public double mejoraPorcentaje;
        public String recomendacion;
        public double factorUtilizacion;
        public int totalCajas;
    }
    
    public List<ResultadoOptimizacion> optimizar(
            Configuracion configuracionBase,
            int maxNormales,
            int maxRapidas) {
        
        List<ResultadoOptimizacion> resultados = new ArrayList<>();
        
        for (int normales = 4; normales <= maxNormales; normales++) {
            for (int rapidas = 1; rapidas <= maxRapidas; rapidas++) {
                if (normales + rapidas > 12) continue;
                
                ResultadoOptimizacion resultado = new ResultadoOptimizacion();
                resultado.numCajasNormales = normales;
                resultado.numCajasRapidas = rapidas;
                resultado.totalCajas = normales + rapidas;
                
                double lambda = 60.0;
                double mu = 60.0 / 5.0;
                double capacidad = (normales * mu) + (rapidas * mu * 1.5);
                resultado.factorUtilizacion = lambda / capacidad;
                
                resultado.clientesAtendidos = (int)(lambda * 10 * 
                    Math.min(1, 1 / resultado.factorUtilizacion));
                
                resultado.colaMaxima = (int)(resultado.factorUtilizacion * 60);
                if (resultado.colaMaxima < 5) resultado.colaMaxima = 5;
                
                resultado.tiempoPromedio = 5.0 / Math.min(1, 1 / resultado.factorUtilizacion);
                
                if (resultado.factorUtilizacion > 0.9) {
                    resultado.recomendacion = " Sobrecargado - Considere aumentar cajas";
                } else if (resultado.factorUtilizacion < 0.5) {
                    resultado.recomendacion = "ℹ Subutilizado - Considere reducir cajas";
                } else {
                    resultado.recomendacion = " Balanceado - Configuracion optima";
                }
                
                resultados.add(resultado);
            }
        }
        
        resultados.sort((a, b) -> b.clientesAtendidos - a.clientesAtendidos);
        
        if (!resultados.isEmpty()) {
            int maxClientes = resultados.get(0).clientesAtendidos;
            for (ResultadoOptimizacion r : resultados) {
                r.mejoraPorcentaje = ((double)(r.clientesAtendidos - maxClientes) / 
                    maxClientes) * 100;
            }
        }
        
        return resultados;
    }
}