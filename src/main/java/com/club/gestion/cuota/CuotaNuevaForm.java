package com.club.gestion.cuota;

import com.club.gestion.socio.Socio;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO de formulario para crear una cuota individual.
 *
 * Se usa un DTO en lugar de bindear directamente la entidad {@link Cuota}
 * porque el formulario solo recibe el id del socio (no la entidad completa),
 * y para que el estado y el resto de los campos internos no se puedan
 * manipular desde el formulario. Los limites de anio y mes son los mismos
 * que usa el formulario de generacion mensual.
 */
public class CuotaNuevaForm {

    @NotNull(message = "Debe seleccionar un socio")
    private Long socioId;

    @NotNull(message = "El año es obligatorio")
    @Min(value = 2000, message = "El año debe estar entre 2000 y 2100")
    @Max(value = 2100, message = "El año debe estar entre 2000 y 2100")
    private Integer anio;

    @NotNull(message = "El mes es obligatorio")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    private Integer mes;

    @NotNull(message = "El importe es obligatorio")
    @Positive(message = "El importe debe ser mayor a cero")
    private Integer importe;

    @NotNull(message = "El vencimiento es obligatorio")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;

    public CuotaNuevaForm() {
    }

    /**
     * Arma la nueva Cuota con los datos del formulario y el socio ya
     * resuelto. La cuota nace PENDIENTE (estado por defecto de la entidad).
     */
    public Cuota aCuota(Socio socio) {
        Cuota cuota = new Cuota();
        cuota.setSocio(socio);
        cuota.setAnio(anio);
        cuota.setMes(mes);
        cuota.setImporte(importe);
        cuota.setFechaVencimiento(fechaVencimiento);
        return cuota;
    }

    public Long getSocioId() {
        return socioId;
    }

    public void setSocioId(Long socioId) {
        this.socioId = socioId;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
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
