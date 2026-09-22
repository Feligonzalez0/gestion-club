package com.club.gestion.cuota;

import com.club.gestion.socio.Socio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Representa la cuota de un socio para un periodo (anio + mes) determinado.
 *
 * No puede existir mas de una cuota para el mismo socio en el mismo periodo:
 * esa regla se garantiza con una restriccion unica a nivel de base de datos.
 *
 * En esta etapa solo se modela la entidad; la generacion mensual automatica
 * de cuotas queda pendiente para una fase posterior.
 */
@Entity
@Table(
        name = "cuotas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cuota_socio_periodo", columnNames = {"socio_id", "anio", "mes"})
        }
)
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @NotNull
    @Column(nullable = false)
    private Integer anio;

    @NotNull
    @Min(1)
    @Max(12)
    @Column(nullable = false)
    private Integer mes;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private Integer importe;

    @NotNull
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CuotaEstado estado = CuotaEstado.PENDIENTE;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    public Cuota() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
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

    public CuotaEstado getEstado() {
        return estado;
    }

    public void setEstado(CuotaEstado estado) {
        this.estado = estado;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }
}
