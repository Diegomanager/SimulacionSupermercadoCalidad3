package com.supermercado.domain.service;

import com.supermercado.domain.model.Caja;
import com.supermercado.domain.model.Cliente;
import com.supermercado.domain.model.Recomendacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticoServiceTest {
    
    private DiagnosticoService service;
    private List<Caja> cajas;
    
    @BeforeEach
    void setUp() {
        service = new DiagnosticoService();
        cajas = new ArrayList<>();
    }
    
    @Test
    void testDiagnostico_CajaNormal_ColaCritica() {
        Caja caja = new Caja(1, false);
        for (int i = 0; i < 50; i++) {
            caja.agregarCliente(new Cliente(i, 5));
        }
        cajas.add(caja);
        
        List<DiagnosticoService.Diagnostico> diagnosticos = 
            service.diagnosticar(cajas);
        
        assertEquals(1, diagnosticos.size());
        assertEquals(DiagnosticoService.NivelAlerta.CRITICO, 
            diagnosticos.get(0).nivel);
        assertTrue(diagnosticos.get(0).mensaje.contains("COLA EXCESIVA"));
    }
    
    @Test
    void testDiagnostico_CajaRapida_Subutilizada() {
        Caja caja = new Caja(1, true);
        
        // Simular que ya atendio muchos clientes
        for (int i = 0; i < 60; i++) {
            Cliente cliente = new Cliente(i, 3);
            caja.agregarCliente(cliente);
            caja.prepararSiguienteCliente();
            caja.finalizarAtencion();
        }
        
        // Agregar 2 clientes en cola (subutilizada)
        for (int i = 0; i < 2; i++) {
            caja.agregarCliente(new Cliente(100 + i, 3));
        }
        
        cajas.add(caja);
        
        List<DiagnosticoService.Diagnostico> diagnosticos = 
            service.diagnosticar(cajas);
        
        assertEquals(DiagnosticoService.NivelAlerta.ATENCION, 
            diagnosticos.get(0).nivel);
        assertTrue(diagnosticos.get(0).mensaje.contains("subutilizada"));
    }
    
    @Test
    void testDiagnostico_CajaNormal_ColaAlta() {
        Caja caja = new Caja(1, false);
        for (int i = 0; i < 30; i++) {
            caja.agregarCliente(new Cliente(i, 5));
        }
        cajas.add(caja);
        
        List<DiagnosticoService.Diagnostico> diagnosticos = 
            service.diagnosticar(cajas);
        
        assertEquals(DiagnosticoService.NivelAlerta.ALERTA, 
            diagnosticos.get(0).nivel);
    }
    
    @Test
    void testDiagnostico_CajaNormal_FuncionandoCorrectamente() {
        Caja caja = new Caja(1, false);
        for (int i = 0; i < 10; i++) {
            caja.agregarCliente(new Cliente(i, 5));
        }
        cajas.add(caja);
        
        List<DiagnosticoService.Diagnostico> diagnosticos = 
            service.diagnosticar(cajas);
        
        assertEquals(DiagnosticoService.NivelAlerta.NORMAL, 
            diagnosticos.get(0).nivel);
    }
    
    @Test
    void testGenerarRecomendacionGeneral_Criticos() {
        Caja caja = new Caja(1, false);
        for (int i = 0; i < 50; i++) {
            caja.agregarCliente(new Cliente(i, 5));
        }
        cajas.add(caja);
        
        List<DiagnosticoService.Diagnostico> diagnosticos = 
            service.diagnosticar(cajas);
        
        Recomendacion recomendacion = 
            service.generarRecomendacionGeneral(diagnosticos);
        
        assertEquals("CRITICO", recomendacion.getPrioridad());
        assertTrue(recomendacion.getMensaje().contains("URGENTE"));
    }
    
    @Test
    void testGenerarRecomendacionGeneral_Optimo() {
        Caja caja = new Caja(1, false);
        for (int i = 0; i < 5; i++) {
            caja.agregarCliente(new Cliente(i, 5));
        }
        cajas.add(caja);
        
        List<DiagnosticoService.Diagnostico> diagnosticos = 
            service.diagnosticar(cajas);
        
        Recomendacion recomendacion = 
            service.generarRecomendacionGeneral(diagnosticos);
        
        assertEquals("NORMAL", recomendacion.getPrioridad());
        assertTrue(recomendacion.getMensaje().contains("OPTIMA"));
    }
}