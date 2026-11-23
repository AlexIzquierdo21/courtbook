# 02 – Modelo de dominio

Todo en Java puro. Value objects como `record` con validación en el constructor compacto. Entidades como clases con identidad, sin setters públicos: el estado cambia solo mediante métodos con nombre de negocio.

## Value objects

| Record | Campos | Invariantes |
|---|---|---|
| `UserId` | `UUID value` | no nulo |
| `ClubId`, `CourtId`, `ReservationId` | `Long value` | no nulo, > 0 |
| `Email` | `String value` | formato válido, se normaliza a minúsculas |
| `PasswordHash` | `String value` | no vacío; `toString` enmascarado |
| `PlainPassword` | `String value` | reglas de D14 (8 caracteres a 72 bytes, sin espacios, mayúscula, minúscula, dígito y especial); `toString` enmascarado. **Nunca** se persiste ni se loguea; solo viaja hasta `PasswordHasher` |
| `InvitationId` | `UUID value` | no nulo |
| `InvitationTokenHash` | `String value` | 64 caracteres hex (SHA-256) |
| `Money` | `BigDecimal amount`, `Currency currency` | amount ≥ 0, escala 2, solo EUR en fase 1. Métodos `percentage(int)`, `isZero()` |
| `TimeSlot` | `LocalDateTime start`, `LocalDateTime end` | start < end. Métodos `overlaps(TimeSlot)`, `duration()`, `startsWithin(...)` |
| `OpeningHours` | `LocalTime opens`, `LocalTime closes` | opens < closes |
| `PeakWindow` | `Set<DayOfWeek> days`, `LocalTime from`, `LocalTime to` | from < to, days no vacío. Método `contains(LocalDateTime)` |
| `CourtPricing` | `Money offPeak`, `Money peak` | ambos misma moneda |

Enums: `Sport { PADEL, TENNIS }`, `Role { PLAYER, ADMIN }`, `ReservationStatus { CONFIRMED, CANCELLED }`.

## Entidades / agregados

### User
`UserId id`, `String firstName`, `String lastName`, `Email email`, `PasswordHash passwordHash`, `Role role`.
- `User.create(...)` → siempre `PLAYER`. `User.createAdmin(...)` solo lo usan la invitación y el bootstrap.
- `User.restore(...)` para reconstruir desde persistencia, con las mismas invariantes.
- `isAdmin()`.

### AdminInvitation (agregado, módulo `user`)
`InvitationId id`, `Email email`, `InvitationTokenHash tokenHash`, `UserId invitedBy`, `LocalDateTime createdAt`, `LocalDateTime expiresAt`, `InvitationStatus status` (`PENDING`, `ACCEPTED`, `REVOKED`), `LocalDateTime acceptedAt`, `Long version`.
- `AdminInvitation.issue(email, tokenHash, invitedBy, now, ttl)` → `PENDING`, `expiresAt = now + ttl`.
- `accept(now)` → `ACCEPTED`. Lanza `InvitationExpiredException` si `now >= expiresAt`, `InvitationNotPendingException` si no está `PENDING`.
- `revoke()`.
- `isExpired(now)`.

El token en claro **no** está en el agregado: lo genera el caso de uso, se lo pasa al notificador y solo persiste el hash.

### Club (agregado)
`ClubId id`, `String name`, `String address`, `OpeningHours openingHours`, `List<PeakWindow> peakWindows`.
- `isPeak(LocalDateTime)`.

### Court (agregado; referencia a `ClubId`, no al objeto Club)
`CourtId id`, `ClubId clubId`, `String name`, `Sport sport`, `Duration slotDuration`, `CourtPricing pricing`, `boolean active`.
- `deactivate()`.

### Reservation (agregado principal)
`ReservationId id`, `CourtId courtId`, `UserId userId`, `TimeSlot slot`, `Money price`, `ReservationStatus status`, `Money penalty` (cero si no hay), `LocalDateTime createdAt`, `LocalDateTime cancelledAt`, `Long version`.

- Factoría `Reservation.create(courtId, userId, slot, price, now)` → `CONFIRMED`.
- `cancel(LocalDateTime now, CancellationPolicy policy)` → pasa a `CANCELLED`, calcula `penalty`. Lanza excepción si ya está cancelada o ya empezó.
- `isActive(LocalDateTime now)` → `CONFIRMED && slot.start > now`.
- `isOwnedBy(UserId)`.

