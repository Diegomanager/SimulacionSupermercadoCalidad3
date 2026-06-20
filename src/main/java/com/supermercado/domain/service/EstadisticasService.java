package com.supermercado.domain.service;

import com.supermercado.domain.model.Caja;
import com.supermercado.domain.model.Cliente;
import com.supermercado.application.dto.EstadisticasDTO;

import java.util.List;

public class EstadisticasService {
    
    public EstadisticasDTO calcularEstadisticas(List<Caja> cajas) {
        if (cajas == null || cajas.isEmpty()) {
            return new EstadisticasDTO();
        }
        
        int totalAtendidos = 0;
        int totalArticulos = 0;
        int totalMinutosAtencion = 0;
        int totalEnCola = 0;
        String cajeroEstrella = "-";
        int maxAtendidos = 0;
        double tiempoPromedioAtencion = 0.0;
        double articulosPromedio = 0.0;

        for (Caja caja : cajas) {
            int atendidos = caja.getTotalAtendidos();
            totalAtendidos += atendidos;
            totalEnCola += caja.getClientesEnCola();
            
            for (Cliente cliente : caja.getClientesAtendidos()) {
                totalArticulos += cliente.getCantidadArticulos();
                totalMinutosAtencion += cliente.getTiempoAtencionReal();
            }
            
            if (atendidos > maxAtendidos) {
                maxAtendidos = atendidos;
                cajeroEstrella = caja.getId();
            }
        }
        
        if (totalAtendidos > 0) {
            tiempoPromedioAtencion = (double) totalMinutosAtencion / totalAtendidos;
            articulosPromedio = (double) totalArticulos / totalAtendidos;
        }

        return new EstadisticasDTO.Builder()
                .totalClientesAtendidos(totalAtendidos)
                .totalArticulosVendidos(totalArticulos)
                .totalMinutosAtencion(totalMinutosAtencion)
                .clientesEnCola(totalEnCola)
                .cajeroEstrella(cajeroEstrella)
                .tiempoPromedioAtencion(tiempoPromedioAtencion)
                .articulosPromedio(articulosPromedio)
                .build();
    }
}
