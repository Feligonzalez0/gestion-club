package com.club.gestion.socio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO de formulario para el alta y la edicion de un socio.
 *
 * Se usa un DTO en lugar de bindear directamente la entidad {@link Socio}
 * porque el formulario no expone {@code estado} ni {@code fechaAlta} (los
 * gestiona el sistema), y porque en el alta no se pide {@code numeroSocio}
 * (se asigna automaticamente): bindear la entidad obligaria a relajar sus
 * validaciones NotNull para ese caso.
 */
public class SocioForm {

    private Long numeroSocio;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    private String telefono;

    private String direccion;

    @Email(message = "El email no tiene un formato valido")
    private String email;

    @NotNull(message = "La categoria es obligatoria")
    private SocioCategoria categoria;

    public SocioForm() {
    }

    public static SocioForm desde(Socio socio) {
        SocioForm form = new SocioForm();
        form.setNumeroSocio(socio.getNumeroSocio());
        form.setNombre(socio.getNombre());
        form.setApellido(socio.getApellido());
        form.setDni(socio.getDni());
        form.setFechaNacimiento(socio.getFechaNacimiento());
        form.setTelefono(socio.getTelefono());
        form.setDireccion(socio.getDireccion());
        form.setEmail(socio.getEmail());
        form.setCategoria(socio.getCategoria());
        return form;
    }

    /**
     * Vuelca los datos editables del formulario sobre una entidad Socio.
     * No toca id, estado ni fechaAlta: esos campos los administra el
     * SocioService.
     */
    public void copiarA(Socio socio) {
        socio.setNumeroSocio(numeroSocio);
        socio.setNombre(nombre);
        socio.setApellido(apellido);
        socio.setDni(dni);
        socio.setFechaNacimiento(fechaNacimiento);
        socio.setTelefono(telefono);
        socio.setDireccion(direccion);
        socio.setEmail(email);
        socio.setCategoria(categoria);
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

    public SocioCategoria getCategoria() {
        return categoria;
    }

    public void setCategoria(SocioCategoria categoria) {
        this.categoria = categoria;
    }
}
