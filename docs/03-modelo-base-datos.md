# 03 – Modelo de base de datos

MySQL 8.4, InnoDB, `utf8mb4`. Esquema gestionado **solo** por Flyway (`spring.jpa.hibernate.ddl-auto=validate`). Nombres en `snake_case`, tablas en singular.

## Diagrama

```
app_user 1 ──── * reservation * ──── 1 court * ──── 1 club 1 ──── * peak_window
```

## Tablas

### `app_user`
(`user` es palabra reservada; no la uses.)

| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(255) | NOT NULL, UNIQUE (`uk_app_user_email`) |
| role | VARCHAR(20) | NOT NULL, CHECK in ('PLAYER','ADMIN') |
| version | BIGINT | NOT NULL DEFAULT 0 |
| created_at | DATETIME(6) | NOT NULL |

### `club`

| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(100) | NOT NULL, UNIQUE |
| address | VARCHAR(255) | NOT NULL |
| opens_at | TIME | NOT NULL |
| closes_at | TIME | NOT NULL, CHECK `closes_at > opens_at` |

### `peak_window`

| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| club_id | BIGINT | NOT NULL, FK → club(id) ON DELETE CASCADE |
| day_of_week | VARCHAR(10) | NOT NULL (`MONDAY`…`SUNDAY`) |
| from_time | TIME | NOT NULL |
| to_time | TIME | NOT NULL, CHECK `to_time > from_time` |

Una fila por día. El `Set<DayOfWeek>` del dominio se aplana en el mapper. Índice: `idx_peak_window_club (club_id)`.

### `court`

| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| club_id | BIGINT | NOT NULL, FK → club(id) |
| name | VARCHAR(50) | NOT NULL |
| sport | VARCHAR(10) | NOT NULL, CHECK in ('PADEL','TENNIS') |
| slot_minutes | INT | NOT NULL, CHECK > 0 |
| off_peak_price | DECIMAL(8,2) | NOT NULL, CHECK ≥ 0 |
| peak_price | DECIMAL(8,2) | NOT NULL, CHECK ≥ 0 |
| active | BOOLEAN | NOT NULL DEFAULT TRUE |

Restricciones: `uk_court_club_name (club_id, name)`.

### `reservation`

| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| court_id | BIGINT | NOT NULL, FK → court(id) |
| user_id | BIGINT | NOT NULL, FK → app_user(id) |
| start_at | DATETIME | NOT NULL |
| end_at | DATETIME | NOT NULL, CHECK `end_at > start_at` |
| price | DECIMAL(8,2) | NOT NULL |
| penalty | DECIMAL(8,2) | NOT NULL DEFAULT 0 |
| status | VARCHAR(20) | NOT NULL, CHECK in ('CONFIRMED','CANCELLED') |
| created_at | DATETIME(6) | NOT NULL |
| cancelled_at | DATETIME(6) | NULL |
| version | BIGINT | NOT NULL DEFAULT 0 |
| active_marker | TINYINT | **columna generada** (ver abajo) |

Índices:
- `uk_reservation_court_slot (court_id, start_at, active_marker)` — UNIQUE, el antisolapes.
- `idx_reservation_user_status (user_id, status, start_at)` — mis reservas y conteo de activas.
- `idx_reservation_court_start (court_id, start_at)` — disponibilidad del día (lo cubre en parte el UNIQUE; compruébalo con `EXPLAIN` y decide si sobra).

## Cómo garantizar que no hay solapes en MySQL

MySQL no tiene restricciones de exclusión (`EXCLUDE USING gist` de PostgreSQL), así que un rango no puede declararse "no solapable". Lo resolvemos con diseño:

1. **Franjas alineadas (D1/D2).** Si todas las reservas de una pista empiezan en la rejilla y duran exactamente `slot_minutes`, dos reservas se solapan **si y solo si** tienen el mismo `start_at`. El problema de rangos se convierte en un problema de igualdad.
2. **Unicidad solo para reservas confirmadas.** Un UNIQUE sobre `(court_id, start_at)` impediría volver a reservar una franja cancelada. Solución: columna generada `active_marker` que vale `1` si `status = 'CONFIRMED'` y `NULL` si no. MySQL permite varios `NULL` en un índice único, así que las canceladas no colisionan y las confirmadas sí.
   Pista: `GENERATED ALWAYS AS (...) STORED`. Busca la sintaxis tú.
3. **Doble validación.** El caso de uso comprueba antes si la franja está libre (error claro y barato). Pero entre la comprobación y el `INSERT` otra transacción puede colarse: el índice es el árbitro final. El adaptador traduce `DataIntegrityViolationException` con ese índice a `SlotAlreadyBookedException` → 409.
4. **Invariante dependiente.** Esto se rompe si un día cambias `slot_minutes` de una pista con reservas futuras. Regla: no se puede cambiar la duración de una pista (no hay endpoint para ello en fase 1). Déjalo documentado.

En Hibernate, `active_marker` se mapea como de solo lectura (`insertable = false, updatable = false`) o directamente no se mapea.

## Bloqueo optimista: dónde y para qué

| Riesgo | Mecanismo |
|---|---|
| Dos usuarios reservan la misma franja a la vez | Índice único (arriba). No es bloqueo optimista; es una restricción. |
| Dos cancelaciones / cancelación y modificación simultáneas de la misma reserva | `@Version` en `reservation`. El segundo `UPDATE` falla → `ConcurrentModificationException` → 409. |
| El mismo usuario lanza 2 reservas en paralelo teniendo 2 activas con máximo 3 | Ambas cuentan 2 y crean la 3.ª y la 4.ª. Solución: `UserRepository.lockForBooking(userId)` incrementa la `version` de `app_user` dentro de la transacción de reserva (`OPTIMISTIC_FORCE_INCREMENT`). La segunda transacción falla al hacer commit → 409, el cliente reintenta y ya recibe `MAX_ACTIVE_RESERVATIONS`. |

Entiende bien el tercer caso: es el que más se pregunta en entrevistas.

## Migraciones previstas

| Fichero | Contenido |
|---|---|
| `V1__create_app_user.sql` | `app_user` |
| `V2__create_club_and_peak_window.sql` | `club`, `peak_window` |
| `V3__create_court.sql` | `court` |
| `V4__create_reservation.sql` | `reservation` + columna generada + índices |

Nunca edites una migración ya aplicada: crea una nueva. Datos de prueba solo en tests, no en migraciones de producción.
