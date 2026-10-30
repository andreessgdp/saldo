# Saldo

App Android para apuntar gastos e ingresos en segundos y revisar el mes. Todo se guarda en el móvil, sin servidor.

## Qué hace

- **Registro rápido**: teclado numérico propio, categoría, cuenta, fecha (hoy / ayer / otra) y nota opcional.
- **Movimientos** por mes, agrupados por día, con ingresos, gastos y balance arriba.
- **Resumen**: donut de gasto por categoría, presupuestos con barra de progreso, barras de los últimos 6 meses, comparativa con el mes anterior, media mensual y tasa de ahorro.
- **Cuentas**: Tarjeta, Efectivo, Ahorro (editables), saldo de cada una, patrimonio total y transferencias entre cuentas.
- **Recurrentes**: nómina, renting, gym, suscripciones... se apuntan solos el día que toca.
- **Presupuesto mensual** por categoría (Ajustes → Categorías).
- **Copia de seguridad**: exportar / restaurar JSON y exportar CSV para Excel.
- **Widgets**: "Resumen del mes" (gastado, balance, presupuesto y botones) y "Añadir rápido" (2×1).
- **Atajos**: mantén pulsado el icono → Nuevo gasto / Nuevo ingreso / Transferencia.

Desde un widget o atajo, al guardar vuelve directamente al escritorio.

## Cómo conseguir el APK

### Opción A — GitHub (sin instalar nada)
1. Crea un repo (privado) en GitHub y sube esta carpeta a la rama `main`.
2. La Action **Build APK** compila sola (~4 min).
3. Desde el móvil: GitHub → tu repo → **Releases** → descarga `saldo-v1.0.X.apk` → instalar (acepta "instalar apps desconocidas" para el navegador).

Cada push genera una versión nueva que se instala **encima** de la anterior sin perder datos (todas se firman con la misma clave de `keystore/`).

### Opción B — Android Studio
Abre la carpeta, espera a que sincronice Gradle y pulsa ▶ con el móvil conectado por USB.

## Stack
Kotlin · Jetpack Compose (Material 3, colores dinámicos) · Room · Glance (widgets). minSdk 26.

> La clave de firma (`keystore/saldo.jks`, contraseña `saldo123`) está en el repo por comodidad: mantén el repo privado.
