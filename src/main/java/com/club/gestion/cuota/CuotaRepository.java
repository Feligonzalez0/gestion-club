package com.club.gestion.cuota;

import com.club.gestion.socio.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {

    Optional<Cuota> findBySocioAndAnioAndMes(Socio socio, Integer anio, Integer mes);

    List<Cuota> findByEstado(CuotaEstado estado);

    List<Cuota> findBySocioAndEstado(Socio socio, CuotaEstado estado);

    List<Cuota> findBySocio(Socio socio);
    
    List<Cuota> findBySocioOrderByAnioDescMesDesc(Socio socio);

    boolean existsBySocio(Socio socio);

    // Filtro combinado para el listado de /cuotas: cada criterio es opcional
    // (si el parametro llega null, no se aplica esa condicion).
    @Query("""
        SELECT c FROM Cuota c
        JOIN FETCH c.socio s
        WHERE (:anio IS NULL OR c.anio = :anio)
          AND (:mes IS NULL OR c.mes = :mes)
          AND (:estado IS NULL OR c.estado = :estado)
          AND (
              :termino IS NULL
              OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))
              OR LOWER(s.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))
              OR LOWER(s.dni) LIKE LOWER(CONCAT('%', :termino, '%'))
          )
        ORDER BY c.anio DESC, c.mes DESC, s.apellido ASC, s.nombre ASC
    """)
    List<Cuota> filtrar(@Param("anio") Integer anio,
                         @Param("mes") Integer mes,
                         @Param("estado") CuotaEstado estado,
                         @Param("termino") String termino);

    @Modifying
    @Query("""
        UPDATE Cuota c
        SET c.estado = com.club.gestion.cuota.CuotaEstado.VENCIDA
        WHERE c.estado = com.club.gestion.cuota.CuotaEstado.PENDIENTE
        AND c.fechaVencimiento < :hoy
    """)
    int marcarCuotasVencidas(@Param("hoy") LocalDate hoy);

    @Query("""
        SELECT c
        FROM Cuota c
        JOIN FETCH c.socio
        WHERE c.estado = :estado
    """)
    List<Cuota> findByEstadoConSocio(@Param("estado") CuotaEstado estado);
}