package com.club.gestion.socio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    Optional<Socio> findByDni(String dni);

    Optional<Socio> findByNumeroSocio(Long numeroSocio);

    List<Socio> findByEstado(SocioEstado estado);

    // Filtrar por estado y coincidencia de búsqueda (nombre, apellido o dni)
    @Query("""
        SELECT s FROM Socio s
        WHERE (
            LOWER(s.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
            OR LOWER(s.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))
            OR LOWER(s.dni) LIKE LOWER(CONCAT('%', :termino, '%'))
            OR LOWER(s.telefono) LIKE LOWER(CONCAT('%', :termino, '%'))
        )
        AND s.estado = :estado
    """)
    List<Socio> buscarPorTextoYEstado(@Param("termino") String termino, @Param("estado") SocioEstado estado);

    Optional<Socio> findFirstByOrderByNumeroSocioDesc();

    Optional<Socio> findByNumeroSocioAndEstado(Long numeroSocio, SocioEstado estado);
    
    boolean existsByDni(String dni);

    boolean existsByNumeroSocio(Long numeroSocio);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByNumeroSocioAndIdNot(Long numeroSocio, Long id);

    List<Socio> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrDniContainingIgnoreCaseOrTelefonoContainingIgnoreCase(
            String nombre, String apellido, String dni, String telefono);
}
