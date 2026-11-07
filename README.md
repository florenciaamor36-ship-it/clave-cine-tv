# Clave Cine TV

App Android de La Clave Argentina para buscar contenido público de YouTube, incluidas transmisiones en vivo activas, y abrir su reproducción en la app oficial de YouTube.

## Estado de esta base

- Aplicación Android independiente; no modifica Clave Video ni su repositorio.
- Consulta de YouTube Data API v3 para búsqueda de videos y transmisiones activas (`eventType=live`).
- Los resultados se abren en la aplicación o web oficial de YouTube, conservando su reproducción, anuncios, branding y restricciones.
- La clave API se introduce en el dispositivo y se guarda en preferencias privadas; para distribución hay que crear clave con restricciones Android (package `ar.com.laclave.cinetv` más huella SHA-1) y configurar cuota.
- No incluye películas, enlaces, canales, credenciales ni catálogo propios.
- Búsqueda no significa que una película sea legalmente gratuita ni disponible para todos; resultados dependen de YouTube y su región/restricciones.
- Esta versión no da acceso a películas compradas/alquiladas desde reproductor propio: esas se reproducen en YouTube con la cuenta que las adquirió.
- Sin reproductor que extraiga streams ni función de descarga.

## Requisitos y construcción

- JDK 17
- Android SDK / Android Gradle Plugin 8.9.1
- Gradle 8.11+ instalado (CI usa Gradle instalado por runner)

```sh
gradle --no-daemon assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. El paquete de prueba usa el sufijo `.debug`.

## Uso

Configurar una clave de YouTube Data API v3 restringida al paquete y huella SHA-1 de firma de esta aplicación. Buscar videos o transmisiones activas y tocar un resultado para abrirlo en YouTube. La búsqueda consume cuota de la API; la cuota y disponibilidad dependen del proyecto de Google Cloud.

No hay catálogo separado para películas alquiladas o compradas ni reproducción fuera de YouTube. No se descargan ni guardan videos de YouTube.

## Privacidad y seguridad

- No hay cuenta ni backend en esta base.
- La clave se guarda en preferencias privadas del dispositivo, pero los secretos embebidos/guardados en un cliente no son totalmente secretos; restringí la clave por aplicación Android y cuotas en Google Cloud, y considera interponer backend antes de una distribución amplia.
- Las consultas a YouTube transmiten el texto de búsqueda, la clave API y datos de red directamente al API oficial de Google.
- Revisar y añadir política de privacidad antes de distribución pública, en conformidad con los requisitos de YouTube API Services.

## Créditos, términos y licencias

Este proyecto no incorpora código fuente de LibreTube. Usa YouTube Data API y dirige al reproductor oficial de YouTube. Antes de distribución hay que mostrar atribución/branding de YouTube, enlazar sus Términos de Servicio, publicar una política de privacidad y cumplir los YouTube API Services Terms/Developer Policies. La API y el contenido de YouTube no conceden derechos para copiar, almacenar, descargar o reproducir por fuera de sus métodos oficiales. AndroidX/Compose se distribuyen bajo sus respectivas licencias; revisar notices de dependencias en una distribución final.

Referencias oficiales:
- API search: https://developers.google.com/youtube/v3/docs/search/list
- Developer policies: https://developers.google.com/youtube/terms/developer-policies
- Required minimum functionality / Referer Android: https://developers.google.com/youtube/terms/required-minimum-functionality
- Condiciones de API: https://developers.google.com/youtube/terms/api-services-terms-of-service
- Películas compradas/alquiladas: https://support.google.com/youtube/answer/9679194?hl=en
