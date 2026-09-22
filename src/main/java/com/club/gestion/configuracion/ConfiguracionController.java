package com.club.gestion.configuracion;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(
            ConfiguracionService configuracionService) {

        this.configuracionService = configuracionService;
    }

    @GetMapping
    public String ver(Model model) {
        model.addAttribute(
                "configuracion",
                configuracionService.obtener());

        return "configuracion/form";
    }

    @PostMapping
    public String actualizar(
            @RequestParam Integer importeCuotaMayor,
            @RequestParam Integer importeCuotaMenor,
            @RequestParam Integer importeCuotaJubilado,
            @RequestParam Integer diaVencimiento,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            configuracionService.actualizar(
                    importeCuotaMayor,
                    importeCuotaMenor,
                    importeCuotaJubilado,
                    diaVencimiento);

        } catch (IllegalArgumentException ex) {

            Configuracion configuracion = configuracionService.obtener();

            configuracion.setImporteCuotaMayor(importeCuotaMayor);
            configuracion.setImporteCuotaMenor(importeCuotaMenor);
            configuracion.setImporteCuotaJubilado(importeCuotaJubilado);
            configuracion.setDiaVencimiento(diaVencimiento);

            model.addAttribute("configuracion", configuracion);
            model.addAttribute("errorNegocio", ex.getMessage());

            return "configuracion/form";
        }

        redirectAttributes.addFlashAttribute(
                "mensajeExito",
                "Configuración actualizada correctamente.");

        return "redirect:/configuracion";
    }
}