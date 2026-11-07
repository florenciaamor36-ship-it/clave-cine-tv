# Clave Cine TV

App Android de La Clave Argentina para organizar y reproducir películas, videos y canales en vivo desde listas M3U/M3U8 que la persona usuaria posee o está autorizada a utilizar.

## Estado de esta primera base

- Aplicación Android independiente; no modifica Clave Video ni su repositorio.
- Reproductor integrado con AndroidX Media3/ExoPlayer y soporte HLS.
- Importación de lista M3U/M3U8 desde archivo local o URL HTTPS.
- Búsqueda por nombre/categoría, filtros Inicio/En vivo/Películas, reproducción y persistencia local de la lista.
- No incluye catálogo de películas, URLs de canales, credenciales IPTV ni contenido de terceros.
- La clasificación inicial de canales/películas es heurística (nombre/grupo); puede requerir corrección para algunas listas.
- Este código todavía no tiene APK compilada ni prueba en dispositivo; el CI se añadirá al repositorio.

## Requisitos y construcción

- JDK 17
- Android SDK / Android Gradle Plugin 8.9.1
- Gradle 8.11+ instalado (CI usa Gradle instalado por runner)

```sh
gradle --no-daemon assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. El paquete de prueba usa el sufijo `.debug`.

## Uso

Importar archivo `.m3u`/`.m3u8` o pegar una URL HTTPS directa a una lista. La app filtra URLs no HTTP/HTTPS dentro de M3U. Las listas quedan localmente en el dispositivo. Al tocar un elemento, se inicia el reproductor integrado.

No se incluye descarga de películas ni televisión en esta versión. No usar para acceder a contenido sin autorización. Verificar derechos de retransmisión, licencias de las fuentes y leyes locales.

## Privacidad y seguridad

- Las listas importadas se guardan en preferencias privadas locales de la aplicación.
- No hay cuenta, telemetría propia ni backend en esta base.
- No importes listas con credenciales que no quieras guardar en este dispositivo. Para una versión futura, se puede mejorar el almacenamiento protegido y permitir borrar listas.
- Los enlaces de contenido pueden contener tokens; no se muestran ni se envían a ningún backend desde esta app.

## Créditos y licencias

El reproductor usa AndroidX Media3/ExoPlayer bajo licencias de Google/AndroidX (consulta notices de dependencias al distribuir). El proyecto es una app nueva; no copia el código de LibreTube. Si se incorporan componentes GPL de LibreTube en el futuro, habrá que revisar compatibilidad de licencias y publicar el código fuente correspondiente.
