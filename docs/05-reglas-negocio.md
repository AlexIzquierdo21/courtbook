# 05 – Reglas de negocio y casos límite

Cada caso marcado con ☐ es un test obligatorio. Los de dominio, sin Spring y con un `ClockPort` fijo. Parámetros por defecto: ver `00-decisiones.md`.

## R1 – Franjas válidas

Una franja es válida si empieza en `opensAt + n × slotMinutes`, dura exactamente `slotMinutes` y termina como muy tarde en `closesAt`.

Ejemplo: club 08:00–23:00, pádel 90 min → 08:00, 09:30, …, 21:30 (termina 23:00). 10 franjas.

- ☐ Franja que empieza a las 08:00 → válida.
- ☐ Última franja que termina exactamente al cierre → válida.
- ☐ Franja que termina 1 min después del cierre → inválida.
- ☐ Inicio no alineado (08:15) → `INVALID_SLOT`.
- ☐ Inicio antes de apertura → `INVALID_SLOT`.
- ☐ Horario que no divide exacto (08:00–22:00 con 90 min): 9 franjas, la última 20:00–21:30; los 30 min sobrantes no generan franja parcial.
- ☐ Tenis 60 min en el mismo club → 15 franjas.

## R2 – Sin solapes

No puede haber dos reservas `CONFIRMED` en la misma pista y franja.

- ☐ Reservar una franja libre → OK.
- ☐ Reservar una franja ocupada → `SLOT_ALREADY_BOOKED`.
- ☐ Reservar una franja cuya única reserva está `CANCELLED` → OK.
- ☐ Misma hora en otra pista del club → OK.
- ☐ (Integración) Insertar dos `CONFIRMED` iguales directamente en BD → viola `uk_reservation_court_slot`.
- ☐ (Concurrencia) N hilos reservan la misma franja a la vez → exactamente 1 éxito, el resto 409.

## R3 – Ventana de reserva

- ☐ Franja que ya ha empezado (start < now) → `SLOT_IN_PAST`.
- ☐ start == now → `SLOT_IN_PAST` (decisión: hay que reservar antes de que empiece).
- ☐ Fecha = hoy + 14 días → OK.
- ☐ Fecha = hoy + 15 días → `BOOKING_TOO_FAR_IN_ADVANCE`.
- ☐ Pista desactivada → `COURT_INACTIVE`.

## R4 – Máximo de reservas activas

Activa = `CONFIRMED` y `start > now`. Máximo 3.

- ☐ Con 2 activas, reservar → OK (pasa a 3).
- ☐ Con 3 activas, reservar → `MAX_ACTIVE_RESERVATIONS`.
- ☐ Con 3 confirmadas de las que 1 ya ha pasado → OK (solo 2 activas).
- ☐ Con 3 activas, cancelar una y reservar → OK.
- ☐ Una reserva en curso (empezó hace 10 min) no cuenta como activa.
- ☐ (Concurrencia) Usuario con 2 activas lanza 2 reservas en paralelo en franjas distintas → como mucho 1 éxito.

## R5 – Precio punta/valle

La franja es punta si su **inicio** está dentro de alguna `PeakWindow` del club (`from ≤ inicio < to`) en ese día de la semana.

- ☐ Martes 18:00 → punta.
- ☐ Pista de 60 min, martes 17:00 → valle aunque termine a las 18:00.
- ☐ Martes 22:00 → valle (`to` es exclusivo).
- ☐ Sábado 09:00 → punta; sábado 18:00 → valle.
- ☐ Club sin ventanas punta → siempre valle.
- ☐ El precio queda congelado: cambiar el precio de la pista (aunque no haya endpoint, pruébalo en dominio/BD) no altera reservas existentes.

## R6 – Cancelación y penalización

- Antelación = `start - now`.
- Antelación ≥ 24 h → `penalty = 0`.
- 0 < antelación < 24 h → `penalty = 50 % de price`, redondeo `HALF_UP` a 2 decimales.
- Antelación ≤ 0 (ya empezó o pasó) → `CANCELLATION_NOT_ALLOWED`.

