package com.club.gestion.cuota;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO de formulario para la edicion de una cuota individual.
 *
 * Solo se pueden corregir el importe y el vencimiento: el socio y el periodo
 * (anio/mes) identifican a la cuota y no son editables. Se usa un DTO en
 * lugar de bindear directamente la entidad {@link Cuota} para que el
 * formulario no pueda modificar esos otros campos.
 */
public class CuotaForm {

    @NotNull(message = "El importe es obligatorio")
    @Positive(message = "El importe debe ser mayor a cero")
    private Integer importe;

    @NotNull(message = "El vencimiento es obligatorio")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;

    public CuotaForm() {
    }

    public static CuotaForm desde(Cuota cuota) {
        CuotaForm form = new CuotaForm();
        form.setImporte(cuota.getImporte());
        form.setFechaVencimiento(cuota.getFechaVencimiento());
        return form;
    }

    /**
     * Vuelca los datos del formulario sobre una Cuota "de paso" que el
     * service usa para actualizar la cuota real.
     */
    public Cuota aCuota() {
        Cuota cuota = new Cuota();
        cuota.setImporte(importe);
        cuota.setFechaVencimiento(fechaVencimiento);
        return cuota;
    }

    public Integer getImporte() {
        return importe;
    }

    public void setImporte(Integer importe) {
        this.importe = importe;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}
