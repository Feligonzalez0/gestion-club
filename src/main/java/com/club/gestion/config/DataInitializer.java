package com.club.gestion.config;

import com.club.gestion.usuario.Usuario;
import com.club.gestion.usuario.UsuarioRepository;
import com.club.gestion.usuario.UsuarioRol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario ADMINISTRADOR inicial unicamente si se definen las
 * variables de entorno ADMIN_USERNAME y ADMIN_PASSWORD y todavia no existe
 * ningun usuario cargado en la base. Si no se definen, la aplicacion
 * arranca normalmente sin crear usuarios (no se hardcodean credenciales).
 *
 * Ver README para instrucciones de uso.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRol(UsuarioRol.ADMINISTRADOR);
        admin.setActivo(true);
        usuarioRepository.save(admin);
    }
}
