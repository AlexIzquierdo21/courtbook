# CourtBook

API REST para reservar pistas deportivas (pádel y tenis) en clubes. Proyecto de aprendizaje con **arquitectura hexagonal**, nivel intermedio.

> Este documento y los de `docs/` son el encargo. Tú implementas; yo reviso. Si algo de la especificación te parece mal, discútelo antes de picar, no después.

## Qué es

Un usuario consulta la disponibilidad de una pista para un día, ve el precio de cada franja (punta o valle) y reserva. Puede cancelar; si cancela con menos de 24 h de antelación, se le aplica una penalización. El sistema garantiza que **nunca** haya dos reservas confirmadas en la misma pista y franja, incluso con peticiones concurrentes.

## Stack

| Pieza | Elección |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.2 (Web MVC, Data JPA, Validation) |
| Build | Maven (wrapper `./mvnw`) |
| Base de datos | MySQL 8.4 en `compose.yaml` |
| Migraciones | Flyway |
| Tests | JUnit 5, MockMvc, Testcontainers (MySQL) |
| Arranque local | Spring Boot Docker Compose Support |

Paquete base: `com.alexizquierdo.courtbook`.

## Alcance (fase 1)

- Registro de usuarios con contraseña (hash BCrypt). Todos se registran como `PLAYER`.
- Invitaciones de `ADMIN`: un admin invita por email con un token de un solo uso que caduca a las 72 h.
- Creación del primer `ADMIN` al arrancar, a partir de la configuración.
- Alta (solo `ADMIN`) y consulta de clubes, con su horario y sus ventanas de hora punta.
- Alta (solo `ADMIN`) y consulta de pistas de un club, con deporte, duración de franja y precios punta/valle.
- Consulta de disponibilidad de una pista para un día: franjas libres/ocupadas y precio de cada una.
- Crear reserva de una franja.
- Cancelar reserva, con penalización si faltan menos de 24 h.
- Listar mis reservas (activas e históricas).
- Notificación al crear y cancelar, a través de un puerto con un adaptador falso (log).
- Control de concurrencia: sin solapes garantizados por la BD y bloqueo optimista.
- Errores con formato `ProblemDetail` (RFC 9457).

## Fuera de alcance (fase 1)

- Login. En la fase 1 el usuario se identifica con la cabecera `X-User-Id`, y los endpoints de admin comprueban el rol de ese usuario. **Fase 2:** login con email y contraseña (JWT). **Fase 3:** login con Google.
- Recuperar la contraseña, verificar el email, promocionar a admin a un jugador que ya existe.
- Pagos reales. La penalización se registra como importe, no se cobra.
- Reservas de varias franjas seguidas, reservas recurrentes, listas de espera.
- Partidos abiertos, invitar jugadores, dividir el pago.
- Notificaciones reales (email, push).
- Varias zonas horarias. Todo en `Europe/Madrid`.
- Front-end, paginación avanzada, caché, métricas.

## Documentación

| Documento | Contenido |
|---|---|
| [00 – Decisiones de producto](docs/00-decisiones.md) | Valores elegidos y por qué |
| [01 – Estructura de carpetas](docs/01-estructura-carpetas.md) | Paquetes, capas y reglas de dependencia |
| [02 – Modelo de dominio](docs/02-modelo-dominio.md) | Agregados, value objects, políticas y puertos |
| [03 – Modelo de base de datos](docs/03-modelo-base-datos.md) | Tablas, relaciones, índices, antisolapes |
| [04 – Contrato de la API](docs/04-contrato-api.md) | Endpoints, payloads y errores |
| [05 – Reglas de negocio](docs/05-reglas-negocio.md) | Reglas y casos límite que deben tener test |
| [06 – Hoja de ruta](docs/06-hoja-de-ruta.md) | Sesiones de 1–2 h con criterios de aceptación |

## Cómo arrancar (lo completas tú en la sesión 0)

```bash
./mvnw spring-boot:run   # levanta MySQL con compose automáticamente
./mvnw verify            # tests, incluidos los de Testcontainers
```

### Primer ADMIN (D16)

Al arrancar, si no existe ningún `ADMIN`, se crea uno con estas variables de entorno. Sin ellas la app arranca igual y lo avisa con un `WARN`. Nunca las pongas con valores en `application.yaml`.

```bash
export COURTBOOK_BOOTSTRAP_ADMIN_EMAIL=admin@courtbook.local
export COURTBOOK_BOOTSTRAP_ADMIN_PASSWORD='Cambia-3sto'   # reglas de D14
./mvnw spring-boot:run
```

## Normas no negociables

1. El dominio es Java puro: ni Spring, ni JPA, ni Lombok. Si borras Spring del classpath, `domain` y `application` compilan.
2. Los casos de uso son interfaces (puertos de entrada); sus implementaciones se registran con `@Bean`, nunca con `@Service`.
3. El tiempo se obtiene de un puerto `ClockPort`. Nada de `LocalDateTime.now()` fuera del adaptador.
4. Las entidades JPA solo existen en el adaptador de persistencia.
5. `@Transactional` solo en infraestructura (adaptador o configuración).
6. Cada regla de `05-reglas-negocio.md` tiene al menos un test.
