package com.club.gestion.cuota;

import com.club.gestion.socio.Socio;
import com.club.gestion.socio.SocioEstado;
import com.club.gestion.socio.SocioService;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.club.gestion.configuracion.Configuracion;
import com.club.gestion.configuracion.ConfiguracionService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

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
     * Importe sugerido de la cuota de cada socio segun su categoria, tomado
     * de la configuracion vigente (mismo criterio que la generacion mensual).
     * Se usa para precargar el importe al crear una cuota individual.
     * Devuelve un mapa idSocio -> importe.
     */
    public Map<Long, Integer> importesSugeridos(List<Socio> socios) {
        Configuracion configuracion = configuracionService.obtener();

        Map<Long, Integer> importes = new HashMap<>();
        for (Socio socio : socios) {
            importes.put(socio.getId(), obtenerImporteSegunCategoria(socio, configuracion));
        }
        return importes;
    }

    /**
     * Busca una cuota junto con su socio (para el formulario de edicion).
     */
    public Optional<Cuota> buscarPorId(Long id) {
        return cuotaRepository.buscarPorIdConSocio(id);
    }

    /**
     * Una cuota solo se puede editar o anular mientras no este PAGADA
     * (es decir, si esta PENDIENTE o VENCIDA). Si ya tiene un pago, primero
     * hay que anular ese pago, lo que la devuelve a PENDIENTE.
     */
    public boolean esModificable(Cuota cuota) {
        return cuota.getEstado() != CuotaEstado.PAGADA;
    }

    /**
     * Corrige el importe y/o el vencimiento de una cuota individual ya
     * generada. El periodo y el socio no se pueden cambiar.
     *
     * El estado se mantiene coherente con el nuevo vencimiento, con la misma
     * regla que aplica {@link #actualizarCuotasVencidas()}: una cuota cuyo
     * vencimiento ya paso queda VENCIDA; si no, PENDIENTE (por ejemplo, al
     * prorrogar el vencimiento de una cuota vencida vuelve a PENDIENTE).
     */
    @Transactional
    public Cuota actualizar(Long id, Cuota datos) {
        Cuota cuota = cuotaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("La cuota no existe."));

        verificarModificable(cuota, "editar");

        if (datos.getImporte() == null || datos.getImporte() <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor a cero.");
        }
        if (datos.getFechaVencimiento() == null) {
            throw new IllegalArgumentException("El vencimiento es obligatorio.");
        }

        cuota.setImporte(datos.getImporte());
        cuota.setFechaVencimiento(datos.getFechaVencimiento());
        cuota.setEstado(datos.getFechaVencimiento().isBefore(LocalDate.now())
                ? CuotaEstado.VENCIDA
                : CuotaEstado.PENDIENTE);

        return cuotaRepository.save(cuota);
    }

    /**
     * Da de baja una cuota individual (por ejemplo, una generada por error).
     * Solo se permite si no esta PAGADA. Se elimina el registro: si se vuelven
     * a generar las cuotas de ese periodo, se creara de nuevo para el socio.
     */
    @Transactional
    public void anular(Long id) {
        Cuota cuota = cuotaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("La cuota no existe."));

        verificarModificable(cuota, "anular");

        cuotaRepository.delete(cuota);
    }

    private void verificarModificable(Cuota cuota, String accion) {
        if (!esModificable(cuota)) {
            throw new IllegalArgumentException(
                    "No se puede " + accion + " una cuota PAGADA. Anule primero el pago asociado.");
        }
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
     * Version paginada de {@link #filtrar(Integer, Integer, CuotaEstado, String)},
     * usada por el listado de /cuotas para no traer todas las cuotas a
     * memoria cuando hay muchos registros.
     */
    public Page<Cuota> filtrar(Integer anio, Integer mes, CuotaEstado estado, String texto, Pageable pageable) {
        String termino = (texto == null || texto.isBlank()) ? null : texto.trim();
        return cuotaRepository.filtrar(anio, mes, estado, termino, pageable);
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

    /**
     * Cantidad de cuotas de un periodo (anio/mes) en un estado determinado.
     * Resuelto como COUNT en la base (sin traer las cuotas a memoria);
     * usado por el dashboard para distinguir PENDIENTES de VENCIDAS y
     * PAGADAS del mes actual.
     */
    public long contarPorEstadoEnPeriodo(int anio, int mes, CuotaEstado estado) {
        return cuotaRepository.countByAnioAndMesAndEstado(anio, mes, estado);
    }

    /**
     * Importe total de las cuotas de un periodo, sin filtrar por estado.
     */
    public long sumarImporteEnPeriodo(int anio, int mes) {
        Long total = cuotaRepository.sumarImportePorPeriodo(anio, mes);
        return total != null ? total : 0L;
    }

    /**
     * Importe de las cuotas de un periodo en un estado determinado (p. ej.
     * lo efectivamente cobrado en el mes, sumando las PAGADAS).
     */
    public long sumarImporteEnPeriodo(int anio, int mes, CuotaEstado estado) {
        Long total = cuotaRepository.sumarImportePorPeriodoYEstado(anio, mes, estado);
        return total != null ? total : 0L;
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
