package com.supermercado.infrastructure.service;

import com.supermercado.application.dto.EstadisticasDTO;
import com.supermercado.application.port.IReporteExportador;
import com.supermercado.application.port.ILogService;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReporteExcelImpl implements IReporteExportador {
    
    private final ILogService logService;
    
    public ReporteExcelImpl(ILogService logService) {
        this.logService = logService;
    }
    
    @Override
    public void exportar(EstadisticasDTO estadisticas, String rutaArchivo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(rutaArchivo))) {
            writer.println("=== REPORTE DE SIMULACI?N ===");
            writer.println("Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.println();
            writer.println("ESTAD?STICAS GENERALES");
            writer.println("----------------------------------------");
            writer.println("Total clientes atendidos: " + estadisticas.getTotalClientesAtendidos());
            writer.println("Total art?culos vendidos: " + estadisticas.getTotalArticulosVendidos());
            writer.println("Total minutos de atenci?n: " + estadisticas.getTotalMinutosAtencion());
            writer.println("Clientes en cola: " + estadisticas.getClientesEnCola());
            writer.println("Cajero Estrella: " + estadisticas.getCajeroEstrella());
            writer.println("Tiempo promedio de atenci?n: " + String.format("%.2f", estadisticas.getTiempoPromedioAtencion()));
            writer.println("Art?culos promedio: " + String.format("%.2f", estadisticas.getArticulosPromedio()));
            writer.println("----------------------------------------");
            
            logService.info("Reporte exportado a: " + rutaArchivo);
        } catch (IOException e) {
            logService.error("Error al exportar reporte", e);
        }
    }
}
