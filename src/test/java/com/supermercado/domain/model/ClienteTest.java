package com.supermercado.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente(1, 10);
    }

    @Test
    void testConstructor_IntId() {
        // El constructor con int convierte el id a String
        assertNotNull(cliente.getId());
        assertTrue(cliente.getId().equals("1") || cliente.getId().contains("1"));
        assertEquals(10, cliente.getCantidadArticulos());
        // 10 artículos => es rápido (menos de 10 es rápido, 10 o menos también)
        assertTrue(cliente.esRapido());
        assertEquals(0, cliente.getTiempoLlegada());
        assertEquals(0, cliente.getTiempoAtencionReal());
        assertEquals(0, cliente.getTiempoInicioAtencion());
    }

    @Test
    void testConstructor_StringId() {
        Cliente cliente2 = new Cliente("C-100", 5);
        assertEquals("C-100", cliente2.getId());
        assertEquals(5, cliente2.getCantidadArticulos());
        assertTrue(cliente2.esRapido());
    }

    @Test
    void testNoRapido() {
        Cliente clienteLento = new Cliente(2, 15);
        assertEquals(15, clienteLento.getCantidadArticulos());
        assertFalse(clienteLento.esRapido());
    }

    @Test
    void testTiempos() {
        cliente.setTiempoLlegada(10);
        cliente.setTiempoAtencionReal(5);
        cliente.setTiempoSalida(18);

        assertEquals(10, cliente.getTiempoLlegada());
        assertEquals(5, cliente.getTiempoAtencionReal());
        assertEquals(18, cliente.getTiempoSalida());
    }

    @Test
    void testCalcularTiempoEspera() {
        cliente.setTiempoLlegada(5);
        cliente.setTiempoInicioAtencion(10);
        cliente.setTiempoSalida(15);

        assertEquals(5, cliente.calcularTiempoEspera());
    }

    @Test
    void testToString() {
        // El toString real devuelve: Cliente-1 [RAPIDO, 10 artículos]
        String result = cliente.toString();
        assertTrue(result.contains("Cliente-1"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("articulos") || result.contains("artículos"));
    }
}