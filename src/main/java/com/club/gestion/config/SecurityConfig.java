package com.club.gestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion base de Spring Security.
 *
 * En esta etapa inicial se permite el acceso libre a todas las rutas para
 * poder verificar que la aplicacion funciona de punta a punta sin login.
 * La infraestructura (entidad Usuario, UsuarioService como
 * UserDetailsService, PasswordEncoder) ya queda preparada para habilitar
 * el login y la autorizacion por rol en la Fase 5 del roadmap, sin
 * necesidad de reescribir esta clase desde cero.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
