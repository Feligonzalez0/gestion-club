package com.club.gestion.pago;

import com.club.gestion.cuota.Cuota;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Representa el pago de una cuota.
 *
 * La relacion es una cuota <-> cero o un pago: al no existir pagos
 * parciales en el MVP, un pago siempre cancela una cuota completa.
 */
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuota_id", nullable = false, unique = true)
    private Cuota cuota;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private Integer importe;

    @NotNull
    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private PagoMedio medioPago;

    public Pago() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cuota getCuota() {
        return cuota;
    }

    public void setCuota(Cuota cuota) {
        this.cuota = cuota;
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
