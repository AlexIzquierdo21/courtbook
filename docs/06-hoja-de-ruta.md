# 06 – Hoja de ruta

Sesiones de 1–2 h. Se avanza **por módulo y de punta a punta**: primero `user` entero (dominio → aplicación → persistencia → REST), después `club` y por último `reservation`. Dentro de cada módulo, de dentro hacia fuera. No pases a la siguiente sesión sin cumplir los criterios de la actual.

**Definición de hecho (todas las sesiones):** `./mvnw verify` en verde, sin código comentado ni `TODO` sin explicar, un commit por sesión con mensaje claro (`feat(user): add Email value object`). Me pasas el diff o el commit y lo reviso.

---

## Sesión 0 – Entorno
Objetivo: el proyecto arranca y los tests de Testcontainers pasan.
- Instala JDK 25 y configúralo en IntelliJ.
- `compose.yaml`: renombra base, usuario y contraseña a algo propio (`courtbook`), quita los valores de plantilla.
- Pasa `application.properties` a `application.yaml`; `ddl-auto: validate`, `open-in-view: false`.
- Completa `name` y `description` del `pom.xml`.

Criterios:
- [x] `./mvnw spring-boot:run` arranca y levanta MySQL solo.
- [x] `./mvnw verify` pasa con el test de contexto usando Testcontainers.
- [ ] README con la sección "Cómo arrancar" verificada por ti.

## Sesión 1 – Dominio de `user` (casi hecha)
Hecho: User, Email, UserId (UUID), PasswordHash, Role y sus tests. Te falta:
- `PlainPassword` (8–72 caracteres, `toString` enmascarado).
- Que `InvalidEmailException` herede de `UserDomainException`, o justifica por qué no.
- Validar el null de `UserId` en el constructor compacto, no en `of`. `UserId.of("x")` no puede lanzar `IllegalArgumentException`: tiene que ser una excepción de dominio.
- `User.createAdmin(...)` e `isAdmin()`.
- Arregla `passwordHashIsNotPrinted`: compara con `HASH.value()`.

Criterios:
- [ ] Casos de R7 que son de dominio (contraseña, toString) en verde.
- [ ] Ningún import de Spring/JPA en `user.domain`.

## Sesión 2 – Aplicación de `user`: registro
- Puertos `RegisterUserUseCase` y `GetUserQuery`, `UserRepository` y `PasswordHasher`.
- `RegisterUserService` y `GetUserService`.
- Fakes en `src/test`: repositorio en memoria y un `PasswordHasher` falso (por ejemplo `"hashed:" + plain`).

Criterios:
- [ ] Casos de R7 de aplicación (duplicado, siempre PLAYER, se guarda el hash) en verde, sin Spring.

## Sesión 3 – Persistencia de `user` y BCrypt
- `V1__create_app_user.sql` con `BINARY(16)`.
- `UserEntity`, repositorio Spring Data, mapper y `UserPersistenceAdapter`.
- Añade `spring-security-crypto` (solo esa librería) y escribe `BCryptPasswordHasher`.

Criterios:
- [ ] `@DataJpaTest` con Testcontainers: guardar, buscar por id, `existsByEmail` (sin distinguir mayúsculas) y `existsAdmin`.
- [ ] El UUID sobrevive a la ida y vuelta: el que guardas es el que lees.
- [ ] Test de `BCryptPasswordHasher`: `matches` da true con la buena y false con otra.

## Sesión 4 – Configuración, REST de `user` y errores base
- `UserConfiguration` con los `@Bean` y el decorador transaccional (doc 01).
- `UserController`: `POST /users` y `GET /users/{id}`.
- `GlobalExceptionHandler` en `shared`, con `ProblemDetail` y `code` (por ahora con los errores que ya existen).
- Resolución de `X-User-Id` como UUID, reutilizable por todos los controladores.

Criterios:
- [ ] `@WebMvcTest`: 201 con `Location`, 400 con `errors`, 409 `EMAIL_ALREADY_REGISTERED`, y ni rastro de la contraseña en el JSON.
- [ ] Cero `@Service`/`@Component` en `user.application`.

## Sesión 5 – Bootstrap del primer ADMIN
- `BootstrapAdminUseCase` y su servicio.
- `AdminBootstrapRunner` (`ApplicationRunner`) en infraestructura, que lee `courtbook.bootstrap.admin.*` con `@ConfigurationProperties`.
- En local, las propiedades por variable de entorno. **No** las pongas con valores en `application.yaml` del repo.

Criterios:
- [ ] Todos los casos de R10 en verde: los de lógica con fakes y uno de integración con `@SpringBootTest`.

## Sesión 6 – Invitaciones: dominio y aplicación
- `AdminInvitation`, `InvitationStatus`, `InvitationId`, `InvitationTokenHash` y sus excepciones.
- Puertos `InviteAdminUseCase`, `AcceptAdminInvitationUseCase`, `AdminInvitationRepository`, `InvitationTokenGenerator` e `InvitationNotifier`.
- La caducidad, `invitation-ttl: 72h`, como parámetro de configuración que llega al servicio.

Criterios:
- [ ] Todos los casos de R9 excepto el de concurrencia, en verde y sin Spring, con un `ClockPort` fijo.

