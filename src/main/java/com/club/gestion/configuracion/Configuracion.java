package com.club.gestion.configuracion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuración general del club.
 *
 * Para el MVP se utiliza una única fila de configuración.
 */
@Entity
@Table(name = "configuracion")
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(
            name = "importe_cuota_mayor",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private Integer importeCuotaMayor;

    @NotNull
    @Positive
    @Column(
            name = "importe_cuota_menor",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private Integer importeCuotaMenor;

    @NotNull
    @Positive
    @Column(
            name = "importe_cuota_jubilado",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private Integer importeCuotaJubilado;

    @NotNull
    @Positive
    @Max(31)
    @Column(name = "dia_vencimiento", nullable = false)
    private Integer diaVencimiento;

    public Configuracion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getImporteCuotaMayor() {
        return importeCuotaMayor;
    }

    public void setImporteCuotaMayor(Integer importeCuotaMayor) {
        this.importeCuotaMayor = importeCuotaMayor;
    }

    public Integer getImporteCuotaMenor() {
        return importeCuotaMenor;
    }

    public void setImporteCuotaMenor(Integer importeCuotaMenor) {
        this.importeCuotaMenor = importeCuotaMenor;
    }

    public Integer getImporteCuotaJubilado() {
        return importeCuotaJubilado;
    }

    public void setImporteCuotaJubilado(Integer importeCuotaJubilado) {
        this.importeCuotaJubilado = importeCuotaJubilado;
    }

    public Integer getDiaVencimiento() {
        return diaVencimiento;
    }

    public void setDiaVencimiento(Integer diaVencimiento) {
        this.diaVencimiento = diaVencimiento;
    }
}