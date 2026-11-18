# 01 – Estructura de carpetas

Organización **por módulo de negocio** (package-by-feature) y, dentro de cada módulo, las tres capas hexagonales. Cada módulo es un hexágono pequeño.

## Módulos

| Módulo | Contiene | Por qué |
|---|---|---|
| `user` | User, UserId, Email, Role | Se llama `user`, no `auth`: autenticación es otra cosa y llega en la fase 2 (irá en su propio módulo `auth` o en `user/infrastructure/security`). |
| `club` | Club **y Court**, OpeningHours, PeakWindow, CourtPricing, SlotCalendar | Una pista no existe sin su club y el precio depende de las ventanas punta del club. Separarlos obligaría a llamadas entre módulos para cada cálculo. Si algún día la pista crece (mantenimiento, iluminación...), se extrae. |
| `reservation` | Reservation, TimeSlot, ReservationStatus, BookingRules, PricingPolicy, CancellationPolicy, NotificationPort | El núcleo. Es el único módulo que depende de otros. |
| `shared` | Money, ClockPort, DomainException base, el `@RestControllerAdvice` | Solo lo que de verdad usan varios. Si dudas, no va aquí: un `shared` gordo es un módulo basura. Los ids **no** van aquí; cada uno en su módulo. |

## Árbol objetivo

```
src/main/java/com/alexizquierdo/courtbook/
├── CourtbookApplication.java
│
├── shared/
│   ├── domain/                      # Money, DomainException
│   ├── application/port/out/        # ClockPort
│   └── infrastructure/
│       ├── clock/                   # SystemClockAdapter
│       └── rest/                    # GlobalExceptionHandler (@RestControllerAdvice)
│
├── user/
│   ├── domain/                      # User, UserId, Email, Role, excepciones
│   ├── application/
│   │   ├── port/in/                 # RegisterUserUseCase, GetUserQuery + commands
│   │   ├── port/out/                # UserRepository
│   │   └── usecase/                 # RegisterUserService, ...
│   └── infrastructure/
│       ├── rest/                    # UserController, dto/, mapper/
│       ├── persistence/             # UserEntity, UserJpaRepository, mapper, UserPersistenceAdapter
│       └── config/                  # UserConfiguration (@Bean)
│
├── club/
│   ├── domain/                      # Club, Court, OpeningHours, PeakWindow, CourtPricing, SlotCalendar
│   ├── application/ ...             # misma forma que user
│   └── infrastructure/ ...
│
└── reservation/
    ├── domain/                      # Reservation, TimeSlot, políticas, BookingRules
    ├── application/
    │   ├── port/in/                 # CreateReservationUseCase, CancelReservationUseCase, GetAvailabilityQuery...
    │   ├── port/out/                # ReservationRepository, NotificationPort
    │   └── usecase/
    └── infrastructure/
        ├── rest/
        ├── persistence/
        ├── notification/            # LoggingNotificationAdapter (falso)
        └── config/                  # ReservationConfiguration, decorador transaccional, BookingRulesProperties

src/main/resources/
├── application.yaml
└── db/migration/                    # V1__..., V2__... (Flyway, global, no por módulo)

src/test/java/com/alexizquierdo/courtbook/
└── <modulo>/{domain,application,infrastructure}/   # misma forma que main
```

## Reglas de dependencia

Dentro de cada módulo:

```
infrastructure ──► application ──► domain
```

Entre módulos:

```
reservation ──► club, user ──► shared
```

- `domain` y `application` de cualquier módulo: Java puro.
- `shared` no depende de ningún módulo.
- `user` y `club` **nunca** dependen de `reservation`. Ciclos prohibidos.
- `reservation` puede usar de otro módulo **solo** su `domain` (ids, value objects) y sus puertos de entrada (`GetCourtQuery`, `GetUserQuery`). Nunca su repositorio, su entidad JPA ni su controlador.
- Los DTO REST nunca llegan a `application`. Las entidades JPA nunca salen de `persistence`.

Consecuencia que debes asumir: la reserva tiene FK a `court` y `app_user` en BD, pero en Java solo conoce `CourtId` y `UserId`. No hay `@ManyToOne` entre entidades de módulos distintos.

## Convenciones

- Puertos de entrada: `CreateReservationUseCase`, `GetAvailabilityQuery`. Una interfaz por acción.
- Commands y queries: records en `port/in` (`CreateReservationCommand`).
- Implementación: `CreateReservationService` en `application/usecase`. Sin anotaciones.
- Puertos de salida: `ReservationRepository` (dominio habla en su idioma: `save`, `findById`, `findActiveByUser`...), no `JpaRepository`.
- Adaptador de salida: `ReservationPersistenceAdapter implements ReservationRepository`.
- Una clase `@Configuration` por módulo.

## Transacciones (decisión de diseño)

Como los servicios de aplicación no llevan anotaciones, las transacciones se aplican en `config` con un **decorador**: una clase en infraestructura que implementa el mismo puerto de entrada, delega en el servicio real y envuelve la llamada en un `TransactionTemplate` (o lleva `@Transactional`). El `@Bean` que expones es el decorador.

Encargo: decide tú entre `TransactionTemplate` y `@Transactional` en el decorador y justifícalo en un comentario de una línea en la clase de configuración.

## Comprobación

Opcional pero recomendado en la sesión 14: un test de arquitectura (ArchUnit) que falle si `domain` o `application` importan `org.springframework..` o `jakarta.persistence..`, si hay ciclos entre módulos, o si `reservation` toca `club.infrastructure` o `user.infrastructure`.
