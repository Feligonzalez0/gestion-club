package com.club.gestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto de Spring Boot levante correctamente, con todas
 * las entidades, repositorios y beans de configuracion cargados.
 */
@SpringBootTest
@ActiveProfiles("test")
class GestionClubApplicationTests {

    @Test
    void contextLoads() {
    }
}
