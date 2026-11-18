# 01 – Estructura de carpetas

## Árbol objetivo

```
src/main/java/com/alexizquierdo/courtbook/
├── CourtbookApplication.java
│
├── domain/                          # Java puro. Cero imports de Spring/JPA.
│   ├── model/
│   │   ├── shared/                  # Money, Sport, ...
│   │   ├── user/                    # User, UserId, Email, Role
│   │   ├── club/                    # Club, ClubId, OpeningHours, PeakWindow
│   │   ├── court/                   # Court, CourtId, CourtPricing
│   │   └── reservation/             # Reservation, ReservationId, TimeSlot, ReservationStatus
│   ├── service/                     # SlotCalendar, PricingPolicy, CancellationPolicy
│   ├── rules/                       # BookingRules (parámetros de negocio, record)
│   └── exception/                   # DomainException y subclases
│
├── application/                     # Java puro. Orquesta, no decide.
│   ├── port/
│   │   ├── in/                      # Casos de uso (interfaces) + commands/queries (records)
│   │   └── out/                     # Repositorios, ClockPort, NotificationPort
│   └── usecase/                     # Implementaciones de los puertos de entrada
│
└── infrastructure/                  # Todo lo que sabe de Spring
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── controller/
    │   │   ├── dto/                 # Request/Response records con Bean Validation
    │   │   ├── mapper/
    │   │   └── error/               # @RestControllerAdvice
    │   └── out/
    │       ├── persistence/
    │       │   ├── entity/          # @Entity
    │       │   ├── repository/      # interfaces Spring Data
    │       │   ├── mapper/          # entidad <-> dominio
    │       │   └── *PersistenceAdapter.java
    │       ├── notification/        # LoggingNotificationAdapter (falso)
    │       └── clock/               # SystemClockAdapter
    └── config/                      # @Configuration con @Bean de casos de uso, transacciones, properties

src/main/resources/
├── application.yaml
└── db/migration/                    # V1__..., V2__... (Flyway)

src/test/java/com/alexizquierdo/courtbook/
├── domain/                          # Tests unitarios puros, sin Spring
├── application/                     # Casos de uso con fakes en memoria, sin Spring
└── infrastructure/                  # @DataJpaTest + Testcontainers, @WebMvcTest, @SpringBootTest
```

## Reglas de dependencia

```
infrastructure ──► application ──► domain
```

- `domain` no depende de nadie.
- `application` depende solo de `domain`.
- `infrastructure` depende de ambos. Nadie depende de `infrastructure`.
- Los DTO REST nunca llegan a `application`: el controlador los convierte en commands.
- Las entidades JPA nunca salen del paquete `persistence`.

## Convenciones

- Puertos de entrada: `CreateReservationUseCase`, `GetAvailabilityQuery`. Una interfaz por acción.
- Commands y queries: records en `port/in` (`CreateReservationCommand`).
- Implementación: `CreateReservationService` en `application/usecase`. Sin anotaciones.
- Puertos de salida: `ReservationRepository` (dominio habla en su idioma: `save`, `findById`, `findActiveByUser`...), no `JpaRepository`.
- Adaptador de salida: `ReservationPersistenceAdapter implements ReservationRepository`.

## Transacciones (decisión de diseño)

Como los servicios de aplicación no llevan anotaciones, las transacciones se aplican en `config` con un **decorador**: una clase en infraestructura que implementa el mismo puerto de entrada, delega en el servicio real y envuelve la llamada en un `TransactionTemplate` (o lleva `@Transactional`). El `@Bean` que expones es el decorador.

Encargo: decide tú entre `TransactionTemplate` y `@Transactional` en el decorador y justifícalo en un comentario de una línea en la clase de configuración.

## Comprobación

Opcional pero recomendado en la sesión 14: un test de arquitectura (ArchUnit) que falle si `domain` o `application` importan `org.springframework..` o `jakarta.persistence..`.
