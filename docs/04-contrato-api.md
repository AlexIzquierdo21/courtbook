# 04 – Contrato de la API

Base: `/api/v1`. JSON. Fechas ISO-8601 sin zona (`2026-11-20T18:00:00`), siempre `Europe/Madrid`. Importes como string decimal (`"12.50"`) para no perder precisión.

Cabecera de identidad (fase 1): `X-User-Id: <id>` en todos los endpoints de reservas. Si falta o no es numérica → 400. Si el usuario no existe → 404.

## Usuarios

### `POST /users` → 201
```json
{ "name": "Alex", "email": "alex@example.com", "role": "PLAYER" }
```
Validación: `name` 1–100, `email` válido, `role` obligatorio.
Respuesta y `Location: /api/v1/users/{id}`:
```json
{ "id": "3f2b8c1e-7a4d-4e9b-9c61-2d5e8f0a1b7c", "name": "Alex", "email": "alex@example.com", "role": "PLAYER" }
```
Errores: 400, 409 `EMAIL_ALREADY_REGISTERED`.

### `GET /users/{id}` → 200 | 404

## Clubes

### `POST /clubs` → 201
```json
{
  "name": "Club Pádel Norte",
  "address": "C/ Mayor 1, Madrid",
  "opensAt": "08:00",
  "closesAt": "23:00",
  "peakWindows": [
    { "days": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"], "from": "18:00", "to": "22:00" },
    { "days": ["SATURDAY","SUNDAY"], "from": "09:00", "to": "14:00" }
  ]
}
```
Errores: 400 (incluido `closesAt <= opensAt`), 409 si el nombre ya existe.

### `GET /clubs` → 200 (lista) · `GET /clubs/{id}` → 200 | 404

## Pistas

### `POST /clubs/{clubId}/courts` → 201
```json
{ "name": "Pista 1", "sport": "PADEL", "slotMinutes": 90, "offPeakPrice": "18.00", "peakPrice": "28.00" }
```
Respuesta:
```json
{ "id": 10, "clubId": 1, "name": "Pista 1", "sport": "PADEL", "slotMinutes": 90,
  "offPeakPrice": "18.00", "peakPrice": "28.00", "active": true }
```
Errores: 400, 404 club, 409 nombre repetido en el club.

### `GET /clubs/{clubId}/courts` → 200
### `POST /courts/{courtId}/deactivate` → 200 (idempotente)

## Disponibilidad

### `GET /courts/{courtId}/availability?date=2026-11-20` → 200
```json
{
  "courtId": 10,
  "date": "2026-11-20",
  "slots": [
    { "start": "2026-11-20T08:00:00", "end": "2026-11-20T09:30:00", "available": true,  "peak": false, "price": "18.00" },
    { "start": "2026-11-20T18:30:00", "end": "2026-11-20T20:00:00", "available": false, "peak": true,  "price": "28.00" }
  ]
}
```
- Las franjas ya pasadas de hoy aparecen con `available: false`.
- `date` fuera de `[hoy, hoy + 14]` → 422 `BOOKING_TOO_FAR_IN_ADVANCE` o `SLOT_IN_PAST`. Decide y documenta; mi preferencia: devolver la lista con todo `available: false` para fechas pasadas y 422 solo para fechas demasiado lejanas.
- Errores: 400 (falta `date`), 404 pista.

## Reservas

### `POST /reservations` → 201
Cabecera `X-User-Id`.
```json
{ "courtId": 10, "start": "2026-11-20T18:30:00" }
```
Respuesta y `Location: /api/v1/reservations/{id}`:
```json
{
  "id": 100, "courtId": 10, "userId": "3f2b8c1e-7a4d-4e9b-9c61-2d5e8f0a1b7c",
  "start": "2026-11-20T18:30:00", "end": "2026-11-20T20:00:00",
  "price": "28.00", "status": "CONFIRMED", "penalty": "0.00",
  "createdAt": "2026-11-18T09:12:03.123", "cancelledAt": null
}
```
Errores:

| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | cuerpo inválido |
| 404 | `USER_NOT_FOUND` / `COURT_NOT_FOUND` | |
| 409 | `SLOT_ALREADY_BOOKED` | franja ocupada (incluida carrera) |
| 409 | `CONCURRENT_MODIFICATION` | conflicto optimista; el cliente puede reintentar |
| 422 | `INVALID_SLOT` | no alineada o fuera de horario |
| 422 | `SLOT_IN_PAST` | |
| 422 | `BOOKING_TOO_FAR_IN_ADVANCE` | |
| 422 | `COURT_INACTIVE` | |
| 422 | `MAX_ACTIVE_RESERVATIONS` | |

### `GET /reservations/{id}` → 200
Solo el propietario. Otro usuario → 404 (no revelar existencia). 

### `GET /reservations?status=ACTIVE|ALL` → 200
Reservas del usuario de `X-User-Id`, ordenadas por `start` ascendente (ACTIVE) o descendente (ALL). Por defecto `ACTIVE`.

### `POST /reservations/{id}/cancel` → 200
Devuelve la reserva con `status: CANCELLED`, `penalty` y `cancelledAt`.

| HTTP | Código | Cuándo |
|---|---|---|
| 404 | `RESERVATION_NOT_FOUND` | no existe o no es tuya |
| 409 | `CONCURRENT_MODIFICATION` | |
| 422 | `CANCELLATION_NOT_ALLOWED` | ya cancelada o ya empezada |

Por qué `POST .../cancel` y no `DELETE`: la reserva no desaparece, cambia de estado y devuelve un resultado (penalización).

## Formato de error (RFC 9457)

```json
{
  "type": "https://courtbook.dev/errors/slot-already-booked",
  "title": "Slot already booked",
  "status": 409,
  "detail": "Court 10 is already booked at 2026-11-20T18:30",
  "instance": "/api/v1/reservations",
  "code": "SLOT_ALREADY_BOOKED"
}
```
Errores de validación añaden `"errors": [{ "field": "courtId", "message": "must not be null" }]`.

Nunca devuelvas un stack trace ni un 500 por un error de negocio. Un 500 en los tests es un bug.
