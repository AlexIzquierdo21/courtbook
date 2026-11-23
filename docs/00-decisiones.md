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
| D9 | Roles | `PLAYER` y `ADMIN`. Todo el mundo se registra como `PLAYER`. Solo un `ADMIN` puede crear `ADMIN`, mediante invitación (D15). Solo `ADMIN` crea clubes y pistas. | Decidido por Alex (2026-11-18). |
| D10 | Identificación del usuario (fase 1) | Cabecera `X-User-Id` (UUID). Los endpoints de `ADMIN` cargan ese usuario y comprueban su rol. | Placeholder hasta el login. **Es inseguro a propósito**: cualquiera puede mandar el UUID de un admin. No lo despliegues así. |
| D11 | Zona horaria | Todo en `Europe/Madrid`, guardado como `DATETIME` (hora local). | Un solo país; evita conversiones en fase 1. |
| D12 | Pista desactivada | Una pista puede desactivarse; no admite nuevas reservas, las existentes se mantienen. | |
| D13 | Identificadores | `UserId` es UUID, generado en el dominio (`UserId.generate()`). En MySQL, `BINARY(16)`. Club, pista y reserva siguen con `BIGINT AUTO_INCREMENT`. | Decidido por Alex. Ojo: MySQL **no tiene tipo `UUID`**. Y un UUIDv4 aleatorio como PK fragmenta el índice de InnoDB; a esta escala da igual, pero si quieres hacerlo bien usa UUIDv7 (ordenado por tiempo). |
| D14 | Registro | Con contraseña desde la fase 1, para todos. Contraseña: mínimo 8 caracteres, máximo 72 **bytes** en UTF-8 (límite de BCrypt), sin espacios, con al menos una mayúscula, una minúscula, un dígito y un carácter especial (así está implementado en `PlainPassword`). Se guarda solo el hash (BCrypt) a través del puerto `PasswordHasher`. | Decidido por Alex. Las reglas de composición y la prohibición de espacios van en contra de NIST SP 800-63B; se mantienen por decisión de Alex. Dependencia: `spring-security-crypto`, **no** el starter de Security. |
| D15 | Invitación de ADMIN | Un `ADMIN` invita por email. Token aleatorio de 32 bytes (Base64URL); en BD solo se guarda su SHA-256. Caduca a las **72 h**. Un solo uso. Una invitación nueva al mismo email revoca la pendiente anterior. No se puede invitar a un email ya registrado. | Ver R9. |
| D16 | Primer ADMIN | Seed por **configuración**, no por Flyway: propiedades `courtbook.bootstrap.admin.email` y `.password` (por variable de entorno). Al arrancar, si no existe ningún `ADMIN`, se crea; si ya existe, no hace nada. Sin propiedades, no hace nada. | En Flyway tendrías que commitear un hash de contraseña, igual en todos los entornos y para siempre. Ver R10. |
| D17 | Login | Fase 1: no hay login, solo registro con contraseña y `X-User-Id`. Fase 2: login con email y contraseña para todos (JWT). Fase 3: login con Google como segundo método, a través del puerto `ExternalIdentityVerifier` con un adaptador de Google. | Decidido por Alex (2026-11-18). Hacerlo en este orden te obliga a que el dominio no dependa de cómo se autentica nadie. |
