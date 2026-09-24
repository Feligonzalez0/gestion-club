package com.club.gestion.dashboard;

import com.club.gestion.cuota.CuotaEstado;
import com.club.gestion.cuota.CuotaService;
import com.club.gestion.socio.SocioCategoria;
import com.club.gestion.socio.SocioEstado;
import com.club.gestion.socio.SocioService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

/**
 * Dashboard principal (Etapa 1): panorama general del club para el mes
 * actual, mas socios activos y su distribucion por categoria.
 *
 * Todas las metricas se calculan con consultas agregadas (COUNT/SUM) a
 * nivel de base de datos, sin traer listas completas de socios/cuotas a
 * memoria.
 */
@Controller
public class DashboardController {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-AR");

    private final SocioService socioService;
    private final CuotaService cuotaService;

    public DashboardController(SocioService socioService, CuotaService cuotaService) {
        this.socioService = socioService;
        this.cuotaService = cuotaService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        LocalDate hoy = LocalDate.now();
        int anioActual = hoy.getYear();
        int mesActual = hoy.getMonthValue();

        model.addAttribute("periodoActual", nombrePeriodo(hoy));

        // Socios
        model.addAttribute("cantidadSociosActivos", socioService.contarActivos());

        Map<SocioCategoria, Long> sociosPorCategoria =
                socioService.contarPorCategoria(SocioEstado.ACTIVO);
        model.addAttribute("sociosMayor", sociosPorCategoria.get(SocioCategoria.MAYOR));
        model.addAttribute("sociosMenor", sociosPorCategoria.get(SocioCategoria.MENOR));
        model.addAttribute("sociosJubilado", sociosPorCategoria.get(SocioCategoria.JUBILADO));

        // Cuotas del mes actual, separadas por estado (PENDIENTE y VENCIDA
        // se cuentan por separado: antes se mezclaban en un solo listado).
        long cuotasPendientes = cuotaService.contarPorEstadoEnPeriodo(anioActual, mesActual, CuotaEstado.PENDIENTE);
        long cuotasVencidas = cuotaService.contarPorEstadoEnPeriodo(anioActual, mesActual, CuotaEstado.VENCIDA);
        long cuotasPagadas = cuotaService.contarPorEstadoEnPeriodo(anioActual, mesActual, CuotaEstado.PAGADA);
        long cuotasDelMes = cuotasPendientes + cuotasVencidas + cuotasPagadas;

        model.addAttribute("cuotasDelMes", cuotasDelMes);
        model.addAttribute("cuotasPendientes", cuotasPendientes);
        model.addAttribute("cuotasVencidas", cuotasVencidas);
        model.addAttribute("cuotasPagadas", cuotasPagadas);

        // Recaudacion del mes actual y % de cobranzas (por importe: cuotas
        // pagadas del mes / importe total de cuotas del mes x 100).
        long importeTotalMes = cuotaService.sumarImporteEnPeriodo(anioActual, mesActual);
        long importeCobradoMes = cuotaService.sumarImporteEnPeriodo(anioActual, mesActual, CuotaEstado.PAGADA);
        double porcentajeCobranza = importeTotalMes == 0
                ? 0.0
                : Math.round((importeCobradoMes * 1000.0) / importeTotalMes) / 10.0;

        model.addAttribute("importeCobradoMes", importeCobradoMes);
        model.addAttribute("porcentajeCobranza", porcentajeCobranza);

        return "dashboard";
    }

    private String nombrePeriodo(LocalDate fecha) {
        String mes = fecha.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
        String mesCapitalizado = mes.substring(0, 1).toUpperCase(LOCALE_ES) + mes.substring(1);
        return mesCapitalizado + " " + fecha.getYear();
    }
}