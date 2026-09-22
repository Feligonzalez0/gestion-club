# Gestion Club — Sistema de gestión de socios

## 1. Qué es el proyecto

Base inicial de una aplicación web para que el personal administrativo de un
club amateur de fútbol gestione socios, cuotas y pagos. Esta entrega **no**
implementa el MVP completo: deja un proyecto compilable, ejecutable y con
una arquitectura simple por capas, lista para ir sumando funcionalidades.

## 2. Tecnologías utilizadas

- Java 21
- Spring Boot 3.3 (Spring MVC, Spring Data JPA, Spring Security, Validation)
- Hibernate
- PostgreSQL
- Thymeleaf + Bootstrap 5 (vía CDN)
- Maven
- JUnit 5, Mockito, Spring Boot Test

No se usa ningún framework de frontend (React/Angular/Vue) ni JavaScript
adicional: la interfaz es server-side, renderizada con Thymeleaf.

## 3. Cómo configurar PostgreSQL

1. Crear una base de datos vacía, por ejemplo:

   ```sql
   CREATE DATABASE gestion_club;
   ```

2. Definir las variables de entorno de conexión (ver sección siguiente).
   Con `spring.jpa.hibernate.ddl-auto=update` (valor por defecto), Hibernate
   crea/actualiza automáticamente las tablas al arrancar la aplicación por
   primera vez — no hace falta correr scripts SQL manualmente.

## 4. Variables de entorno necesarias

| Variable         | Descripción                                   | Valor por defecto                              |
|-------------------|-----------------------------------------------|-------------------------------------------------|
| `DB_URL`          | URL JDBC de PostgreSQL                        | `jdbc:postgresql://localhost:5432/gestion_club`  |
| `DB_USERNAME`     | Usuario de la base de datos                   | `postgres`                                       |
| `DB_PASSWORD`     | Contraseña de la base de datos                | `postgres`                                       |
| `DDL_AUTO`        | Estrategia de esquema de Hibernate            | `update`                                         |
| `SHOW_SQL`        | Loguear las sentencias SQL generadas          | `false`                                          |
| `ADMIN_USERNAME`  | (Opcional) usuario admin a crear al arrancar  | *(vacío, no se crea usuario)*                    |
| `ADMIN_PASSWORD`  | (Opcional) contraseña del usuario admin       | *(vacío, no se crea usuario)*                    |

No hay credenciales hardcodeadas en el código: los valores por defecto son
solo para desarrollo local y deben sobreescribirse con variables de entorno
reales en cualquier otro ambiente.

Ejemplo para levantar la app localmente (bash):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/gestion_club
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

## 5. Cómo ejecutar el proyecto con Maven

```bash
mvn spring-boot:run
```

o, generando el jar y ejecutándolo:

```bash
mvn clean package
java -jar target/gestion-club-0.0.1-SNAPSHOT.jar
```

La aplicación queda disponible en `http://localhost:8080`.

Rutas disponibles en esta entrega:

- `GET /` y `GET /dashboard` → panel inicial con indicadores mínimos.
- `GET /socios` → listado de socios (vacío si no hay datos cargados).
- `GET /cuotas` → pantalla inicial del módulo de cuotas.
- `GET /pagos` → pantalla inicial del módulo de pagos.

## 6. Cómo ejecutar los tests

```bash
mvn test
```

Los tests corren contra una base H2 en memoria (perfil `test`), por lo que
no requieren tener PostgreSQL levantado.

## 7. Estructura general del proyecto

```
src/main/java/com/club/gestion/
├── GestionClubApplication.java
├── socio/       → Socio, SocioEstado, SocioCategoria, SocioRepository, SocioService, SocioController
├── cuota/       → Cuota, CuotaEstado, CuotaRepository, CuotaService, CuotaController
├── pago/        → Pago, PagoMedio, PagoRepository, PagoService, PagoController
├── usuario/     → Usuario, UsuarioRol, UsuarioRepository, UsuarioService
├── dashboard/   → DashboardController (rutas "/" y "/dashboard")
└── config/      → SecurityConfig, DataInitializer

src/main/resources/
├── application.properties
├── templates/   → dashboard.html, socios/list.html, cuotas/list.html, pagos/list.html, fragments/nav.html
└── static/css/styles.css

src/test/java/com/club/gestion/
├── GestionClubApplicationTests.java   → test de contexto
├── socio/SocioServiceTest.java        → tests unitarios con Mockito
└── cuota/CuotaServiceTest.java        → tests unitarios con Mockito
```

Arquitectura por capas simple, sin interfaces `I*` ni `*Impl` innecesarias:

```
Controller → Service → Repository → Entity → PostgreSQL
```

La lógica de negocio vive en los `Service`; los `Controller` solo reciben
requests, delegan al service y devuelven la vista.

## 8. Qué funcionalidades están implementadas actualmente

- Proyecto Spring Boot funcional (compila y arranca).
- Conexión a PostgreSQL configurada vía variables de entorno.
- Entidades `Socio`, `Cuota`, `Pago` y `Usuario`, con las restricciones
  descritas en la consigna (unicidad de DNI/número de socio, unicidad de
  cuota por socio+período, relaciones cuota↔pago, etc.).
- Repositorios JPA básicos para cada entidad.
- Services con la lógica mínima necesaria para ser testeada:
  - `SocioService.crear` valida DNI y número de socio únicos.
  - `CuotaService.crear` valida que no exista otra cuota para el mismo
    socio y período, y calcula la deuda de un socio.
  - `PagoService.registrarPago` registra el pago y marca la cuota como
    `PAGADA` (sin pagos parciales).
- Rutas GET (`/`, `/dashboard`, `/socios`, `/cuotas`, `/pagos`) con vistas
  Thymeleaf mínimas que no producen errores.
- Infraestructura de Spring Security preparada (`UsuarioService` como
  `UserDetailsService`, `PasswordEncoder`), con todas las rutas abiertas por
  ahora (no hay login todavía).
- Tests: contexto de Spring Boot + tests unitarios de `SocioService` y
  `CuotaService` con Mockito.

## 9. Qué funcionalidades quedan pendientes

Ver el roadmap a continuación. En resumen: pantallas y endpoints de generación mensual de cuotas, flujo completo
de registro de pagos, login y autorización por rol, dashboard con todos los
indicadores, reportes y exportación CSV/Excel, y pantalla de configuración.

## Roadmap

### Fase 1 — Base

- [x] Proyecto Spring Boot
- [x] PostgreSQL
- [x] JPA
- [x] Entidades principales
- [x] Estructura por módulos
- [x] Thymeleaf
- [x] Tests básicos

### Fase 2 — Socios

- [x] Alta
- [x] Modificación
- [x] Inactivación
- [x] Búsqueda
- [ ] Ficha del socio

### Fase 3 — Cuotas

- [ ] Configuración de cuotas
- [ ] Generación mensual
- [ ] Consulta de cuotas

### Fase 4 — Pagos

- [ ] Registrar pago
- [ ] Actualizar cuota
- [ ] Historial
- [ ] Comprobante

### Fase 5 — Administración

- [ ] Login
- [ ] Roles
- [ ] Dashboard
- [ ] Reportes
- [ ] Exportación