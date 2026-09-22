package com.club.gestion.usuario;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementa {@link UserDetailsService} para dejar conectada la
 * infraestructura de Spring Security con la entidad {@link Usuario}.
 *
 * Por ahora no se habilita un flujo de login (ver SecurityConfig), pero
 * este service ya permite cargar un usuario por username cuando se active
 * la autenticacion en una fase posterior.
 */
@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .disabled(!usuario.isActivo())
                .roles(usuario.getRol().name())
                .build();
    }

    public Usuario crear(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }
}