> **Sobre `version`:** es un concepto de persistencia, pero para que el bloqueo optimista funcione entre lectura y escritura el valor tiene que viajar con el objeto. Lo aceptamos como campo opaco en el dominio (el dominio no lo usa, solo lo transporta). Es una concesión consciente; apúntala en tu Notion.

## Servicios y políticas de dominio

| Clase | Responsabilidad |
|---|---|
| `SlotCalendar` | Dado `OpeningHours`, `slotDuration` y una fecha, genera la lista de `TimeSlot` válidos. `isValidSlot(TimeSlot)` comprueba alineación y horario. |
| `PricingPolicy` | `priceFor(Court, Club, TimeSlot)` → punta si la franja empieza en una `PeakWindow`. |
| `CancellationPolicy` | `penaltyFor(Reservation, now)` → 0 o `percentage` del precio según `BookingRules`. |
| `BookingRules` (record) | `int maxActiveReservations`, `int maxAdvanceDays`, `Duration freeCancellationWindow`, `int penaltyPercentage`. Lo construye la configuración a partir de `application.yaml`; el dominio no sabe de dónde viene. |

Las franjas **no se persisten**: se calculan al vuelo. "Franja horaria" es un value object, no una tabla.

## Excepciones de dominio

Jerarquía bajo `DomainException` (unchecked). Cada una con un `code` estable que el `@RestControllerAdvice` usará.

Las excepciones de validación del módulo `user` heredan de `UserDomainException`, para poder tratar juntos todos los errores de usuario. Cada subclase conserva su propio `code` a través del constructor `protected UserDomainException(code, message)`:

| Excepción | Código |
|---|---|
| `UserNotFoundException`, `ClubNotFoundException`, `CourtNotFoundException`, `ReservationNotFoundException` | `*_NOT_FOUND` |
| `EmailAlreadyRegisteredException` | `EMAIL_ALREADY_REGISTERED` |
| `UserDomainException` (invariantes de `User`, `UserId`, `PasswordHash`) | `INVALID_USER` |
| `InvalidPasswordException` (hereda de `UserDomainException`) | `INVALID_PASSWORD` |
| `AdminRequiredException` | `ADMIN_REQUIRED` |
| `InvitationNotFoundException` | `INVITATION_NOT_FOUND` |
| `InvitationExpiredException` | `INVITATION_EXPIRED` |
| `InvitationNotPendingException` | `INVITATION_NOT_PENDING` |
| `InvalidEmailException` (hereda de `UserDomainException`) | `INVALID_EMAIL` |
| `InvalidSlotException` | `INVALID_SLOT` |
| `SlotInPastException` | `SLOT_IN_PAST` |
| `BookingTooFarInAdvanceException` | `BOOKING_TOO_FAR_IN_ADVANCE` |
| `CourtInactiveException` | `COURT_INACTIVE` |
| `SlotAlreadyBookedException` | `SLOT_ALREADY_BOOKED` |
| `MaxActiveReservationsException` | `MAX_ACTIVE_RESERVATIONS` |
| `CancellationNotAllowedException` | `CANCELLATION_NOT_ALLOWED` |
| `ConcurrentModificationException` (en `shared.domain`; nombre propio, no el de `java.util`) | `CONCURRENT_MODIFICATION` |
| `ClubNameAlreadyExistsException` | `CLUB_NAME_ALREADY_EXISTS` |
| `CourtNameAlreadyExistsException` | `COURT_NAME_ALREADY_EXISTS` |

Una reserva ajena se trata como inexistente (`ReservationNotFoundException`), para no revelar que existe.

## Puertos

### Entrada (`application/port/in`, solo interfaces; la implementación es `<Nombre>Service` en `application/usecase`)

