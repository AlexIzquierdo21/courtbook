# 06 – Hoja de ruta

Sesiones de 1–2 h. Orden de dentro hacia fuera: dominio → aplicación → persistencia → REST. No pases a la siguiente sesión sin cumplir los criterios de la actual.

**Definición de hecho (todas las sesiones):** `./mvnw verify` en verde, sin código comentado ni `TODO` sin explicar, un commit por sesión con mensaje claro (`feat(domain): add TimeSlot value object`). Me pasas el diff o el commit y lo reviso.

---

## Sesión 0 – Entorno
Objetivo: el proyecto arranca y los tests de Testcontainers pasan.
- Instala JDK 25 y configúralo en IntelliJ.
- `compose.yaml`: renombra base, usuario y contraseña a algo propio (`courtbook`), quita los valores de plantilla.
- Pasa `application.properties` a `application.yaml`; `ddl-auto: validate`, `open-in-view: false`.
- Completa `name` y `description` del `pom.xml`.

Criterios:
- [ ] `./mvnw spring-boot:run` arranca y levanta MySQL solo.
- [ ] `./mvnw verify` pasa con el test de contexto usando Testcontainers.
- [ ] README con la sección "Cómo arrancar" verificada por ti.

## Sesión 1 – Paquetes y value objects básicos
- Crea el árbol de `01-estructura-carpetas.md` (solo lo que uses).
- `Money`, `Email`, ids, `TimeSlot`, enums.

Criterios:
- [ ] Records con validación en constructor compacto.
- [ ] Tests unitarios: valores inválidos lanzan excepción; `TimeSlot.overlaps` cubre contiguos (no solapan), contenidos e iguales.
- [ ] Ningún import de Spring/JPA en `domain`.

## Sesión 2 – Club, Court y SlotCalendar
- `OpeningHours`, `PeakWindow`, `CourtPricing`, `Club`, `Court`, `SlotCalendar`.

Criterios:
- [ ] Todos los casos de R1 en verde.
- [ ] `Club.isPeak` con casos de R5.

## Sesión 3 – Reservation y políticas
- `Reservation` con `create`, `cancel`, `isActive`, `isOwnedBy`.
- `BookingRules`, `PricingPolicy`, `CancellationPolicy`.
- Excepciones de dominio con `code`.

Criterios:
- [ ] Casos de R5 y R6 en verde con hora fija (nada de `now()`).
- [ ] `Reservation` no tiene setters públicos.

## Sesión 4 – Puertos y CreateReservation
- Puertos de salida e interfaces de entrada.
- `CreateReservationService` siguiendo el flujo de `02-modelo-dominio.md`.
- Fakes en memoria en `src/test` para repositorios, `ClockPort` fijo y `NotificationPort` espía.

Criterios:
- [ ] Casos de R2 (no concurrentes), R3, R4 (no concurrentes) y R8 en verde sin Spring.
- [ ] Ningún test de esta capa tarda más de 100 ms.

## Sesión 5 – Resto de casos de uso
- `CancelReservation`, `GetAvailability`, `ListUserReservations`, `GetReservation`, usuarios, clubes, pistas.

Criterios:
- [ ] R6 (ajena, ya cancelada, franja liberada), R7 en verde.
- [ ] Disponibilidad: franjas pasadas no disponibles; precio y `peak` correctos.

## Sesión 6 – Persistencia I: usuarios, clubes y pistas
- Migraciones V1–V3, entidades, repositorios Spring Data, mappers, adaptadores.

Criterios:
- [ ] `@DataJpaTest` con Testcontainers (no H2) guarda y recupera cada agregado.
- [ ] `ddl-auto=validate` arranca sin errores.
- [ ] Las entidades no aparecen fuera de `persistence`.

## Sesión 7 – Persistencia II: reservas y antisolapes
- Migración V4 con columna generada e índices.
- `ReservationPersistenceAdapter`, traducción de violación del índice a `SlotAlreadyBookedException`.
- `@Version` y `lockForBooking`.

Criterios:
- [ ] Test que inserta dos confirmadas iguales y espera la excepción de dominio.
- [ ] Test que cancela y vuelve a reservar la misma franja.
- [ ] `EXPLAIN` de la query de disponibilidad y de `countActiveByUser` usa índice (pega la salida en el PR/commit).

## Sesión 8 – Configuración
- `@Configuration` con `@Bean` de cada caso de uso.
- Decorador transaccional (ver doc 01).
- `SystemClockAdapter`, `LoggingNotificationAdapter`.
- `BookingRules` desde `application.yaml` con `@ConfigurationProperties`.

Criterios:
- [ ] Cero `@Service`/`@Component` en `application`.
- [ ] Cambiar `max-active-reservations` en yaml cambia el comportamiento sin tocar código.
- [ ] `@SpringBootTest` levanta el contexto.

## Sesión 9 – REST I: usuarios, clubes y pistas
- Controladores, DTOs con Bean Validation, mappers.

Criterios:
- [ ] `@WebMvcTest` por controlador: 201 + `Location`, 400 con `errors`, 404.
- [ ] Ningún controlador recibe ni devuelve objetos de dominio en la firma pública HTTP.

## Sesión 10 – REST II: disponibilidad y reservas
- Endpoints de disponibilidad y reservas; resolución de `X-User-Id`.

Criterios:
- [ ] Contrato de `04-contrato-api.md` respetado campo a campo (importes como string).
- [ ] Falta de `X-User-Id` → 400.

## Sesión 11 – Manejo de errores
- `@RestControllerAdvice` con `ProblemDetail` y `code`.

Criterios:
- [ ] Cada código de la tabla de errores tiene un test que comprueba status y `code`.
- [ ] Un error inesperado devuelve 500 genérico sin stack trace y se loguea.

## Sesión 12 – Concurrencia
- Test de integración (`@SpringBootTest` + Testcontainers) con `ExecutorService` y `CountDownLatch`.

Criterios:
- [ ] 10 hilos sobre la misma franja → 1 × 201 y 9 × 409, y 1 sola fila `CONFIRMED` en BD.
- [ ] Usuario con 2 activas, 2 peticiones paralelas → como mucho 3 activas al final.
- [ ] Explicas en 3 líneas (en el commit o en Notion) por qué cada caso queda cubierto.

## Sesión 13 – Pulido y E2E
- Test end-to-end: crear usuario, club, pista, ver disponibilidad, reservar, cancelar con y sin penalización.
- Revisa logs, nombres y README.

Criterios:
- [ ] Flujo completo en un solo test verde.
- [ ] README actualizado con ejemplos `curl`.

## Sesión 14 (opcional) – Test de arquitectura
- ArchUnit: `domain` y `application` sin dependencias de Spring/JPA; nadie depende de `infrastructure`.

Criterios:
- [ ] El test falla si añades un `@Service` en `application` (compruébalo y revierte).

---

## Fase 2 (no empezar hasta cerrar la fase 1)
- Spring Security + JWT: registro con contraseña, login, `X-User-Id` sustituido por el token.
- Roles: solo `ADMIN` crea clubes y pistas.
- Notificaciones tras commit (eventos o outbox).
- OpenAPI/Swagger.
