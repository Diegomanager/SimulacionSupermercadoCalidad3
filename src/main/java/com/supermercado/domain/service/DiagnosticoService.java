package com.supermercado.domain.service;

import com.supermercado.domain.model.Caja;
import com.supermercado.domain.model.Recomendacion;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticoService {
    
    public enum NivelAlerta { 
        NORMAL("", "Funcionando correctamente"),
        ATENCION("", "Monitorear"),
        ALERTA("", "Requiere atencion"),
        CRITICO("", "¡URGENTE!");
        
        public final String icono;
        public final String descripcion;
        
        NivelAlerta(String icono, String descripcion) {
            this.icono = icono;
            this.descripcion = descripcion;
        }
    }
    
    public static class Diagnostico {
        public String cajaId;
        public NivelAlerta nivel;
        public String mensaje;
        public String sugerencia;
        public int colaActual;
        public int colaMaxima;
        public int clientesAtendidos;
        public boolean esRapida;
        
        @Override
        public String toString() {
            return String.format("%s %s: %s (Cola: %d/%d)", 
                nivel.icono, cajaId, mensaje, colaActual, colaMaxima);
        }
    }
    
    public List<Diagnostico> diagnosticar(List<Caja> cajas) {
        List<Diagnostico> diagnosticos = new ArrayList<>();
        
        for (Caja caja : cajas) {
            Diagnostico d = new Diagnostico();
            d.cajaId = caja.getId();
            d.colaActual = caja.getClientesEnCola();
            d.colaMaxima = caja.getColaMaxima();
            d.clientesAtendidos = caja.getTotalAtendidos();
            d.esRapida = caja.esRapida();
            
            if (d.colaActual > 40) {
                d.nivel = NivelAlerta.CRITICO;
                d.mensaje = "COLA EXCESIVA " + d.colaActual + " clientes esperando";
                d.sugerencia = d.esRapida ? 
                    "Convertir a NORMAL o abrir otra caja RAPIDA" :
                    "Aumentar numero de cajas NORMALES o reducir tiempo de atencion";
            } else if (d.colaActual > 25) {
                d.nivel = NivelAlerta.ALERTA;
                d.mensaje = "Cola alta: " + d.colaActual + " clientes";
                d.sugerencia = "Considerar abrir otra caja o revisar tiempos";
            } else if (d.colaActual < 3 && d.clientesAtendidos > 50) {
                d.nivel = NivelAlerta.ATENCION;
                d.mensaje = "Caja subutilizada: solo " + d.colaActual + " clientes en cola";
                d.sugerencia = d.esRapida ? 
                    "Redirigir mas clientes a esta caja RAPIDA" :
                    "Considerar convertir a RAPIDA";
            } else {
                d.nivel = NivelAlerta.NORMAL;
                d.mensaje = "Funcionando correctamente";
                d.sugerencia = "Mantener configuracion actual";
            }
            
            diagnosticos.add(d);
        }
        
        return diagnosticos;
    }
    
    public Recomendacion generarRecomendacionGeneral(List<Diagnostico> diagnosticos) {
        long criticos = diagnosticos.stream()
            .filter(d -> d.nivel == NivelAlerta.CRITICO).count();
        long alertas = diagnosticos.stream()
            .filter(d -> d.nivel == NivelAlerta.ALERTA).count();
        long atencion = diagnosticos.stream()
            .filter(d -> d.nivel == NivelAlerta.ATENCION).count();
        
        StringBuilder mensaje = new StringBuilder();
        String prioridad = "NORMAL";
        String configSugerida = "";
        double mejora = 0.0;
        
        if (criticos > 0) {
            prioridad = "CRITICO";
            mensaje.append("URGENTE: " + criticos + " cajas con cola critica.\n");
            mensaje.append("   Aumentar el numero de cajas\n");
            mensaje.append("   Reducir tiempos de atencion\n");
            mensaje.append("   Revisar distribucion de clientes");
            configSugerida = "Aumentar cajas en " + criticos + " unidades";
            mejora = 25.0;
        } else if (alertas > 0) {
            prioridad = "ALTO";
            mensaje.append("ATENCION: " + alertas + " cajas con cola alta.\n");
            mensaje.append("   Considerar abrir mas cajas en horas pico\n");
            mensaje.append("   Optimizar tiempos de atencion");
            configSugerida = "Ajustar tiempos o agregar cajas temporales";
            mejora = 15.0;
        } else if (atencion > 0) {
            prioridad = "MEDIO";
            mensaje.append("INFO: " + atencion + " cajas subutilizadas.\n");
            mensaje.append("   Redistribuir clientes a estas cajas\n");
            mensaje.append("   Considerar convertir cajas normales a rapidas");
            configSugerida = "Redistribuir carga entre cajas";
            mejora = 10.0;
        } else {
            mensaje.append("CONFIGURACION OPTIMA!\n");
            mensaje.append("   Todas las cajas funcionan correctamente\n");
            mensaje.append("   Mantener la configuracion actual");
            configSugerida = "Mantener configuracion actual";
            mejora = 0.0;
        }
        
        return new Recomendacion(
            mensaje.toString(),
            prioridad,
            configSugerida,
            mejora,
            false
        );
    }
}