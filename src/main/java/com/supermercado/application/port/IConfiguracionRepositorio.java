package com.supermercado.application.port;

import com.supermercado.application.dto.ConfiguracionDTO;

public interface IConfiguracionRepositorio {
    ConfiguracionDTO cargar();
    void guardar(ConfiguracionDTO configuracion);
}
