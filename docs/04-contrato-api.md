# 04 – Contrato de la API

Base: `/api/v1`. JSON. Fechas ISO-8601 sin zona (`2026-11-20T18:00:00`), siempre `Europe/Madrid`. Importes como string decimal (`"12.50"`) para no perder precisión.

Cabecera de identidad (fase 1, hasta que haya login): `X-User-Id: <uuid>` en los endpoints de reservas y en los de admin. Si falta o no es un UUID válido → 400. Si el usuario no existe → 404. Si el endpoint es de admin y el usuario no lo es → 403 `ADMIN_REQUIRED`.

Endpoints de admin: `POST /clubs`, `POST /clubs/{id}/courts`, `POST /courts/{id}/deactivate`, `POST /admin/invitations`.

## Usuarios

### `POST /users` → 201
Público (sin cabecera).
```json
{ "firstName": "Alex", "lastName": "Izquierdo", "email": "alex@example.com", "password": "unaClaveLarga" }
```
Validación: nombres de 1 a 100 caracteres, `email` válido, `password` con las reglas de D14 (la validación real la hace `PlainPassword`; en el DTO basta con `@NotBlank`). **No hay campo `role`**: si llega, se ignora y el usuario es siempre `PLAYER`.
Respuesta y `Location: /api/v1/users/{id}`:
```json
{ "id": "3f2b8c1e-7a4d-4e9b-9c61-2d5e8f0a1b7c", "firstName": "Alex", "lastName": "Izquierdo", "email": "alex@example.com", "role": "PLAYER" }
```
Nunca devuelvas `password` ni `passwordHash`, en ninguna respuesta.
Errores: 400, 409 `EMAIL_ALREADY_REGISTERED`.

### `GET /users/{id}` → 200 | 404

## Invitaciones de ADMIN

### `POST /admin/invitations` → 201 (admin)
```json
{ "email": "nuevo.admin@example.com" }
```
Respuesta, **sin el token** (el token solo sale por `InvitationNotifier`, que en la fase 1 lo escribe en el log):
```json
{ "id": "9a1c...", "email": "nuevo.admin@example.com", "status": "PENDING", "expiresAt": "2026-11-21T12:00:00" }
```
Errores: 400, 403 `ADMIN_REQUIRED`, 409 `EMAIL_ALREADY_REGISTERED`.

### `POST /admin/invitations/accept` → 201
Público: el token es la credencial.
```json
{ "token": "q3J0...base64url", "firstName": "Ana", "lastName": "García", "password": "otraClaveLarga" }
```
Respuesta: el usuario creado, con `role: ADMIN`. El email sale de la invitación.

| HTTP | Código | Cuándo |
|---|---|---|
| 404 | `INVITATION_NOT_FOUND` | token desconocido |
| 409 | `CONCURRENT_MODIFICATION` | dos aceptaciones simultáneas |
| 409 | `EMAIL_ALREADY_REGISTERED` | el email se registró después de invitar |
| 422 | `INVITATION_EXPIRED` | |
| 422 | `INVITATION_NOT_PENDING` | ya aceptada o revocada |

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
Errores: 400 (incluido `closesAt <= opensAt`), 403 `ADMIN_REQUIRED`, 409 `CLUB_NAME_ALREADY_EXISTS`.

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
Errores: 400, 403 `ADMIN_REQUIRED`, 404 `CLUB_NOT_FOUND`, 409 `COURT_NAME_ALREADY_EXISTS`.

### `GET /clubs/{clubId}/courts` → 200
### `POST /courts/{courtId}/deactivate` → 200 (idempotente)
Errores: 403 `ADMIN_REQUIRED`, 404 `COURT_NOT_FOUND`.

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

## Mapeo de excepciones a HTTP

| Tipo | HTTP |
|---|---|
| Bean Validation del DTO | 400 `VALIDATION_ERROR` |
| Validación de value objects (`INVALID_EMAIL`, `INVALID_PASSWORD`, `INVALID_USER`) | 400 |
| `*_NOT_FOUND` | 404 |
| `ADMIN_REQUIRED` | 403 |
| `*_ALREADY_*`, `SLOT_ALREADY_BOOKED`, `CONCURRENT_MODIFICATION` | 409 |
| Reglas de negocio (`INVALID_SLOT`, `SLOT_IN_PAST`, `MAX_ACTIVE_RESERVATIONS`, `INVITATION_EXPIRED`...) | 422 |

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