| Interfaz | Entrada | Salida |
|---|---|---|
| `RegisterUserUseCase` | `RegisterUserCommand(firstName, lastName, email, plainPassword)` | `User` |
| `GetUserQuery` | `UserId` | `User` |
| `InviteAdminUseCase` | `InviteAdminCommand(requesterId, email)` | `AdminInvitation` |
| `AcceptAdminInvitationUseCase` | `AcceptInvitationCommand(token, firstName, lastName, plainPassword)` | `User` |
| `BootstrapAdminUseCase` | `BootstrapAdminCommand(email, plainPassword)` | `Optional<User>` (vacío si ya había admin) |
| `CreateClubUseCase` | `CreateClubCommand(requesterId, name, address, openingHours, peakWindows)` | `Club` |
| `GetClubQuery` / `ListClubsQuery` | `ClubId` / — | `Club` / `List<Club>` |
| `CreateCourtUseCase` | `CreateCourtCommand(requesterId, clubId, name, sport, slotDuration, offPeak, peak)` | `Court` |
| `DeactivateCourtUseCase` | `DeactivateCourtCommand(requesterId, courtId)` | `Court` |
| `ListCourtsQuery` | `ClubId` | `List<Court>` |
| `GetAvailabilityQuery` | `CourtId`, `LocalDate` | `List<SlotAvailability(slot, free, price, peak)>` |
| `CreateReservationUseCase` | `CreateReservationCommand(userId, courtId, start)` | `Reservation` |
| `CancelReservationUseCase` | `CancelReservationCommand(userId, reservationId)` | `Reservation` |
| `GetReservationQuery` | `UserId`, `ReservationId` | `Reservation` |
| `ListUserReservationsQuery` | `UserId`, filtro `ACTIVE | ALL` | `List<Reservation>` |

Devolver objetos de dominio a infraestructura está permitido; el controlador los mapea a DTO.

### Salida (`application/port/out`)

| Interfaz | Métodos mínimos |
|---|---|
| `UserRepository` | `save`, `findById`, `existsByEmail`, `existsAdmin()` |
| `PasswordHasher` | `PasswordHash hash(PlainPassword)`, `boolean matches(PlainPassword, PasswordHash)` |
| `AdminInvitationRepository` | `save`, `findByTokenHash`, `findPendingByEmail` |
| `InvitationTokenGenerator` | `String newToken()`, `InvitationTokenHash hash(String token)` |
| `InvitationNotifier` | `adminInvited(Email, String token, LocalDateTime expiresAt)`. Adaptador falso: loguea el enlace |
| `ClubRepository` | `save`, `findById`, `findAll` |
| `CourtRepository` | `save`, `findById`, `findByClub` |
| `ReservationRepository` | `save`, `findById`, `findConfirmedByCourtAndDay(CourtId, LocalDate)`, `countActiveByUser(UserId, now)`, `findByUser(UserId)` |
| `BookingQuotaLock` (módulo `reservation`) | `acquire(UserId)`: serializa las reservas de un mismo usuario (ver doc 03) |
| `ClockPort` | `LocalDateTime now()` |
| `NotificationPort` | `reservationConfirmed(Reservation)`, `reservationCancelled(Reservation)` |

## Flujo de `CreateReservationService` (orden esperado)

1. Cargar usuario (`GetUserQuery`), pista y club (`GetCourtQuery`, `GetClubQuery`); 404 si no existen.
2. Pista activa.
3. Construir `TimeSlot` desde `start` + `slotDuration`; validar con `SlotCalendar`.
4. `now = clock.now()`; no en pasado; no más allá de `maxAdvanceDays`.
5. `bookingQuotaLock.acquire(userId)` y contar reservas activas del usuario < `maxActiveReservations`.
6. Comprobar que la franja está libre (comprobación "amable"; la garantía real es el índice único).
7. Calcular precio con `PricingPolicy`.
8. `Reservation.create(...)`, guardar.
9. Notificar. Si la notificación falla, **la reserva no se deshace** (log y seguir).

## Comprobar que el solicitante es ADMIN

Los casos de uso de admin (`InviteAdmin`, `CreateClub`, `CreateCourt`, `DeactivateCourt`) reciben `requesterId` en su command y lo primero que hacen es cargar el usuario y lanzar `AdminRequiredException` si no es `ADMIN`. Para eso `club` usa `GetUserQuery` del módulo `user` (permitido según el doc 01). Cuando llegue el login, la autorización pasará a Spring Security y esta comprobación se queda como defensa en profundidad, o la quitas. Lo decides entonces.

## Flujo de `AcceptAdminInvitationService`

1. `hash = tokenGenerator.hash(token)`; `findByTokenHash` → `InvitationNotFoundException` si no existe.
2. `invitation.accept(clock.now())`: aquí saltan la caducidad y el "ya usada".
3. Comprobar que el email no se ha registrado entretanto → `EmailAlreadyRegisteredException`.
4. `User.createAdmin(...)` con el email **de la invitación** (no se acepta otro en la petición) y el hash de la contraseña.
5. Guardar usuario e invitación en la misma transacción.
