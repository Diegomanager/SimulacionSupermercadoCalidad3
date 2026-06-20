package com.supermercado.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CajaTest {

    private Caja cajaRapida;
    private Caja cajaNormal;

    @BeforeEach
    void setUp() {
        cajaRapida = new Caja(1, true);
        cajaNormal = new Caja(2, false);
    }

    @Test
    void testConstructor_IntId() {
        // El getId() puede devolver "CAJA 1" o "CAJA 1 (R)" dependiendo de la implementación
        String id = cajaRapida.getId();
        assertTrue(id.contains("CAJA 1"));
        assertTrue(cajaRapida.esRapida());
        assertEquals(EstadoCaja.LIBRE, cajaRapida.getEstado());
        assertEquals(0, cajaRapida.getClientesEnCola());
        assertEquals(0, cajaRapida.getTotalAtendidos());
        assertNull(cajaRapida.getClienteActual());
        assertEquals(0, cajaRapida.getColaMaxima());
    }

    @Test
    void testAgregarCliente() {
        Cliente cliente = new Cliente(1, 5);
        assertTrue(cajaRapida.agregarCliente(cliente));
        assertEquals(1, cajaRapida.getClientesEnCola());
    }

    @Test
    void testPrepararSiguienteCliente() {
        Cliente cliente1 = new Cliente(1, 5);
        Cliente cliente2 = new Cliente(2, 3);
        cajaRapida.agregarCliente(cliente1);
        cajaRapida.agregarCliente(cliente2);

        Cliente preparado = cajaRapida.prepararSiguienteCliente();
        assertEquals(cliente1, preparado);
        assertEquals(1, cajaRapida.getClientesEnCola());
        assertEquals(EstadoCaja.OCUPADA, cajaRapida.getEstado());
    }

    @Test
    void testFinalizarAtencion() {
        Cliente cliente = new Cliente(1, 5);
        cajaRapida.agregarCliente(cliente);
        cajaRapida.prepararSiguienteCliente();

        assertEquals(0, cajaRapida.getTotalAtendidos());
        cajaRapida.finalizarAtencion();
        assertEquals(1, cajaRapida.getTotalAtendidos());
        assertEquals(EstadoCaja.LIBRE, cajaRapida.getEstado());
        assertNull(cajaRapida.getClienteActual());
    }

    @Test
    void testPausarYReanudar() {
        // Primero agregar un cliente y poner la caja en estado OCUPADA
        Cliente cliente = new Cliente(1, 5);
        cajaRapida.agregarCliente(cliente);
        cajaRapida.prepararSiguienteCliente();
        assertEquals(EstadoCaja.OCUPADA, cajaRapida.getEstado());
        
        // Pausar - ahora sí debe funcionar porque la caja está OCUPADA
        cajaRapida.pausar();
        assertEquals(EstadoCaja.PAUSADA, cajaRapida.getEstado());
        // NOTA: estaActiva() devuelve true para PAUSADA (solo DETENIDA es false)
        // Verificamos que el estado cambió correctamente
        assertTrue(cajaRapida.estaActiva());
        assertEquals(EstadoCaja.PAUSADA, cajaRapida.getEstado());
        
        // Reanudar
        cajaRapida.reanudar();
        // Después de reanudar, debe volver a OCUPADA
        assertEquals(EstadoCaja.OCUPADA, cajaRapida.getEstado());
        assertTrue(cajaRapida.estaActiva());
        
        // Verificar que el cliente sigue siendo el mismo
        assertNotNull(cajaRapida.getClienteActual());
        assertEquals(cliente, cajaRapida.getClienteActual());
    }

    @Test
    void testDetener() {
        cajaRapida.detener();
        assertEquals(EstadoCaja.DETENIDA, cajaRapida.getEstado());
    }

    @Test
    void testTieneClientesPendientes() {
        assertFalse(cajaRapida.tieneClientesPendientes());

        cajaRapida.agregarCliente(new Cliente(1, 5));
        assertTrue(cajaRapida.tieneClientesPendientes());

        cajaRapida.prepararSiguienteCliente();
        assertTrue(cajaRapida.tieneClientesPendientes());

        cajaRapida.finalizarAtencion();
        assertFalse(cajaRapida.tieneClientesPendientes());
    }

    @Test
    void testEstaOcupada() {
        assertFalse(cajaRapida.estaOcupada());
        
        cajaRapida.agregarCliente(new Cliente(1, 5));
        cajaRapida.prepararSiguienteCliente();
        assertTrue(cajaRapida.estaOcupada());
        
        cajaRapida.finalizarAtencion();
        assertFalse(cajaRapida.estaOcupada());
    }
}