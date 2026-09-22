package com.club.gestion.socio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Representa a un socio del club.
 *
 * Los socios no se eliminan fisicamente: la baja se modela a traves del
 * campo {@link #estado} (INACTIVO), nunca borrando el registro.
 */
@Entity
@Table(
        name = "socios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_socio_numero_socio", columnNames = "numero_socio"),
                @UniqueConstraint(name = "uk_socio_dni", columnNames = "dni")
        }
)
public class Socio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "numero_socio", nullable = false)
    private Long numeroSocio;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @NotBlank
    @Column(nullable = false)
    private String apellido;

    @NotBlank
    @Column(nullable = false)
    private String dni;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    private String telefono;

    private String direccion;

    @Email
    private String email;

    @NotNull
    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocioEstado estado;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocioCategoria categoria;

    public Socio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumeroSocio() {
        return numeroSocio;
    }

    public void setNumeroSocio(Long numeroSocio) {
        this.numeroSocio = numeroSocio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDate fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public SocioEstado getEstado() {
        return estado;
    }

    public void setEstado(SocioEstado estado) {
        this.estado = estado;
    }

    public SocioCategoria getCategoria() {
        return categoria;
    }

    public void setCategoria(SocioCategoria categoria) {
        this.categoria = categoria;
    }
}
