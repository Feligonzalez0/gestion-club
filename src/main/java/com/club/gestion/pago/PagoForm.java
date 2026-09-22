package com.club.gestion.pago;

import com.club.gestion.cuota.Cuota;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO de formulario para el alta y la edicion de un pago.
 *
 * Se usa un DTO en lugar de bindear directamente la entidad {@link Pago}
 * porque el formulario solo recibe el id de la cuota (no la entidad
 * completa) y porque, en la edicion, la cuota asociada no es editable: un
 * pago siempre queda ligado a la cuota con la que se creo.
 */
public class PagoForm {

    @NotNull(message = "Debe seleccionar una cuota")
    private Long cuotaId;

    @NotNull(message = "El importe es obligatorio")
    @Positive(message = "El importe debe ser mayor a cero")
    private Integer importe;

    @NotNull(message = "La fecha de pago es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaPago;

    @NotNull(message = "El medio de pago es obligatorio")
    private PagoMedio medioPago;

    public PagoForm() {
    }

    public static PagoForm desde(Pago pago) {
        PagoForm form = new PagoForm();
        form.setCuotaId(pago.getCuota().getId());
        form.setImporte(pago.getImporte());
        form.setFechaPago(pago.getFechaPago());
        form.setMedioPago(pago.getMedioPago());
        return form;
    }

    /**
     * Vuelca los datos del formulario sobre una nueva entidad Pago. La
     * cuota se arma solo con el id (referencia minima); el service es
     * quien la resuelve por completo contra la base de datos.
     */
    public Pago aPago() {
        Pago pago = new Pago();
        Cuota cuota = new Cuota();
        cuota.setId(cuotaId);
        pago.setCuota(cuota);
        pago.setImporte(importe);
        pago.setFechaPago(fechaPago);
        pago.setMedioPago(medioPago);
        return pago;
    }

    public Long getCuotaId() {
        return cuotaId;
    }

    public void setCuotaId(Long cuotaId) {
        this.cuotaId = cuotaId;
    }

    public Integer getImporte() {
        return importe;
    }

    public void setImporte(Integer importe) {
        this.importe = importe;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public PagoMedio getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(PagoMedio medioPago) {
        this.medioPago = medioPago;
    }
}
