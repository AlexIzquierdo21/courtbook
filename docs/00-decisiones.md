# 00 – Decisiones de producto

Valores por defecto que he fijado para poder especificar. Están **pendientes de tu confirmación**; si cambias alguno, actualiza este fichero y los documentos afectados.

| # | Decisión | Valor elegido | Por qué |
|---|---|---|---|
| D1 | Duración de franja | Configurable **por pista**: 90 min pádel, 60 min tenis. Una reserva = una franja. | Es lo habitual en clubes; reservar varias franjas complica sin enseñar nada nuevo. |
| D2 | Alineación de franjas | Las franjas se generan desde la hora de apertura del club en saltos de la duración de la pista. Solo se puede reservar una franja alineada. | Permite garantizar los no-solapes con un índice único (ver doc 03). |
| D3 | Horario del club | Un único horario para todos los días (p. ej. 08:00–23:00). La última franja debe terminar antes o justo al cierre. | Suficiente para fase 1. |
| D4 | Máximo de reservas activas | **3** por usuario (configurable). Activa = `CONFIRMED` y aún no ha empezado. | |
| D5 | Antelación máxima | **14 días** desde hoy (configurable). | Evita acaparar pistas. |
| D6 | Penalización | Cancelar con **menos de 24 h** antes del inicio: penalización del **50 %** del precio. Con 24 h o más: gratis. Una reserva ya empezada no se puede cancelar. | |
| D7 | Hora punta | Ventanas por club (día de la semana + hora inicio/fin). Por defecto: L–V 18:00–22:00 y S–D 09:00–14:00. Una franja es punta si **empieza** dentro de una ventana. | Regla simple y testeable. |
| D8 | Precio | Dos precios por pista y franja: valle y punta, en EUR. El precio se **congela** en la reserva al crearla. | Cambiar la tarifa no debe alterar reservas existentes. |
| D9 | Roles | `PLAYER` y `ADMIN`. En fase 1 solo se guardan; no se comprueban. En fase 2 (JWT) solo `ADMIN` gestiona clubes y pistas. | |
| D10 | Identificación del usuario (fase 1) | Cabecera `X-User-Id`. | Placeholder hasta JWT. |
| D11 | Zona horaria | Todo en `Europe/Madrid`, guardado como `DATETIME` (hora local). | Un solo país; evita conversiones en fase 1. |
| D12 | Pista desactivada | Una pista puede desactivarse; no admite nuevas reservas, las existentes se mantienen. | |
