package com.club.gestion.socio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.club.gestion.cuota.Cuota;
import com.club.gestion.cuota.CuotaRepository;

@ExtendWith(MockitoExtension.class)
class SocioServiceTest {

    @Mock
    private SocioRepository socioRepository;

    private CuotaRepository cuotaRepository;
    @InjectMocks
    private SocioService socioService;

    private Socio socio;

    @BeforeEach
    void setUp() {
        socio = new Socio();
        socio.setId(1L);
        socio.setNumeroSocio(1L);
        socio.setNombre("Juan");
        socio.setApellido("Perez");
        socio.setDni("30111222");
        socio.setFechaAlta(LocalDate.now());
        socio.setEstado(SocioEstado.ACTIVO);
        socio.setCategoria(SocioCategoria.MAYOR);
    }

    // ---------- Crear ----------

    @Test
    void creaSocioCuandoNoExisteDuplicado() {
        when(socioRepository.existsByDni(socio.getDni())).thenReturn(false);
        when(socioRepository.existsByNumeroSocio(socio.getNumeroSocio())).thenReturn(false);
        when(socioRepository.save(socio)).thenReturn(socio);

        Socio creado = socioService.crear(socio);

        assertThat(creado).isEqualTo(socio);
        assertThat(creado.getEstado()).isEqualTo(SocioEstado.ACTIVO);
        verify(socioRepository).save(socio);
    }

    @Test
    void creaSocioAsignaNumeroAutomaticamenteCuandoNoSeProporciona() {
        Socio nuevo = new Socio();
        nuevo.setNombre("Ana");
        nuevo.setApellido("Gomez");
        nuevo.setDni("40222333");
        nuevo.setCategoria(SocioCategoria.MENOR);

        when(socioRepository.findFirstByOrderByNumeroSocioDesc()).thenReturn(Optional.of(socio));
        when(socioRepository.existsByDni(nuevo.getDni())).thenReturn(false);
        when(socioRepository.existsByNumeroSocio(2L)).thenReturn(false);
        when(socioRepository.save(nuevo)).thenReturn(nuevo);

        Socio creado = socioService.crear(nuevo);

        assertThat(creado.getNumeroSocio()).isEqualTo(2L);
        assertThat(creado.getEstado()).isEqualTo(SocioEstado.ACTIVO);
        assertThat(creado.getFechaAlta()).isEqualTo(LocalDate.now());
    }

    @Test
    void rechazaSocioConDniDuplicado() {
        when(socioRepository.existsByDni(socio.getDni())).thenReturn(true);

        assertThatThrownBy(() -> socioService.crear(socio))
                .isInstanceOf(IllegalArgumentException.class);

        verify(socioRepository, never()).save(any());
    }

    @Test
    void rechazaSocioConNumeroSocioDuplicado() {
        when(socioRepository.existsByDni(socio.getDni())).thenReturn(false);
        when(socioRepository.existsByNumeroSocio(socio.getNumeroSocio())).thenReturn(true);

        assertThatThrownBy(() -> socioService.crear(socio))
                .isInstanceOf(IllegalArgumentException.class);

        verify(socioRepository, never()).save(any());
    }

    // ---------- Actualizar ----------

    @Test
    void actualizaCorrectamenteUnSocio() {
        Socio datos = new Socio();
        datos.setNumeroSocio(1L);
        datos.setNombre("Juan Actualizado");
        datos.setApellido("Perez");
        datos.setDni("30111222");
        datos.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        datos.setCategoria(SocioCategoria.MAYOR);

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(socioRepository.existsByDniAndIdNot("30111222", 1L)).thenReturn(false);
        when(socioRepository.existsByNumeroSocioAndIdNot(1L, 1L)).thenReturn(false);
        when(socioRepository.save(socio)).thenReturn(socio);

        Socio actualizado = socioService.actualizar(1L, datos);

        assertThat(actualizado.getNombre()).isEqualTo("Juan Actualizado");
        verify(socioRepository).save(socio);
    }

    @Test
    void permiteMantenerSuPropioDniAlActualizar() {
        Socio datos = new Socio();
        datos.setNumeroSocio(1L);
        datos.setNombre("Juan");
        datos.setApellido("Perez");
        datos.setDni(socio.getDni());
        datos.setCategoria(SocioCategoria.MAYOR);

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(socioRepository.existsByDniAndIdNot(socio.getDni(), 1L)).thenReturn(false);
        when(socioRepository.existsByNumeroSocioAndIdNot(1L, 1L)).thenReturn(false);
        when(socioRepository.save(socio)).thenReturn(socio);

        Socio actualizado = socioService.actualizar(1L, datos);

        assertThat(actualizado.getDni()).isEqualTo(socio.getDni());
    }

