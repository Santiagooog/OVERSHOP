# OVERSHOP

OVERSHOP es una aplicación Android nativa, desarrollada en Kotlin, para una tienda de ropa urbana. Incluye flujos diferenciados para Comprador, Vendedor y Administrador.

## Funciones

- Registro e inicio de sesión con correo y contraseña, cierre de sesión y recuperación de contraseña mediante Firebase Authentication.
- Acceso biométrico local en dispositivos compatibles, después de iniciar sesión con Firebase.
- Catálogo en Cloud Firestore, filtros por categoría, búsqueda y productos destacados según ventas.
- Carrito e historial de pedidos persistidos en Firestore.
- Gestión de productos y pedidos para Vendedor y Administrador.
- Gestión de perfiles y creación de cuentas desde el panel de Administración.
- Captura de ubicación en checkout y guardado de la dirección en el perfil del Comprador.
- Flujo de compra académico: registra una orden de prueba en Firestore y no procesa pagos.

## Arquitectura

- `app/src/main/java/com/overshop/app`: Activities, Fragments, modelos, adaptadores y repositorios Kotlin. Las clases y las operaciones principales tienen comentarios KDoc.
- `app/src/main/res`: pantallas XML, estilos, cadenas y recursos visuales.
- `firestore.rules`: reglas de acceso para las colecciones Firestore.
- `storage.rules`: reglas preparadas para Storage cuando se habilite un bucket.
- `functions`: backend administrativo preparado para desplegarse como Cloud Functions.
- `entregables`: documento técnico, manual de usuario y ZIP limpio de código fuente.

## Requisitos y compilación

1. Abre la raíz del proyecto en Android Studio y sincroniza Gradle.
2. Usa Android SDK 36 y el JDK compatible con el Gradle Wrapper.
3. Configura `app/google-services.json` para el proyecto Firebase `overshop-5c97d` y el paquete `com.overshop.app`.
4. Habilita Email/Password en Firebase Authentication e inicializa Cloud Firestore.
5. Compila en Windows con `gradlew.bat assembleDebug`.

La compilación `assembleDebug` finalizó correctamente el 16 de noviembre de 2026. El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. La huella, permisos de ubicación y cámara deben validarse en un teléfono compatible.

## Estado de los servicios Firebase

El proyecto se mantiene en el plan Spark. Authentication y Firestore se usan en los flujos activos; las reglas de Firestore están desplegadas. Las compras son simuladas y no cobran dinero.

Cloud Storage no está habilitado en el plan actual, por lo que no se pueden subir imágenes nuevas desde cámara o galería. Los productos pueden mostrar imágenes predeterminadas o URLs públicas ya registradas. La eliminación de identidades de Firebase Authentication desde el panel requiere Cloud Functions, aún no desplegadas. Room contiene esquema y DAO, pero los flujos activos usan Firestore.

La primera cuenta Administrador se configura en Firebase Console: crea la identidad en Authentication y su perfil en `users/{uid}` con `role: "Administrador"`. El registro público de la aplicación solo permite Comprador y Vendedor.

## Entregables

- `entregables/Documento_Tecnico_OVERSHOP.docx`
- `entregables/Manual_Usuario_OVERSHOP.docx`
- `entregables/OVERSHOP_entrega_fuente.zip`

Antes de publicar el manual en un repositorio público, oculta los datos personales visibles en sus capturas. El video de lanzamiento se entrega por separado y debe durar como máximo un minuto, grabado en un teléfono Android físico.

## Repositorio

Repositorio público: <https://github.com/Santiagooog/OVERSHOP>
