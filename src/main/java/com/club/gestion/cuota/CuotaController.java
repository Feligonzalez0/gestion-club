package com.club.gestion.cuota;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import com.club.gestion.pago.PagoService;

@Controller
@RequestMapping("/cuotas")
public class CuotaController {

    // Cantidad de cuotas que se muestran por pagina en el listado.
    private static final int TAMANIO_PAGINA = 20;

    private final CuotaService cuotaService;
    private final PagoService pagoService;

    public CuotaController(CuotaService cuotaService, PagoService pagoService) {
        this.cuotaService = cuotaService;
        this.pagoService = pagoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String anio,
                          @RequestParam(required = false) String mes,
                          @RequestParam(required = false) String estado,
                          @RequestParam(required = false) String q,
                          @RequestParam(name = "pagina", defaultValue = "1") int pagina,
                          Model model) {
        Integer anioFiltro = parseEntero(anio);
        Integer mesFiltro = parseEntero(mes);
        CuotaEstado estadoFiltro = (estado == null || estado.isBlank()) ? null : CuotaEstado.valueOf(estado);

        Pageable pageable = PageRequest.of(Math.max(0, pagina - 1), TAMANIO_PAGINA);
        Page<Cuota> paginaCuotas = cuotaService.filtrar(anioFiltro, mesFiltro, estadoFiltro, q, pageable);
        List<Cuota> cuotas = paginaCuotas.getContent();

        List<Long> idsPagadas = cuotas.stream()
                .filter(c -> c.getEstado() == CuotaEstado.PAGADA)
                .map(Cuota::getId)
                .collect(Collectors.toList());
        Map<Long, Long> pagoIdPorCuota = pagoService.mapearPagoIdPorCuotaId(idsPagadas);

        model.addAttribute("cuotas", cuotas);
        model.addAttribute("pagina", paginaCuotas);
        model.addAttribute("pagoIdPorCuota", pagoIdPorCuota);
        model.addAttribute("anio", anioFiltro);
        model.addAttribute("mes", mesFiltro);
        model.addAttribute("estado", estadoFiltro);
        model.addAttribute("q", q);
        model.addAttribute("anioActual", LocalDate.now().getYear());
        model.addAttribute("mesActual", LocalDate.now().getMonthValue());
        cuotaService.actualizarCuotasVencidas();
        return "cuotas/list";
    }

    @PostMapping("/generar")
    public String generar(@RequestParam(required = false) String anio,
                           @RequestParam(required = false) String mes,
                           RedirectAttributes redirectAttributes) {
        Integer anioVal = parseEntero(anio);
        Integer mesVal = parseEntero(mes);

        if (anioVal == null || mesVal == null) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Debe indicar un mes y un anio validos para generar las cuotas.");
            return "redirect:/cuotas";
        }

        if (mesVal < 1 || mesVal > 12) {
            redirectAttributes.addFlashAttribute("mensajeError", "Mes inválido, intente nuevamente.");
            return "redirect:/cuotas";
        }

        int generadas = cuotaService.generarCuotasDelMes(anioVal, mesVal);
        redirectAttributes.addFlashAttribute("mensajeExito",
                generadas + " cuota(s) generada(s) para el periodo " + mesVal + "/" + anioVal + ".");
        return "redirect:/cuotas?anio=" + anioVal + "&mes=" + mesVal;
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        Optional<Cuota> cuotaOpt = cuotaService.buscarPorId(id);

        if (cuotaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Cuota no encontrada.");
            return "redirect:/cuotas";
        }

        Cuota cuota = cuotaOpt.get();

        if (!cuotaService.esModificable(cuota)) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "No se puede editar una cuota PAGADA. Anule primero el pago asociado.");
            return redirectAlPeriodo(cuota);
        }

        model.addAttribute("cuotaForm", CuotaForm.desde(cuota));
        model.addAttribute("cuota", cuota);
        model.addAttribute("cuotaId", id);

        return "cuotas/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("cuotaForm") CuotaForm cuotaForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return volverAlFormulario(id, model, redirectAttributes, null);
        }

        try {
            Cuota actualizada = cuotaService.actualizar(id, cuotaForm.aCuota());

            redirectAttributes.addFlashAttribute("mensajeExito", "Cuota actualizada correctamente.");
            return redirectAlPeriodo(actualizada);

        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "Cuota no encontrada.");
            return "redirect:/cuotas";

        } catch (IllegalArgumentException ex) {
            return volverAlFormulario(id, model, redirectAttributes, ex.getMessage());
        }
    }

    @PostMapping("/{id}/anular")
    public String anular(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        Optional<Cuota> cuotaOpt = cuotaService.buscarPorId(id);

        if (cuotaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Cuota no encontrada.");
            return "redirect:/cuotas";
        }

        Cuota cuota = cuotaOpt.get();

        try {
            cuotaService.anular(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Cuota anulada correctamente.");

        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "Cuota no encontrada.");
            return "redirect:/cuotas";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }

        return redirectAlPeriodo(cuota);
    }

    // Vuelve a mostrar el formulario de edicion (error de validacion o de
    // negocio), recargando la cuota para los datos de solo lectura.
    private String volverAlFormulario(Long id, Model model,
                                      RedirectAttributes redirectAttributes, String errorNegocio) {
        Optional<Cuota> cuotaOpt = cuotaService.buscarPorId(id);

        if (cuotaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Cuota no encontrada.");
            return "redirect:/cuotas";
        }

        model.addAttribute("cuota", cuotaOpt.get());
        model.addAttribute("cuotaId", id);
        if (errorNegocio != null) {
            model.addAttribute("errorNegocio", errorNegocio);
        }
        return "cuotas/form";
    }

    // Despues de editar/anular se vuelve al listado filtrado por el periodo
    // de la cuota, igual que luego de generar las cuotas de un periodo.
    private String redirectAlPeriodo(Cuota cuota) {
        return "redirect:/cuotas?anio=" + cuota.getAnio() + "&mes=" + cuota.getMes();
    }

    private Integer parseEntero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
}
