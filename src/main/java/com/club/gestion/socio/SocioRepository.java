package com.club.gestion.socio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Version paginada del mismo filtro (estado + busqueda de texto libre,
    // incluyendo numero de socio) para el listado de /socios y
    // /socios/inactivos. El termino puede llegar null, en cuyo caso no se
    // aplica ese criterio y solo se filtra por estado.
    @Query(
        value = """
            SELECT s FROM Socio s
            WHERE s.estado = :estado
              AND (
                  :termino IS NULL
                  OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.dni) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.telefono) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR CAST(s.numeroSocio AS string) LIKE CONCAT('%', :termino, '%')
              )
            ORDER BY s.apellido ASC, s.nombre ASC
        """,
        countQuery = """
            SELECT COUNT(s) FROM Socio s
            WHERE s.estado = :estado
              AND (
                  :termino IS NULL
                  OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.dni) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.telefono) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR CAST(s.numeroSocio AS string) LIKE CONCAT('%', :termino, '%')
              )
        """
    )
    Page<Socio> buscarPorTextoYEstadoPaginado(@Param("termino") String termino,
                                               @Param("estado") SocioEstado estado,
                                               Pageable pageable);

    Optional<Socio> findFirstByOrderByNumeroSocioDesc();

    Optional<Socio> findByNumeroSocioAndEstado(Long numeroSocio, SocioEstado estado);
    
    boolean existsByDni(String dni);

    boolean existsByNumeroSocio(Long numeroSocio);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByNumeroSocioAndIdNot(Long numeroSocio, Long id);

    List<Socio> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrDniContainingIgnoreCaseOrTelefonoContainingIgnoreCase(
            String nombre, String apellido, String dni, String telefono);
}
