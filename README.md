# OVERSHOP Android

Aplicación Android nativa para comercio de ropa urbana (Kotlin, XML, Firebase).

## Abrir y compilar
1. Abre la carpeta raíz en Android Studio y sincroniza Gradle.
2. Usa JDK 21 y Android SDK compatible con compileSdk 36.
3. Configura Firebase para el paquete `com.overshop.app` y coloca `app/google-services.json` (se incluye la configuración cliente del proyecto asociado).
4. Compila con `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).

## Estado conocido a 14-11-2026
- Acceso y registro de Comprador/Vendedor: Firebase Authentication y Firestore.
- Recuperación de contraseña: Firebase Authentication.
- Biométrico: verificación del sensor, aún no enlazada a una sesión Firebase.
- Órdenes: persistencia Firestore implementada en el código, debe probarse con las reglas de la base.
- Usuarios: consulta/edición; alta y eliminación necesitan backend confiable.
- Productos e imágenes: CRUD aún local, no conectado a Firestore/Storage.
- Carrito: estado en memoria.
- Ubicación GPS y pasarela de pago real: no implementadas.

No usar métodos de pago mostrados en la app como cobros reales. Antes de producción configura reglas restrictivas para Firestore/Storage, revisa autenticación de roles y prueba el flujo en teléfono físico.

## Estructura
`app/src/main/java/com/overshop/app` contiene pantallas, modelos, repositorios y lógica de autenticación; `app/src/main/res` contiene interfaces XML y recursos visuales.

## Credenciales y publicación
No incluyas credenciales privadas de Firebase Admin ni secretos de proveedores de pago. `google-services.json` es configuración de cliente y su protección depende de restricciones y reglas del proyecto Firebase. `local.properties`, cachés, APKs generados y `.git` no forman parte del ZIP de fuente.