## Sesión 7 – Invitaciones: persistencia y REST
- `V2__create_admin_invitation.sql`, entidad con `@Version`, adaptador.
- `SecureRandomTokenGenerator` (32 bytes de `SecureRandom`, Base64URL sin padding, hash SHA-256 en hex).
- `LoggingInvitationNotifier`.
- `POST /admin/invitations` y `POST /admin/invitations/accept`.

Criterios:
- [ ] Test de integración: el admin de bootstrap invita, se lee el token del notificador espía, se acepta y el nuevo usuario es `ADMIN`.
- [ ] En la tabla nunca aparece el token en claro.
- [ ] Un `PLAYER` recibe 403 `ADMIN_REQUIRED`.
- [ ] Dos aceptaciones simultáneas: un 201 y un 409.

**Aquí el módulo `user` está cerrado.** Pásamelo entero para revisión antes de seguir.

## Sesión 8 – Dominio de `club`
- `Money` en `shared`, `OpeningHours`, `PeakWindow`, `CourtPricing`, `Club`, `Court` y `SlotCalendar`.

Criterios:
- [ ] Casos de R1 y `Club.isPeak` con los casos de R5 en verde.

## Sesión 9 – `club`: aplicación y persistencia
- Casos de uso de club y pista (los de admin reciben `requesterId` y usan `GetUserQuery`).
- `V3__create_club_and_peak_window.sql` y `V4__create_court.sql`, entidades y adaptadores.

Criterios:
- [ ] Tests de aplicación con fakes, incluido `ADMIN_REQUIRED`.
- [ ] `@DataJpaTest` de club con sus ventanas punta y de pista.

## Sesión 10 – REST de `club`
- Endpoints de clubes y pistas.

Criterios:
- [ ] `@WebMvcTest` por endpoint: 201, 400, 403 y 404.

## Sesión 11 – Dominio de `reservation`
- `TimeSlot`, `Reservation`, `BookingRules`, `PricingPolicy` y `CancellationPolicy`.

Criterios:
- [ ] Casos de R5 y R6 en verde con la hora fija.
- [ ] `Reservation` sin setters públicos.

## Sesión 12 – `reservation`: casos de uso
- `CreateReservation`, `CancelReservation`, `GetAvailability`, `ListUserReservations` y `GetReservation`, con fakes. Sigue el flujo del doc 02.

Criterios:
- [ ] R2, R3 y R4 (sin los de concurrencia) y R8 en verde, sin Spring.

## Sesión 13 – `reservation`: persistencia y antisolapes
- `V5__create_reservation.sql` con la columna generada y los índices, el adaptador, la traducción de la violación del índice, `@Version` y `lockForBooking`.

Criterios:
- [ ] Dos `CONFIRMED` iguales dan la excepción de dominio. Cancelar y volver a reservar la misma franja funciona.
- [ ] `EXPLAIN` de la disponibilidad y de `countActiveByUser` usando índice (pega la salida en el commit).

## Sesión 14 – REST de `reservation` y errores completos
- Endpoints de disponibilidad y reservas. Completa `GlobalExceptionHandler` con todos los códigos.

Criterios:
- [ ] El contrato del doc 04 se cumple campo a campo.
- [ ] Cada código de error tiene un test que comprueba el status y el `code`.

## Sesión 15 – Concurrencia
- `@SpringBootTest` con Testcontainers, `ExecutorService` y `CountDownLatch`.

Criterios:
- [ ] 10 hilos sobre la misma franja: un 201, nueve 409 y una sola fila `CONFIRMED`.
- [ ] Usuario con 2 reservas activas que lanza 2 peticiones a la vez: como mucho 3 activas al final.
- [ ] Explica en 3 líneas por qué queda cubierto cada caso.

## Sesión 16 – E2E y pulido
- Un flujo completo: bootstrap del admin, invitación, el admin crea club y pista, un jugador se registra, consulta la disponibilidad, reserva y cancela con y sin penalización.
- README con ejemplos de `curl`.

## Sesión 17 (opcional) – Test de arquitectura
- ArchUnit: sin Spring ni JPA en `domain`/`application`, sin ciclos entre módulos, y nadie toca la `infrastructure` de otro módulo.

---

## Fase 2 – Login con email y contraseña
- Spring Security + JWT. `POST /auth/login` usa `PasswordHasher.matches`.
- `X-User-Id` desaparece y el usuario sale del token. `ADMIN_REQUIRED` pasa a resolverse con la autorización de Security.

## Fase 3 – Login con Google
- Puerto `ExternalIdentityVerifier` en `user.application.port.out`: `verify(String idToken) → ExternalIdentity(provider, subject, email, emailVerified)`. Su adaptador, `GoogleIdentityVerifier`, valida el ID token de Google.
- Nueva tabla `user_external_identity (user_id, provider, subject)` con UNIQUE `(provider, subject)`. Identifica al usuario por `subject`, nunca solo por el email.
- `password_hash` pasa a ser nullable (un usuario solo de Google no tiene contraseña), y esa invariante se mueve al dominio: el usuario debe tener contraseña o al menos una identidad externa.
- Decide entonces si un admin puede entrar con Google.

## Más adelante
- Notificaciones tras el commit (eventos o outbox) y email real.
- OpenAPI/Swagger.
