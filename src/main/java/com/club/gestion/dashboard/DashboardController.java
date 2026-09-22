package com.club.gestion.dashboard;

import com.club.gestion.cuota.Cuota;
import com.club.gestion.cuota.CuotaService;
import com.club.gestion.socio.SocioService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final SocioService socioService;
    private final CuotaService cuotaService;

    public DashboardController(SocioService socioService, CuotaService cuotaService) {
        this.socioService = socioService;
        this.cuotaService = cuotaService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("cantidadSociosActivos", socioService.contarActivos());
        model.addAttribute("cantidadCuotasPendientes", obtenerCuotasDisponibles());
        return "dashboard";
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
}