    @Test
    void permiteMantenerSuPropioNumeroSocioAlActualizar() {
        Socio datos = new Socio();
        datos.setNumeroSocio(socio.getNumeroSocio());
        datos.setNombre("Juan");
        datos.setApellido("Perez");
        datos.setDni(socio.getDni());
        datos.setCategoria(SocioCategoria.MAYOR);

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(socioRepository.existsByDniAndIdNot(socio.getDni(), 1L)).thenReturn(false);
        when(socioRepository.existsByNumeroSocioAndIdNot(socio.getNumeroSocio(), 1L)).thenReturn(false);
        when(socioRepository.save(socio)).thenReturn(socio);

        Socio actualizado = socioService.actualizar(1L, datos);

        assertThat(actualizado.getNumeroSocio()).isEqualTo(socio.getNumeroSocio());
    }

    @Test
    void rechazaDniDeOtroSocioAlActualizar() {
        Socio datos = new Socio();
        datos.setNumeroSocio(1L);
        datos.setNombre("Juan");
        datos.setApellido("Perez");
        datos.setDni("99888777");
        datos.setCategoria(SocioCategoria.MAYOR);

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(socioRepository.existsByDniAndIdNot("99888777", 1L)).thenReturn(true);

        assertThatThrownBy(() -> socioService.actualizar(1L, datos))
                .isInstanceOf(IllegalArgumentException.class);

        verify(socioRepository, never()).save(any());
    }

    @Test
    void rechazaNumeroSocioDeOtroSocioAlActualizar() {
        Socio datos = new Socio();
        datos.setNumeroSocio(99L);
        datos.setNombre("Juan");
        datos.setApellido("Perez");
        datos.setDni(socio.getDni());
        datos.setCategoria(SocioCategoria.MAYOR);

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(socioRepository.existsByDniAndIdNot(socio.getDni(), 1L)).thenReturn(false);
        when(socioRepository.existsByNumeroSocioAndIdNot(99L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> socioService.actualizar(1L, datos))
                .isInstanceOf(IllegalArgumentException.class);

        verify(socioRepository, never()).save(any());
    }

    // ---------- Estado ----------

    @Test
    void desactivarCambiaEstadoAInactivo() {
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(socioRepository.save(socio)).thenReturn(socio);

        Socio resultado = socioService.desactivar(1L);

        assertThat(resultado.getEstado()).isEqualTo(SocioEstado.INACTIVO);
    }

    @Test
    void activarCambiaEstadoAActivo() {
        socio.setEstado(SocioEstado.INACTIVO);
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(socioRepository.save(socio)).thenReturn(socio);

        Socio resultado = socioService.activar(1L);

        assertThat(resultado.getEstado()).isEqualTo(SocioEstado.ACTIVO);
    }

    // ---------- Busqueda ----------

    @Test
    void buscaPorNombreUtilizandoElRepositorio() {
        when(socioRepository.buscarPorTextoYEstado("Juan", SocioEstado.ACTIVO))
            .thenReturn(List.of(socio));

        List<Socio> resultado = socioService.buscar("Juan", SocioEstado.ACTIVO);

        assertThat(resultado).containsExactly(socio);
    }

    @Test
    void buscaPorNumeroSocioCuandoElTerminoEsNumerico() {
        when(socioRepository.buscarPorTextoYEstado("1", SocioEstado.ACTIVO))
                .thenReturn(List.of());

        when(socioRepository.findByNumeroSocioAndEstado(1L, SocioEstado.ACTIVO))
                .thenReturn(Optional.of(socio));

        List<Socio> resultado = socioService.buscar("1", SocioEstado.ACTIVO);

        assertThat(resultado).containsExactly(socio);
    }

    @Test
    void buscarSinTextoDevuelveTodosLosSocios() {
        when(socioRepository.findByEstado(SocioEstado.ACTIVO))
                .thenReturn(List.of(socio));

        List<Socio> resultado = socioService.buscar(null, SocioEstado.ACTIVO);

        assertThat(resultado).containsExactly(socio);
    }

    @Test
    void eliminarSocioInactivo() {
        Socio socio = new Socio();
        socio.setId(1L);
        socio.setEstado(SocioEstado.INACTIVO);

        when(socioRepository.findById(1L))
                .thenReturn(Optional.of(socio));

        socioService.eliminar(1L);

        verify(socioRepository).delete(socio);
    }

    @Test
    void noPuedeEliminarSocioActivo() {
        Socio socio = new Socio();
        socio.setId(1L);
        socio.setEstado(SocioEstado.ACTIVO);

        when(socioRepository.findById(1L))
                .thenReturn(Optional.of(socio));

        assertThatThrownBy(() -> socioService.eliminar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Solo se pueden eliminar socios inactivos.");

        verify(socioRepository, never()).delete(any());
    }

    @Test
    void eliminarSocioInexistente() {
        when(socioRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> socioService.eliminar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El socio no existe.");

        verify(socioRepository, never()).delete(any());
    }

}
