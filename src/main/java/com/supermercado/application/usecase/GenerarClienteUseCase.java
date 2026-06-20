package com.supermercado.application.usecase;

import com.supermercado.domain.model.Cliente;

import java.util.Random;

public class GenerarClienteUseCase {
    
    private static int contador = 0;
    private final Random random;
    
    public GenerarClienteUseCase() {
        this.random = new Random();
    }
    
    public Cliente ejecutar(int minArticulos, int maxArticulos) {
        if (minArticulos < 0 || maxArticulos < minArticulos) {
            throw new IllegalArgumentException("Rango de art?culos inv?lido");
        }
        int articulos = minArticulos + random.nextInt(maxArticulos - minArticulos + 1);
        return new Cliente(++contador, articulos);
    }
}
