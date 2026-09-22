package com.club.gestion.socio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.club.gestion.cuota.CuotaRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Logica de negocio relacionada a los socios: alta, edicion, activacion/
 * desactivacion logica y busqueda basica.
 *
 * El numero de socio se asigna automaticamente al crear (siguiente
 * disponible) salvo que ya venga informado; en la edicion si es un campo
 * editable por el usuario. Ningun socio se elimina fisicamente: la baja se
 * modela cambiando su estado a INACTIVO.
 */
@Service
public class SocioService {

    private final SocioRepository socioRepository;
    private final CuotaRepository cuotaRepository;

    public SocioService(SocioRepository socioRepository, CuotaRepository cuotaRepository) {
        this.socioRepository = socioRepository;
        this.cuotaRepository = cuotaRepository;
    }

    public Socio crear(Socio socio) {
        if (socio.getNumeroSocio() == null) {
            socio.setNumeroSocio(siguienteNumeroSocio());
        }

        validarDniUnico(socio.getDni(), null);
        validarNumeroSocioUnico(socio.getNumeroSocio(), null);

        if (socio.getEstado() == null) {
            socio.setEstado(SocioEstado.ACTIVO);
        }
        if (socio.getFechaAlta() == null) {
            socio.setFechaAlta(LocalDate.now());
        }

        return socioRepository.save(socio);
    }

    public Socio actualizar(Long id, Socio datosActualizados) {
        Socio socio = socioRepository.findById(id).orElseThrow();

        if (datosActualizados.getNumeroSocio() == null) {
            throw new IllegalArgumentException("El numero de socio es obligatorio.");
        }

        validarDniUnico(datosActualizados.getDni(), id);
        validarNumeroSocioUnico(datosActualizados.getNumeroSocio(), id);

        socio.setNumeroSocio(datosActualizados.getNumeroSocio());
        socio.setNombre(datosActualizados.getNombre());
        socio.setApellido(datosActualizados.getApellido());
        socio.setDni(datosActualizados.getDni());
        socio.setFechaNacimiento(datosActualizados.getFechaNacimiento());
        socio.setTelefono(datosActualizados.getTelefono());
        socio.setDireccion(datosActualizados.getDireccion());
        socio.setEmail(datosActualizados.getEmail());
        socio.setCategoria(datosActualizados.getCategoria());

        return socioRepository.save(socio);
    }

    public Socio desactivar(Long id) {
        Socio socio = socioRepository.findById(id).orElseThrow();
        socio.setEstado(SocioEstado.INACTIVO);
        return socioRepository.save(socio);
    }

    public Socio activar(Long id) {
        Socio socio = socioRepository.findById(id).orElseThrow();
        socio.setEstado(SocioEstado.ACTIVO);
        return socioRepository.save(socio);
    }

    public List<Socio> listarTodos() {
        return socioRepository.findAll();
    }

    public List<Socio> listarActivos() {
        return socioRepository.findByEstado(SocioEstado.ACTIVO);
    }

    public List<Socio> listarInactivos() {
        return socioRepository.findByEstado(SocioEstado.INACTIVO);
    }

    /**
     * Busca coincidencias parciales en nombre, apellido, DNI y telefono, y
     * ademas una coincidencia exacta por numero de socio cuando el termino
     * de busqueda es numerico. Si no se informa termino, devuelve todos los
     * socios. Resuelto enteramente a traves del repositorio, sin traer
     * todos los registros a memoria para filtrar.
     */
    /*
    public List<Socio> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarActivos();
        }

        String termino = texto.trim();
        List<Socio> resultados = new ArrayList<>(
                socioRepository.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrDniContainingIgnoreCaseOrTelefonoContainingIgnoreCase(
                        termino, termino, termino, termino));

        try {
            Long numeroSocio = Long.valueOf(termino);
            socioRepository.findByNumeroSocio(numeroSocio).ifPresent(socio -> {
                boolean yaIncluido = resultados.stream()
                        .anyMatch(s -> s.getId().equals(socio.getId()));
                if (!yaIncluido && socio.getEstado() == SocioEstado.ACTIVO) {
                    resultados.add(socio);
                }
            });
        } catch (NumberFormatException ignorado) {
            // el termino no es numerico: no aplica busqueda por numero de socio
        }

        return resultados;
    }
    */
    public List<Socio> buscar(String texto, SocioEstado estado) {
        if (texto == null || texto.isBlank()) {
            return socioRepository.findByEstado(estado);
        }

        String termino = texto.trim();
        
        List<Socio> resultados = new ArrayList<>(
                socioRepository.buscarPorTextoYEstado(termino, estado));

        try {
            Long numeroSocio = Long.valueOf(termino);

            socioRepository.findByNumeroSocioAndEstado(numeroSocio, estado)
                    .ifPresent(socio -> {
                        boolean yaIncluido = resultados.stream()
                                .anyMatch(s -> s.getId().equals(socio.getId()));

                        if (!yaIncluido) {
                            resultados.add(socio);
                        }
                    });

        } catch (NumberFormatException ignorado) {
            // El término no es numérico: no aplica búsqueda por número de socio
        }

        return resultados;
    }

    @Transactional
    public void eliminar(Long id) {
        Socio socio = socioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El socio no existe."));

        if (socio.getEstado() != SocioEstado.INACTIVO) {
            throw new IllegalArgumentException(
                    "Solo se pueden eliminar socios inactivos."
            );
        }

        if (cuotaRepository.existsBySocio(socio)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar el socio porque tiene cuotas asociadas."
            );
        }

        socioRepository.delete(socio);
    }
    
    public Optional<Socio> buscarPorId(Long id) {
        return socioRepository.findById(id);
    }

    public long contarActivos() {
        return listarActivos().size();
    }

    private Long siguienteNumeroSocio() {
        return socioRepository.findFirstByOrderByNumeroSocioDesc()
                .map(s -> s.getNumeroSocio() + 1)
                .orElse(1L);
    }

    private void validarDniUnico(String dni, Long idActual) {
        boolean existe = (idActual == null)
                ? socioRepository.existsByDni(dni)
                : socioRepository.existsByDniAndIdNot(dni, idActual);
        if (existe) {
            throw new IllegalArgumentException("Ya existe un socio con ese DNI.");
        }
    }

    private void validarNumeroSocioUnico(Long numeroSocio, Long idActual) {
        boolean existe = (idActual == null)
                ? socioRepository.existsByNumeroSocio(numeroSocio)
                : socioRepository.existsByNumeroSocioAndIdNot(numeroSocio, idActual);
        if (existe) {
            throw new IllegalArgumentException("Ya existe un socio con ese numero de socio.");
        }
    }
}
