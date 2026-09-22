package com.club.gestion.cuota;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

        int generadas = cuotaService.generarCuotasDelMes(anioVal, mesVal);
        redirectAttributes.addFlashAttribute("mensajeExito",
                generadas + " cuota(s) generada(s) para el periodo " + mesVal + "/" + anioVal + ".");
        return "redirect:/cuotas?anio=" + anioVal + "&mes=" + mesVal;
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
