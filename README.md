# OVERSHOP

Aplicación Android nativa para una tienda de ropa urbana, desarrollada en Kotlin. La app usa Firebase Authentication y Cloud Firestore para cuentas, catálogo, carrito y pedidos. El estado de Firebase es importante para interpretar la entrega: el proyecto se mantiene en Spark por decisión del equipo, y algunas funciones quedan condicionadas al acceso a servicios que Spark no ofrece actualmente.

## Estado comprobado

- Compila con `gradlew.bat assembleDebug` (14 nov 2026; APK debug generado en `app/build/outputs/apk/debug/app-debug.apk`).
- Authentication por correo: registro de comprador/vendedor, inicio, cierre y recuperación por correo.
- Acceso biométrico local con sesión persistente de Firebase Auth.
- Catálogo, carrito, pedidos, historial, perfiles y coordenadas de entrega conectados a Cloud Firestore.
- Firestore Security Rules desplegadas en `overshop-5c97d`.
- Geolocalización de checkout disponible si el usuario concede el permiso de ubicación.
- Checkout simulado sin pasarela: registra el pedido con `paymentStatus: simulated` y no cobra dinero.

## Funciones que requieren habilitar servicios

Este proyecto se conserva en Firebase Spark. Firebase indica que Cloud Storage for Firebase no está disponible en Spark y que desplegar Cloud Functions requiere habilitar Blaze. Por ello, las imágenes nuevas no pueden persistirse en Firebase Storage y el CRUD privilegiado de cuentas Auth (crear/eliminar) sigue pendiente de desplegar. El checkout es una simulación académica: guarda pedidos en Firestore, pero no procesa cobros ni representa transacciones reales.

La edición de perfiles/cuentas existentes y cambios permitidos por las reglas se hacen desde Firestore; esto no equivale a crear o borrar el usuario de Firebase Authentication. El registro público solo debe ofrecer Comprador y Vendedor. Nunca se debe permitir que una persona se otorgue el rol Administrador desde la app.

## Compilar e instalar

1. Abre la raíz del proyecto en Android Studio y sincroniza Gradle.
2. Usa JDK compatible con Gradle del wrapper y Android SDK `compileSdk 36`.
3. Verifica `app/google-services.json` y el identificador `com.overshop.app`.
4. Compila con `gradlew.bat assembleDebug` en Windows.
5. Instala el APK debug en dispositivo Android físico para validar cámara, huella y ubicación.

## Configuración de Firebase disponible

1. En Firebase Authentication, habilita Email/Password.
2. Inicializa Cloud Firestore.
3. Las reglas se despliegan con `firebase deploy --only firestore:rules --project overshop-5c97d`.
4. Crea manualmente la primera cuenta Administrador en Authentication y su documento `users/{uid}` con `uid`, `fullName`, `email`, `phone` y `role: "Administrador"`.

No se inicializó Storage ni se desplegaron Functions porque ello requiere cambiar el proyecto a Blaze. Si el responsable cambia el plan más adelante, revise costos/límites, inicialice Storage y despliegue `storage.rules` y las funciones de administración. El flujo de pagos no está implementado ni habilitado: la compra es una simulación.

## Estructura

- `app/src/main/java/com/overshop/app`: Activities, Fragments, modelos y repositorios Kotlin.
- `app/src/main/res`: layouts, estilos y recursos visuales.
- `functions`: backend preparado para CRUD administrativo, pendiente de despliegue.
- `firestore.rules`, `storage.rules`: políticas de backend; las reglas de Storage están pendientes y no otorgan acceso mientras no haya bucket.
- `entregables`: documento técnico y manual de usuario.

La captura disponible es solo de la pantalla de inicio de sesión. Se deben agregar capturas originales de todos los flujos probados y grabar el video de lanzamiento (máximo 60 segundos) en un teléfono físico; no usar emulador para ese video. El usuario debe revisar estos límites antes de entregar para que documentos y demostración no afirmen capacidades que no están desplegadas.
