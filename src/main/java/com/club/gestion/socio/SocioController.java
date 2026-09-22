package com.club.gestion.socio;

import com.club.gestion.cuota.Cuota;
import com.club.gestion.cuota.CuotaEstado;
import com.club.gestion.cuota.CuotaService;
import com.club.gestion.pago.Pago;
import com.club.gestion.pago.PagoService;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * CRUD de socios. La logica de negocio (unicidad, alta/baja logica,
 * asignacion de numero de socio, busqueda) vive en {@link SocioService};
 * este controller solo recibe requests, delega y arma la respuesta.
 */
@Controller
@RequestMapping("/socios")
public class SocioController {

    private final SocioService socioService;
    private final CuotaService cuotaService;
    private final PagoService pagoService;

    public SocioController(SocioService socioService, CuotaService cuotaService, PagoService pagoService) {
        this.socioService = socioService;
        this.cuotaService = cuotaService;
        this.pagoService = pagoService;
    }

    @GetMapping
    public String listar(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("socios", socioService.buscar(q, SocioEstado.ACTIVO));
        model.addAttribute("q", q);
        return "socios/list";
    }

    @GetMapping("/inactivos")
    public String listarInactivos(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("socios", socioService.buscar(q, SocioEstado.INACTIVO));
        model.addAttribute("q", q);
        return "socios/inactivos";
    }

    @GetMapping("/nuevo")
    public String nuevoFormulario(Model model) {
        model.addAttribute("socioForm", new SocioForm());
        model.addAttribute("esEdicion", false);
        return "socios/form";
    }

    @GetMapping("/{id}")
    public String ficha(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return socioService.buscarPorId(id)
                .map(socio -> {
                    List<Cuota> historialCuotas = cuotaService.listarPorSocio(socio);
                    List<Pago> historialPagos = pagoService.listarPorSocio(socio);

                    int anioActual = LocalDate.now().getYear();
                    int mesActual = LocalDate.now().getMonthValue();

                    Cuota cuotaActual = historialCuotas.stream()
                            .filter(c -> c.getAnio() == anioActual && c.getMes() == mesActual)
                            .findFirst()
                            .orElse(historialCuotas.isEmpty() ? null : historialCuotas.get(0));

                    long cuotasPendientes = historialCuotas.stream()
                            .filter(c -> c.getEstado() == CuotaEstado.PENDIENTE)
                            .count();

                    long cuotasPagadasEsteAnio = historialCuotas.stream()
                            .filter(c -> c.getEstado() == CuotaEstado.PAGADA && c.getAnio() == anioActual)
                            .count();

                    int totalAbonadoEsteAnio = historialPagos.stream()
                            .filter(p -> p.getFechaPago() != null && p.getFechaPago().getYear() == anioActual)
                            .mapToInt(Pago::getImporte)
                            .sum();

                    List<Long> idsCuotasPagadas = historialCuotas.stream()
                            .filter(c -> c.getEstado() == CuotaEstado.PAGADA)
                            .map(Cuota::getId)
                            .collect(Collectors.toList());
                    Map<Long, Long> pagoIdPorCuota = pagoService.mapearPagoIdPorCuotaId(idsCuotasPagadas);

                    model.addAttribute("socio", socio);
                    model.addAttribute("cuotaActual", cuotaActual);
                    model.addAttribute("cuotasPendientes", cuotasPendientes);
                    model.addAttribute("cuotasPagadasEsteAnio", cuotasPagadasEsteAnio);
                    model.addAttribute("totalAbonadoEsteAnio", totalAbonadoEsteAnio);
                    model.addAttribute("historialCuotas", historialCuotas);
                    model.addAttribute("historialPagos", historialPagos);
                    model.addAttribute("pagoIdPorCuota", pagoIdPorCuota);
                    model.addAttribute("anioActual", anioActual);
                    model.addAttribute("mesActual", mesActual);
                    return "socios/detalle";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("mensajeError", "Socio no encontrado.");
                    return "redirect:/socios";
                });
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("socioForm") SocioForm socioForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("esEdicion", false);
            return "socios/form";
        }

        try {
            Socio nuevo = new Socio();
            socioForm.copiarA(nuevo);
            socioService.crear(nuevo);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("esEdicion", false);
            model.addAttribute("errorNegocio", ex.getMessage());
            return "socios/form";
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Socio creado correctamente.");
        return "redirect:/socios";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return socioService.buscarPorId(id)
                .map(socio -> {
                    model.addAttribute("socioForm", SocioForm.desde(socio));
                    model.addAttribute("esEdicion", true);
                    model.addAttribute("socioId", id);
                    return "socios/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("mensajeError", "Socio no encontrado.");
                    return "redirect:/socios";
                });
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                              @Valid @ModelAttribute("socioForm") SocioForm socioForm,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("esEdicion", true);
            model.addAttribute("socioId", id);
            return "socios/form";
        }

        try {
            Socio datos = new Socio();
            socioForm.copiarA(datos);
            socioService.actualizar(id, datos);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("esEdicion", true);
            model.addAttribute("socioId", id);
            model.addAttribute("errorNegocio", ex.getMessage());
            return "socios/form";
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "Socio no encontrado.");
            return "redirect:/socios";
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Socio actualizado correctamente.");
        return "redirect:/socios";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            socioService.desactivar(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Socio desactivado correctamente.");
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "Socio no encontrado.");
        }
        return "redirect:/socios";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            socioService.activar(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Socio activado correctamente.");
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "Socio no encontrado.");
        }
        return "redirect:/socios/inactivos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            socioService.eliminar(id);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Socio eliminado definitivamente.");

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    ex.getMessage());

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se puede eliminar el socio porque tiene cuotas o pagos asociados.");
        }

        return "redirect:/socios/inactivos";
    }
}