- ☐ Exactamente 24 h antes → sin penalización.
- ☐ 23 h 59 min antes → penalización.
- ☐ 1 min antes → penalización.
- ☐ En el minuto de inicio → `CANCELLATION_NOT_ALLOWED`.
- ☐ Precio 27.75 → penalización 13.88 (redondeo).
- ☐ Cancelar una reserva ya cancelada → `CANCELLATION_NOT_ALLOWED`.
- ☐ Cancelar una reserva ajena → `RESERVATION_NOT_FOUND` (404).
- ☐ Tras cancelar, la franja vuelve a aparecer libre en disponibilidad.

## R7 – Usuarios y registro

- ☐ Email duplicado (incluso con mayúsculas distintas) → `EMAIL_ALREADY_REGISTERED`.
- ☐ Email inválido → 400 `INVALID_EMAIL`.
- ☐ Registro → siempre `PLAYER`, aunque la petición traiga `"role": "ADMIN"`.
- ☐ Contraseña de 7 caracteres → `INVALID_PASSWORD`. De 8 → OK. De 72 bytes → OK. De 73 bytes → `INVALID_PASSWORD` (cuenta bytes: `ñ` son 2).
- ☐ Sin mayúscula, minúscula, dígito o carácter especial, o con algún espacio (incluido U+00A0) → `INVALID_PASSWORD`.
- ☐ Lo que se guarda es el hash: tras registrar, `passwordHasher.matches(plain, user.passwordHash())` es true y el hash no es igual al texto plano.
- ☐ Ninguna respuesta JSON contiene `password` ni `passwordHash`.
- ☐ `PlainPassword.toString()` no contiene el valor.
- ☐ Petición de admin con un `X-User-Id` de `PLAYER` → 403 `ADMIN_REQUIRED`.

## R9 – Invitaciones de ADMIN

Caducidad 72 h (configurable). Un solo uso. Solo se guarda el hash del token.

- ☐ Un `PLAYER` intenta invitar → `ADMIN_REQUIRED`.
- ☐ Invitar a un email ya registrado → `EMAIL_ALREADY_REGISTERED`.
- ☐ Invitar → `PENDING`, `expiresAt = now + 72h`, se llama una vez a `InvitationNotifier` con el token en claro, y en el repositorio solo está el hash.
- ☐ Invitar dos veces al mismo email → la primera pasa a `REVOKED` y solo funciona el token nuevo.
- ☐ Aceptar con token válido → usuario `ADMIN` con el email de la invitación; invitación `ACCEPTED` con `acceptedAt`.
- ☐ Aceptar a las 71 h 59 min → OK. Exactamente a las 72 h → `INVITATION_EXPIRED`.
- ☐ Aceptar dos veces el mismo token → la segunda da `INVITATION_NOT_PENDING`.
- ☐ Aceptar un token revocado → `INVITATION_NOT_PENDING`.
- ☐ Token inventado → `INVITATION_NOT_FOUND`.
- ☐ El email se registró como `PLAYER` después de invitar → al aceptar, `EMAIL_ALREADY_REGISTERED`. Promocionar a un jugador existente queda fuera de alcance.
- ☐ (Concurrencia) Dos aceptaciones simultáneas del mismo token → se crea un solo `ADMIN`.

## R10 – Primer ADMIN (bootstrap)

- ☐ No hay ningún `ADMIN` y las propiedades están definidas → al arrancar se crea un `ADMIN`.
- ☐ Ya existe un `ADMIN` → no se crea nada, aunque el email configurado sea otro.
- ☐ Sin propiedades → no hace nada, y lo deja en el log como `WARN`.
- ☐ Arrancar dos veces seguidas → sigue habiendo un solo admin de bootstrap (idempotente).
- ☐ La contraseña de bootstrap no aparece en el log.

## R8 – Notificaciones

- ☐ Al crear una reserva se llama a `NotificationPort.reservationConfirmed` una vez.
- ☐ Al cancelar se llama a `reservationCancelled` una vez.
- ☐ Si la reserva falla (cualquier regla), no se notifica nada.
- ☐ Si el adaptador de notificación lanza excepción, la reserva queda guardada igualmente.

Pregunta para que la pienses (no la implementes aún): la notificación se envía dentro de la transacción. ¿Qué pasa si la notificación sale y luego el commit falla? ¿Cómo lo resolverías? (Pista: eventos tras commit / outbox.)
