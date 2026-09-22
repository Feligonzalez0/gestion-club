package com.club.gestion.pago;

import com.club.gestion.cuota.Cuota;
import com.club.gestion.cuota.CuotaService;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * CRUD de pagos. La logica de negocio (validaciones, actualizacion del
 * estado de la cuota asociada) vive en {@link PagoService}; este
 * controller solo recibe requests, delega y arma la respuesta.
 */
@Controller
@RequestMapping("/pagos")
public class PagoController {

    // Cantidad de pagos que se muestran por pagina en el listado.
    private static final int TAMANIO_PAGINA = 20;

    private final PagoService pagoService;
    private final CuotaService cuotaService;

    public PagoController(PagoService pagoService, CuotaService cuotaService) {
        this.pagoService = pagoService;
        this.cuotaService = cuotaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String anio,
                         @RequestParam(required = false) String mes,
                         @RequestParam(required = false) String fecha,
                         @RequestParam(name = "pagina", defaultValue = "1") int pagina,
                         Model model) {

        Integer anioFiltro = parseEntero(anio);
        Integer mesFiltro = parseEntero(mes);
        LocalDate fechaFiltro = parseFecha(fecha);

        Pageable pageable = PageRequest.of(Math.max(0, pagina - 1), TAMANIO_PAGINA);
        Page<Pago> paginaPagos = pagoService.filtrar(anioFiltro, mesFiltro, fechaFiltro, q, pageable);

        model.addAttribute("pagos", paginaPagos.getContent());
        model.addAttribute("pagina", paginaPagos);
        model.addAttribute("q", q);
        model.addAttribute("anio", anioFiltro);
        model.addAttribute("mes", mesFiltro);
        model.addAttribute("fecha", fecha);

        return "pagos/list";
    }

    @GetMapping("/nuevo")
    public String nuevoFormulario(@RequestParam(required = false) Long cuotaId,
                                  Model model) {

        PagoForm form = new PagoForm();
        form.setFechaPago(LocalDate.now());

        List<Cuota> cuotasDisponibles = obtenerCuotasDisponibles();

        if (cuotaId != null) {
            cuotasDisponibles.stream()
                    .filter(c -> c.getId().equals(cuotaId))
                    .findFirst()
                    .ifPresent(cuota -> {
                        form.setCuotaId(cuota.getId());
                        form.setImporte(cuota.getImporte());
                    });
        }

        model.addAttribute("pagoForm", form);
        model.addAttribute("cuotasPendientes", cuotasDisponibles);
        model.addAttribute("esEdicion", false);

        return "pagos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("pagoForm") PagoForm pagoForm,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("cuotasPendientes", obtenerCuotasDisponibles());
            model.addAttribute("esEdicion", false);
            return "pagos/form";
        }

        try {
            pagoService.registrarPago(pagoForm.aPago());

        } catch (IllegalArgumentException ex) {
            model.addAttribute("cuotasPendientes", obtenerCuotasDisponibles());
            model.addAttribute("esEdicion", false);
            model.addAttribute("errorNegocio", ex.getMessage());
            return "pagos/form";

        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("cuotasPendientes", obtenerCuotasDisponibles());
            model.addAttribute("esEdicion", false);
            model.addAttribute("errorNegocio",
                    "Esa cuota ya tiene un pago registrado.");
            return "pagos/form";
        }

        redirectAttributes.addFlashAttribute(
                "mensajeExito",
                "Pago registrado correctamente. La cuota fue marcada como PAGADA."
        );

        return "redirect:/pagos";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        return pagoService.buscarPorId(id)
                .map(pago -> {
                    model.addAttribute("pago", pago);
                    return "pagos/detalle";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute(
                            "mensajeError",
                            "Pago no encontrado."
                    );
                    return "redirect:/pagos";
                });
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        Optional<Pago> pagoOpt = pagoService.buscarPorId(id);

        if (pagoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "Pago no encontrado."
            );
            return "redirect:/pagos";
        }

        Pago pago = pagoOpt.get();

        model.addAttribute("pagoForm", PagoForm.desde(pago));
        model.addAttribute("pago", pago);
        model.addAttribute("esEdicion", true);
        model.addAttribute("pagoId", id);

        return "pagos/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("pagoForm") PagoForm pagoForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            pagoService.buscarPorId(id)
                    .ifPresent(pago -> model.addAttribute("pago", pago));

            model.addAttribute("esEdicion", true);
            model.addAttribute("pagoId", id);

            return "pagos/form";
        }

        try {
            pagoService.actualizar(id, pagoForm.aPago());

        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "Pago no encontrado."
            );
            return "redirect:/pagos";

        } catch (IllegalArgumentException ex) {
            pagoService.buscarPorId(id)
                    .ifPresent(pago -> model.addAttribute("pago", pago));

            model.addAttribute("esEdicion", true);
            model.addAttribute("pagoId", id);
            model.addAttribute("errorNegocio", ex.getMessage());

            return "pagos/form";
        }

        redirectAttributes.addFlashAttribute(
                "mensajeExito",
                "Pago actualizado correctamente."
        );

        return "redirect:/pagos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id,
                           RedirectAttributes redirectAttributes) {

        try {
            pagoService.anular(id);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Pago anulado correctamente. La cuota vuelve a estado PENDIENTE."
            );

        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "Pago no encontrado."
            );
        }

        return "redirect:/pagos";
    }

    /**
     * Obtiene todas las cuotas que pueden recibir un pago:
     * cuotas pendientes + cuotas vencidas.
     */
    private List<Cuota> obtenerCuotasDisponibles() {
        List<Cuota> cuotas = cuotaService.listarPendientes();
        cuotas.addAll(cuotaService.listarVencidas());
        return cuotas;
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

    private LocalDate parseFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}