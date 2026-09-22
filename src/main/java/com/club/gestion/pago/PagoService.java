package com.club.gestion.pago;

import com.club.gestion.cuota.Cuota;
import com.club.gestion.cuota.CuotaEstado;
import com.club.gestion.cuota.CuotaRepository;
import com.club.gestion.socio.Socio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Logica de negocio relacionada a los pagos.
 *
 * Como no existen pagos parciales, registrar un pago siempre marca la
 * cuota asociada como PAGADA; anular un pago siempre la vuelve a
 * PENDIENTE. La relacion cuota-pago es 1 a 1 (ver {@link Pago}), por lo
 * que no puede existir mas de un pago para la misma cuota: esto se valida
 * aqui y ademas queda garantizado por la restriccion unica de base de
 * datos sobre la columna cuota_id.
 */
@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;

    public PagoService(PagoRepository pagoRepository, CuotaRepository cuotaRepository) {
        this.pagoRepository = pagoRepository;
        this.cuotaRepository = cuotaRepository;
    }

    @Transactional
    public Pago registrarPago(Pago pago) {
        if (pago.getCuota() == null || pago.getCuota().getId() == null) {
            throw new IllegalArgumentException("Debe seleccionar la cuota que se esta abonando.");
        }

        Cuota cuota = cuotaRepository.findById(pago.getCuota().getId())
                .orElseThrow(() -> new IllegalArgumentException("La cuota seleccionada no existe."));

        if (cuota.getEstado() == CuotaEstado.PAGADA || pagoRepository.findByCuotaId(cuota.getId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Esa cuota ya tiene un pago registrado. No se permiten pagos duplicados para la misma cuota.");
        }

        validarFecha(pago.getFechaPago());

        pago.setId(null);
        pago.setCuota(cuota);
        Pago pagoGuardado = pagoRepository.save(pago);

        cuota.setEstado(CuotaEstado.PAGADA);
        cuota.setFechaPago(pago.getFechaPago());
        cuotaRepository.save(cuota);

        return pagoGuardado;
    }

    @Transactional
    public Pago actualizar(Long id, Pago datos) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("El pago no existe."));

        validarFecha(datos.getFechaPago());

        pago.setImporte(datos.getImporte());
        pago.setFechaPago(datos.getFechaPago());
        pago.setMedioPago(datos.getMedioPago());

        Pago pagoActualizado = pagoRepository.save(pago);

        // La cuota guarda su propia fecha de pago (usada para reportes y
        // para mostrarla en /cuotas sin tener que unir con /pagos), asi
        // que se mantiene sincronizada con la fecha real del pago.
        Cuota cuota = pago.getCuota();
        cuota.setFechaPago(pago.getFechaPago());
        cuotaRepository.save(cuota);

        return pagoActualizado;
    }

    @Transactional
    public void anular(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("El pago no existe."));

        Cuota cuota = pago.getCuota();

        pagoRepository.delete(pago);

        cuota.setEstado(CuotaEstado.PENDIENTE);
        cuota.setFechaPago(null);
        cuotaRepository.save(cuota);
    }

    /**
     * Filtro combinado para el listado de pagos. Cualquier criterio en
     * null/vacio se ignora (no se aplica esa condicion).
     */
    public List<Pago> filtrar(Integer anio, Integer mes, LocalDate fecha, String texto) {
        String termino = (texto == null || texto.isBlank()) ? null : texto.trim();
        return pagoRepository.filtrar(anio, mes, fecha, termino);
    }

    public Optional<Pago> buscarPorId(Long id) {
        return pagoRepository.buscarPorIdConDetalle(id);
    }

    /**
     * Historial completo de pagos de un socio, del mas reciente al mas
     * antiguo (para la ficha del socio).
     */
    public List<Pago> listarPorSocio(Socio socio) {
        return pagoRepository.buscarPorSocio(socio);
    }

    /**
     * Arma un mapa cuotaId -> pagoId para el conjunto de cuotas indicado,
     * de forma de poder enlazar, desde /cuotas, a cada pago que cancelo
     * una cuota PAGADA sin resolver un pago a la vez.
     */
    public Map<Long, Long> mapearPagoIdPorCuotaId(List<Long> cuotaIds) {
        Map<Long, Long> resultado = new HashMap<>();
        if (cuotaIds == null || cuotaIds.isEmpty()) {
            return resultado;
        }
        for (Pago pago : pagoRepository.findByCuotaIdIn(cuotaIds)) {
            resultado.put(pago.getCuota().getId(), pago.getId());
        }
        return resultado;
    }

    private void validarFecha(LocalDate fechaPago) {
        if (fechaPago != null && fechaPago.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de pago no puede ser futura.");
        }
    }
}
