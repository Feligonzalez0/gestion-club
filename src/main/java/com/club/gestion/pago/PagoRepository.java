package com.club.gestion.pago;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.club.gestion.socio.Socio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByCuotaId(Long cuotaId);

    List<Pago> findByCuotaIdIn(List<Long> cuotaIds);

    // Trae el pago junto con su cuota y el socio de esa cuota en una sola
    // consulta, para poder mostrarlos en el detalle/edicion sin depender de
    // carga perezosa (open-in-view esta deshabilitado).
    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.cuota c
        JOIN FETCH c.socio s
        WHERE p.id = :id
    """)
    Optional<Pago> buscarPorIdConDetalle(@Param("id") Long id);

    // Filtro combinado para el listado de /pagos: cada criterio es opcional
    // (si el parametro llega null, no se aplica esa condicion).
    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.cuota c
        JOIN FETCH c.socio s
        WHERE (:anio IS NULL OR c.anio = :anio)
          AND (:mes IS NULL OR c.mes = :mes)
          AND (:fecha IS NULL OR p.fechaPago = :fecha)
          AND (
              :termino IS NULL
              OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
              OR LOWER(s.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))
              OR LOWER(s.dni) LIKE LOWER(CONCAT('%', :termino, '%'))
          )
        ORDER BY p.fechaPago DESC, p.id DESC
    """)
    List<Pago> filtrar(@Param("anio") Integer anio,
                        @Param("mes") Integer mes,
                        @Param("fecha") LocalDate fecha,
                        @Param("termino") String termino);

    // Version paginada del mismo filtro, usada por el listado de /pagos.
    // Se define un countQuery explicito (sin JOIN FETCH) para que el
    // conteo de paginas no dependa de la derivacion automatica de Spring
    // Data a partir de una consulta con fetch join.
    @Query(
        value = """
            SELECT p FROM Pago p
            JOIN FETCH p.cuota c
            JOIN FETCH c.socio s
            WHERE (:anio IS NULL OR c.anio = :anio)
              AND (:mes IS NULL OR c.mes = :mes)
              AND (:fecha IS NULL OR p.fechaPago = :fecha)
              AND (
                  :termino IS NULL
                  OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.dni) LIKE LOWER(CONCAT('%', :termino, '%'))
              )
            ORDER BY p.fechaPago DESC, p.id DESC
        """,
        countQuery = """
            SELECT COUNT(p) FROM Pago p
            JOIN p.cuota c
            JOIN c.socio s
            WHERE (:anio IS NULL OR c.anio = :anio)
              AND (:mes IS NULL OR c.mes = :mes)
              AND (:fecha IS NULL OR p.fechaPago = :fecha)
              AND (
                  :termino IS NULL
                  OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))
                  OR LOWER(s.dni) LIKE LOWER(CONCAT('%', :termino, '%'))
              )
        """
    )
    Page<Pago> filtrar(@Param("anio") Integer anio,
                        @Param("mes") Integer mes,
                        @Param("fecha") LocalDate fecha,
                        @Param("termino") String termino,
                        Pageable pageable);

    // Trae los pagos de un socio junto con su cuota, del mas reciente al
    // mas antiguo (para la ficha del socio).
    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.cuota c
        WHERE c.socio = :socio
        ORDER BY p.fechaPago DESC, p.id DESC
    """)
    List<Pago> buscarPorSocio(@Param("socio") Socio socio);

}
