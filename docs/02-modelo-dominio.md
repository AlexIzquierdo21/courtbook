# 02 – Modelo de dominio

Todo en Java puro. Value objects como `record` con validación en el constructor compacto. Entidades como clases con identidad, sin setters públicos: el estado cambia solo mediante métodos con nombre de negocio.

## Value objects

| Record | Campos | Invariantes |
|---|---|---|
| `UserId`, `ClubId`, `CourtId`, `ReservationId` | `Long value` | no nulo, > 0 |
| `Email` | `String value` | formato válido, se normaliza a minúsculas |
| `Money` | `BigDecimal amount`, `Currency currency` | amount ≥ 0, escala 2, solo EUR en fase 1. Métodos `percentage(int)`, `isZero()` |
| `TimeSlot` | `LocalDateTime start`, `LocalDateTime end` | start < end. Métodos `overlaps(TimeSlot)`, `duration()`, `startsWithin(...)` |
| `OpeningHours` | `LocalTime opens`, `LocalTime closes` | opens < closes |
| `PeakWindow` | `Set<DayOfWeek> days`, `LocalTime from`, `LocalTime to` | from < to, days no vacío. Método `contains(LocalDateTime)` |
| `CourtPricing` | `Money offPeak`, `Money peak` | ambos misma moneda |

Enums: `Sport { PADEL, TENNIS }`, `Role { PLAYER, ADMIN }`, `ReservationStatus { CONFIRMED, CANCELLED }`.

## Entidades / agregados

### User
`UserId id`, `String name`, `Email email`, `Role role`.

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

Jerarquía bajo `DomainException` (unchecked). Cada una con un `code` estable que el `@RestControllerAdvice` usará:

| Excepción | Código |
|---|---|
| `UserNotFoundException`, `ClubNotFoundException`, `CourtNotFoundException`, `ReservationNotFoundException` | `*_NOT_FOUND` |
| `EmailAlreadyRegisteredException` | `EMAIL_ALREADY_REGISTERED` |
| `InvalidEmailException` | `INVALID_EMAIL` |
| `InvalidSlotException` | `INVALID_SLOT` |
| `SlotInPastException` | `SLOT_IN_PAST` |
| `BookingTooFarInAdvanceException` | `BOOKING_TOO_FAR_IN_ADVANCE` |
| `CourtInactiveException` | `COURT_INACTIVE` |
| `SlotAlreadyBookedException` | `SLOT_ALREADY_BOOKED` |
| `MaxActiveReservationsException` | `MAX_ACTIVE_RESERVATIONS` |
| `CancellationNotAllowedException` | `CANCELLATION_NOT_ALLOWED` |
| `NotReservationOwnerException` | `NOT_RESERVATION_OWNER` |
| `ConcurrentModificationException` (nombre propio, no el de `java.util`) | `CONCURRENT_MODIFICATION` |

## Puertos

### Entrada (`application/port/in`)

| Interfaz | Entrada | Salida |
|---|---|---|
| `RegisterUserUseCase` | `RegisterUserCommand(name, email, role)` | `User` |
| `GetUserQuery` | `UserId` | `User` |
| `CreateClubUseCase` | `CreateClubCommand(name, address, openingHours, peakWindows)` | `Club` |
| `GetClubQuery` / `ListClubsQuery` | `ClubId` / — | `Club` / `List<Club>` |
| `CreateCourtUseCase` | `CreateCourtCommand(clubId, name, sport, slotDuration, offPeak, peak)` | `Court` |
| `DeactivateCourtUseCase` | `CourtId` | `Court` |
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
| `UserRepository` | `save`, `findById`, `existsByEmail`, `lockForBooking(UserId)` (ver doc 03) |
| `ClubRepository` | `save`, `findById`, `findAll` |
| `CourtRepository` | `save`, `findById`, `findByClub` |
| `ReservationRepository` | `save`, `findById`, `findConfirmedByCourtAndDay(CourtId, LocalDate)`, `countActiveByUser(UserId, now)`, `findByUser(UserId)` |
| `ClockPort` | `LocalDateTime now()` |
| `NotificationPort` | `reservationConfirmed(Reservation)`, `reservationCancelled(Reservation)` |

## Flujo de `CreateReservationService` (orden esperado)

1. Cargar usuario, pista y club (404 si no existen).
2. Pista activa.
3. Construir `TimeSlot` desde `start` + `slotDuration`; validar con `SlotCalendar`.
4. `now = clock.now()`; no en pasado; no más allá de `maxAdvanceDays`.
5. Contar reservas activas del usuario < `maxActiveReservations`.
6. Comprobar que la franja está libre (comprobación "amable"; la garantía real es el índice único).
7. Calcular precio con `PricingPolicy`.
8. `Reservation.create(...)`, guardar.
9. Notificar. Si la notificación falla, **la reserva no se deshace** (log y seguir).
