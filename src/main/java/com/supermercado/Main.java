package com.supermercado;

import com.supermercado.application.dto.ConfiguracionDTO;
import com.supermercado.application.port.IConfiguracionRepositorio;
import com.supermercado.infrastructure.config.AppConfig;
import com.supermercado.presentation.controller.SimulacionController;
import com.supermercado.presentation.view.SimuladorFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== INICIANDO SIMULADOR DE SUPERMERCADO ===");

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            log.warn("No se pudo establecer LookAndFeel", e);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                IConfiguracionRepositorio configRepo = AppConfig.getInstance().getConfiguracionRepositorio();
                ConfiguracionDTO config = configRepo.cargar();

                SimuladorFrame frame = new SimuladorFrame();
                SimulacionController controller = new SimulacionController(frame);
                frame.setController(controller);
                frame.setConfiguracion(config);
                frame.setVisible(true);

                log.info("GUI iniciada correctamente");
            } catch (Exception e) {
                log.error("Error al iniciar la GUI", e);
                JOptionPane.showMessageDialog(null,
                    "Error al iniciar la aplicacion: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}