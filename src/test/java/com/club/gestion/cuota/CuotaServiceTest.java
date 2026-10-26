package com.club.gestion.cuota;

import com.club.gestion.socio.Socio;
import com.club.gestion.socio.SocioEstado;
import com.club.gestion.socio.SocioService;
import com.club.gestion.configuracion.ConfiguracionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaServiceTest {

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private SocioService socioService;

    private CuotaService cuotaService;
    private ConfiguracionService configuracionService;

    private Socio socio;
    private Cuota cuota;

    @BeforeEach
    void setUp() {
        // Construccion manual (en vez de @InjectMocks) porque el constructor
        // mezcla mocks con un BigDecimal/int de configuracion que Mockito no
        // puede resolver automaticamente.
        cuotaService = new CuotaService(cuotaRepository, socioService, configuracionService);

        socio = new Socio();
        socio.setId(1L);
        socio.setEstado(SocioEstado.ACTIVO);

        cuota = new Cuota();
        cuota.setSocio(socio);
        cuota.setAnio(2026);
        cuota.setMes(3);
        cuota.setImporte(5000);
        cuota.setFechaVencimiento(LocalDate.of(2026, 3, 10));
    }

    // ---------- Alta individual ----------

    @Test
    void creaCuotaCuandoNoExistePeriodo() {
        when(cuotaRepository.findBySocioAndAnioAndMes(socio, 2026, 3)).thenReturn(Optional.empty());
        when(cuotaRepository.save(cuota)).thenReturn(cuota);

        Cuota creada = cuotaService.crear(cuota);

        assertThat(creada.getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        verify(cuotaRepository).save(cuota);
    }

    @Test
    void rechazaCuotaDuplicadaParaMismoSocioYPeriodo() {
        when(cuotaRepository.findBySocioAndAnioAndMes(socio, 2026, 3)).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> cuotaService.crear(cuota))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void rechazaCuotaParaSocioInactivo() {
        socio.setEstado(SocioEstado.INACTIVO);

        assertThatThrownBy(() -> cuotaService.crear(cuota))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cuotaRepository, never()).save(any());
        verify(cuotaRepository, never()).findBySocioAndAnioAndMes(any(), any(), any());
    }

    @Test
    void calculaDeudaSumandoCuotasPendientes() {
        Cuota otraCuota = new Cuota();
        otraCuota.setImporte(5000);

        when(cuotaRepository.findBySocioAndEstado(socio, CuotaEstado.PENDIENTE))
                .thenReturn(List.of(cuota, otraCuota));

        Integer deuda = cuotaService.calcularDeuda(socio);

        assertEquals(deuda, 8000);
    }

    // ---------- Generacion mensual ----------

    @Test
    void generaCuotaParaSocioActivoSinCuotaPrevia() {
        when(socioService.listarActivos()).thenReturn(List.of(socio));
        when(cuotaRepository.findBySocioAndAnioAndMes(socio, 2026, 4)).thenReturn(Optional.empty());
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int generadas = cuotaService.generarCuotasDelMes(2026, 4);

        assertThat(generadas).isEqualTo(1);

        ArgumentCaptor<Cuota> captor = ArgumentCaptor.forClass(Cuota.class);
        verify(cuotaRepository).save(captor.capture());
        Cuota guardada = captor.getValue();
        assertThat(guardada.getSocio()).isEqualTo(socio);
        assertThat(guardada.getAnio()).isEqualTo(2026);
        assertThat(guardada.getMes()).isEqualTo(4);
        assertThat(guardada.getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        assertThat(guardada.getImporte()).isEqualTo(5000);
        assertThat(guardada.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    void noGeneraCuotaDuplicadaParaSocioQueYaTienePeriodo() {
        when(socioService.listarActivos()).thenReturn(List.of(socio));
        when(cuotaRepository.findBySocioAndAnioAndMes(socio, 2026, 3)).thenReturn(Optional.of(cuota));

        int generadas = cuotaService.generarCuotasDelMes(2026, 3);

        assertThat(generadas).isZero();
        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void noGeneraCuotasParaSociosInactivosPorqueNoSeListan() {
        // listarActivos() (SocioService) ya excluye a los inactivos: si no
        // aparecen en la lista, el service ni los evalua.
        when(socioService.listarActivos()).thenReturn(List.of());

        int generadas = cuotaService.generarCuotasDelMes(2026, 5);

        assertThat(generadas).isZero();
        verify(cuotaRepository, never()).save(any());
        verify(cuotaRepository, never()).findBySocioAndAnioAndMes(any(), any(), any());
    }

    @Test
    void ajustaVencimientoAlUltimoDiaDelMesSiElDiaConfiguradoNoExiste() {
        CuotaService servicioConDiaAlto = new CuotaService(cuotaRepository, socioService, configuracionService);

        when(socioService.listarActivos()).thenReturn(List.of(socio));
        when(cuotaRepository.findBySocioAndAnioAndMes(socio, 2026, 2)).thenReturn(Optional.empty());
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(invocation -> invocation.getArgument(0));

        servicioConDiaAlto.generarCuotasDelMes(2026, 2);

        ArgumentCaptor<Cuota> captor = ArgumentCaptor.forClass(Cuota.class);
        verify(cuotaRepository).save(captor.capture());
        // Febrero de 2026 (no bisiesto) tiene 28 dias: el vencimiento se
        // ajusta al ultimo dia del mes en vez de fallar con un dia 31 invalido.
        assertThat(captor.getValue().getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    // ---------- Edicion de una cuota individual ----------

    @Test
    void actualizaImporteYVencimientoDeUnaCuotaPendiente() {
        LocalDate nuevoVencimiento = LocalDate.now().plusDays(15);
        cuota.setEstado(CuotaEstado.PENDIENTE);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cuota datos = new Cuota();
        datos.setImporte(7000);
        datos.setFechaVencimiento(nuevoVencimiento);

        Cuota actualizada = cuotaService.actualizar(1L, datos);

        assertThat(actualizada.getImporte()).isEqualTo(7000);
        assertThat(actualizada.getFechaVencimiento()).isEqualTo(nuevoVencimiento);
        assertThat(actualizada.getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        // El periodo y el socio no se tocan.
        assertThat(actualizada.getAnio()).isEqualTo(2026);
        assertThat(actualizada.getMes()).isEqualTo(3);
        assertThat(actualizada.getSocio()).isEqualTo(socio);
    }

    @Test
    void prorrogarElVencimientoDeUnaCuotaVencidaLaVuelveAPendiente() {
        cuota.setEstado(CuotaEstado.VENCIDA);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cuota datos = new Cuota();
        datos.setImporte(5000);
        datos.setFechaVencimiento(LocalDate.now().plusDays(10));

        Cuota actualizada = cuotaService.actualizar(1L, datos);

        assertThat(actualizada.getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
    }

    @Test
    void unVencimientoYaPasadoDejaLaCuotaVencida() {
        cuota.setEstado(CuotaEstado.PENDIENTE);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cuota datos = new Cuota();
        datos.setImporte(5000);
        datos.setFechaVencimiento(LocalDate.now().minusDays(3));

        Cuota actualizada = cuotaService.actualizar(1L, datos);

        assertThat(actualizada.getEstado()).isEqualTo(CuotaEstado.VENCIDA);
    }

    @Test
    void rechazaEditarUnaCuotaPagada() {
        cuota.setEstado(CuotaEstado.PAGADA);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));

        Cuota datos = new Cuota();
        datos.setImporte(7000);
        datos.setFechaVencimiento(LocalDate.now().plusDays(10));

        assertThatThrownBy(() -> cuotaService.actualizar(1L, datos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAGADA");

        verify(cuotaRepository, never()).save(any());
        assertThat(cuota.getImporte()).isEqualTo(5000);
    }

    @Test
    void rechazaEditarConImporteInvalido() {
        cuota.setEstado(CuotaEstado.PENDIENTE);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));

        Cuota datos = new Cuota();
        datos.setImporte(0);
        datos.setFechaVencimiento(LocalDate.now().plusDays(10));

        assertThatThrownBy(() -> cuotaService.actualizar(1L, datos))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cuotaRepository, never()).save(any());
    }

    @Test
    void editarUnaCuotaInexistenteLanzaNoSuchElement() {
        when(cuotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuotaService.actualizar(99L, new Cuota()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---------- Anulacion de una cuota individual ----------

    @Test
    void anulaUnaCuotaPendiente() {
        cuota.setEstado(CuotaEstado.PENDIENTE);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));

        cuotaService.anular(1L);

        verify(cuotaRepository).delete(cuota);
    }

    @Test
    void anulaUnaCuotaVencida() {
        cuota.setEstado(CuotaEstado.VENCIDA);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));

        cuotaService.anular(1L);

        verify(cuotaRepository).delete(cuota);
    }

    @Test
    void rechazaAnularUnaCuotaPagada() {
        cuota.setEstado(CuotaEstado.PAGADA);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> cuotaService.anular(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAGADA");

        verify(cuotaRepository, never()).delete(any());
    }

    @Test
    void anularUnaCuotaInexistenteLanzaNoSuchElement() {
        when(cuotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuotaService.anular(99L))
                .isInstanceOf(NoSuchElementException.class);

        verify(cuotaRepository, never()).delete(any());
    }

    @Test
    void soloLasCuotasNoPagadasSonModificables() {
        cuota.setEstado(CuotaEstado.PENDIENTE);
        assertThat(cuotaService.esModificable(cuota)).isTrue();

        cuota.setEstado(CuotaEstado.VENCIDA);
        assertThat(cuotaService.esModificable(cuota)).isTrue();

        cuota.setEstado(CuotaEstado.PAGADA);
        assertThat(cuotaService.esModificable(cuota)).isFalse();
    }

    // ---------- Filtro del listado ----------

    @Test
    void filtrarDelegaAlRepositorioNormalizandoTextoVacio() {
        when(cuotaRepository.filtrar(2026, 3, CuotaEstado.PENDIENTE, null)).thenReturn(List.of(cuota));

        List<Cuota> resultado = cuotaService.filtrar(2026, 3, CuotaEstado.PENDIENTE, "   ");

        assertThat(resultado).containsExactly(cuota);
    }
}