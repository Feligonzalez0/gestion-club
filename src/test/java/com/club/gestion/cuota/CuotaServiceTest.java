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

    // ---------- Filtro del listado ----------

    @Test
    void filtrarDelegaAlRepositorioNormalizandoTextoVacio() {
        when(cuotaRepository.filtrar(2026, 3, CuotaEstado.PENDIENTE, null)).thenReturn(List.of(cuota));

        List<Cuota> resultado = cuotaService.filtrar(2026, 3, CuotaEstado.PENDIENTE, "   ");

        assertThat(resultado).containsExactly(cuota);
    }
}