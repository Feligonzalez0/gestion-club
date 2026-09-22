package com.club.gestion.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;
    private final Integer importeCuotaMayorSemilla;
    private final Integer importeCuotaMenorSemilla;
    private final Integer importeCuotaJubiladoSemilla;
    private final int diaVencimientoSemilla;

    public ConfiguracionService(
            ConfiguracionRepository configuracionRepository,
            @Value("${app.cuota.importe-mayor-por-defecto:5000}")
            Integer importeCuotaMayorSemilla,
            @Value("${app.cuota.importe-menor-por-defecto:3000}")
            Integer importeCuotaMenorSemilla,
            @Value("${app.cuota.importe-jubilado-por-defecto:3000}")
            Integer importeCuotaJubiladoSemilla,
            @Value("${app.cuota.dia-vencimiento:10}")
            int diaVencimientoSemilla) {

        this.configuracionRepository = configuracionRepository;
        this.importeCuotaMayorSemilla = importeCuotaMayorSemilla;
        this.importeCuotaMenorSemilla = importeCuotaMenorSemilla;
        this.importeCuotaJubiladoSemilla = importeCuotaJubiladoSemilla;
        this.diaVencimientoSemilla = diaVencimientoSemilla;
    }

    public Configuracion obtener() {
        return configuracionRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(this::crearConValoresSemilla);
    }

    public Configuracion actualizar(
            Integer importeCuotaMayor,
            Integer importeCuotaMenor,
            Integer importeCuotaJubilado,
            Integer diaVencimiento) {

        validarImporte(importeCuotaMayor, "mayores");
        validarImporte(importeCuotaMenor, "menores");
        validarImporte(importeCuotaJubilado, "jubilados");

        if (diaVencimiento == null
                || diaVencimiento < 1
                || diaVencimiento > 31) {

            throw new IllegalArgumentException(
                    "El día de vencimiento debe estar entre 1 y 31.");
        }

        Configuracion configuracion = obtener();

        configuracion.setImporteCuotaMayor(importeCuotaMayor);
        configuracion.setImporteCuotaMenor(importeCuotaMenor);
        configuracion.setImporteCuotaJubilado(importeCuotaJubilado);
        configuracion.setDiaVencimiento(diaVencimiento);

        return configuracionRepository.save(configuracion);
    }

    private void validarImporte(
            Integer importe,
            String categoria) {

        if (importe == null || importe <= 0) {
            throw new IllegalArgumentException(
                    "El importe de la cuota para " + categoria
                            + " debe ser mayor a cero.");
        }
    }

    private Configuracion crearConValoresSemilla() {
        Configuracion configuracion = new Configuracion();

        configuracion.setImporteCuotaMayor(importeCuotaMayorSemilla);
        configuracion.setImporteCuotaMenor(importeCuotaMenorSemilla);
        configuracion.setImporteCuotaJubilado(importeCuotaJubiladoSemilla);
        configuracion.setDiaVencimiento(diaVencimientoSemilla);

        return configuracionRepository.save(configuracion);
    }
}