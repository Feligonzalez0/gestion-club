package com.club.gestion.cuota;

import com.club.gestion.socio.Socio;
import com.club.gestion.socio.SocioEstado;
import com.club.gestion.socio.SocioService;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.club.gestion.configuracion.Configuracion;
import com.club.gestion.configuracion.ConfiguracionService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Logica de negocio relacionada a las cuotas.
 *
 * La generacion automatica mensual y el flujo completo de pagos se
 * implementan en una fase posterior (ver README). Por ahora se deja
 * preparada el alta individual con la validacion de periodo unico por
 * socio y las consultas basicas necesarias para el dashboard.
 */
@Service
public class CuotaService {

    private final CuotaRepository cuotaRepository;
    private final SocioService socioService;
    private final ConfiguracionService configuracionService;

    public CuotaService(
            CuotaRepository cuotaRepository,
            SocioService socioService,
            ConfiguracionService configuracionService) {

        this.cuotaRepository = cuotaRepository;
        this.socioService = socioService;
        this.configuracionService = configuracionService;
    }

    public Cuota crear(Cuota cuota) {
        if (cuota.getSocio() == null || cuota.getSocio().getEstado() != SocioEstado.ACTIVO) {
            throw new IllegalArgumentException("Solo se pueden generar cuotas para socios activos.");
        }

        boolean existePeriodo = cuotaRepository
                .findBySocioAndAnioAndMes(cuota.getSocio(), cuota.getAnio(), cuota.getMes())
                .isPresent();
        if (existePeriodo) {
            throw new IllegalArgumentException(
                    "Ya existe una cuota para ese socio en el periodo indicado");
        }
        if (cuota.getEstado() == null) {
            cuota.setEstado(CuotaEstado.PENDIENTE);
        }
        return cuotaRepository.save(cuota);
    }

    /**
     * Genera una cuota PENDIENTE para cada socio ACTIVO que todavia no
     * tenga una cuota cargada para el periodo indicado. Los socios
     * INACTIVOS se ignoran por completo (ni se listan ni reciben cuota).
     * Devuelve la cantidad de cuotas efectivamente generadas.
     */
    @Transactional
    public int generarCuotasDelMes(int anio, int mes) {

        Configuracion configuracion = configuracionService.obtener();

        List<Socio> sociosActivos = socioService.listarActivos();

        LocalDate vencimiento = calcularFechaVencimiento(
                anio,
                mes,
                configuracion.getDiaVencimiento());

        int generadas = 0;

        for (Socio socio : sociosActivos) {

            boolean yaExiste = cuotaRepository
                    .findBySocioAndAnioAndMes(
                            socio,
                            anio,
                            mes)
                    .isPresent();

            if (yaExiste) {
                continue;
            }

            Cuota cuota = new Cuota();

            cuota.setSocio(socio);
            cuota.setAnio(anio);
            cuota.setMes(mes);

            cuota.setImporte(
                    obtenerImporteSegunCategoria(socio, configuracion));

            cuota.setFechaVencimiento(vencimiento);
            cuota.setEstado(CuotaEstado.PENDIENTE);

            cuotaRepository.save(cuota);
            generadas++;
        }

        return generadas;
    }

    /**
     * Filtro combinado para el listado. Cualquier criterio en null se
     * ignora (no se aplica esa condicion).
     */
    public List<Cuota> filtrar(Integer anio, Integer mes, CuotaEstado estado, String texto) {
        String termino = (texto == null || texto.isBlank()) ? null : texto.trim();
        return cuotaRepository.filtrar(anio, mes, estado, termino);
    }

    /**
     * Historial completo de cuotas de un socio, ordenado del periodo mas
     * reciente al mas antiguo (para la ficha del socio).
     */
    public List<Cuota> listarPorSocio(Socio socio) {
        return cuotaRepository.findBySocioOrderByAnioDescMesDesc(socio);
    }

    public List<Cuota> listarPendientes() {
        return cuotaRepository.findByEstadoConSocio(CuotaEstado.PENDIENTE);
    }

    public List<Cuota> listarPagadas() {
        return cuotaRepository.findByEstado(CuotaEstado.PAGADA);
    }

    public List<Cuota> listarVencidas() {
        return cuotaRepository.findByEstadoConSocio(CuotaEstado.VENCIDA);
    }

    public Integer calcularDeuda(Socio socio) {
        return cuotaRepository.findBySocioAndEstado(socio, CuotaEstado.PENDIENTE).stream()
                .mapToInt(Cuota::getImporte)
                .sum();
    }

    private LocalDate calcularFechaVencimiento(
            int anio,
            int mes,
            int diaVencimiento) {

        YearMonth periodo = YearMonth.of(anio, mes);

        int dia = Math.min(
                diaVencimiento,
                periodo.lengthOfMonth());

        return LocalDate.of(anio, mes, dia);
    }
    
    private Integer obtenerImporteSegunCategoria(
            Socio socio,
            Configuracion configuracion) {

        return switch (socio.getCategoria()) {
            case MAYOR -> configuracion.getImporteCuotaMayor();
            case MENOR -> configuracion.getImporteCuotaMenor();
            case JUBILADO -> configuracion.getImporteCuotaJubilado();
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void actualizarCuotasVencidasAlIniciar() {
        cuotaRepository.marcarCuotasVencidas(LocalDate.now());
    }

    @Transactional
    public int actualizarCuotasVencidas() {
        return cuotaRepository.marcarCuotasVencidas(LocalDate.now());
    }
}
