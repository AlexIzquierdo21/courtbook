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

## R7 – Usuarios

- ☐ Email duplicado (incluso con mayúsculas distintas) → `EMAIL_ALREADY_REGISTERED`.
- ☐ Email inválido → 400.

## R8 – Notificaciones

- ☐ Al crear una reserva se llama a `NotificationPort.reservationConfirmed` una vez.
- ☐ Al cancelar se llama a `reservationCancelled` una vez.
- ☐ Si la reserva falla (cualquier regla), no se notifica nada.
- ☐ Si el adaptador de notificación lanza excepción, la reserva queda guardada igualmente.

Pregunta para que la pienses (no la implementes aún): la notificación se envía dentro de la transacción. ¿Qué pasa si la notificación sale y luego el commit falla? ¿Cómo lo resolverías? (Pista: eventos tras commit / outbox.)
