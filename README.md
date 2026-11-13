# Spotiboost

Cliente de Android para el reproductor web de Spotify (`open.spotify.com`), pensado como proyecto personal. Carga la web dentro de
[GeckoView](https://mozilla.github.io/geckoview/) (el motor de Firefox) y le inyecta una extensión que adapta la interfaz a un teléfono.

No está afiliado a Spotify. Necesitas tu propia cuenta; el audio se reproduce con el DRM Widevine del propio dispositivo.

## Qué hace
- Interfaz móvil: barra inferior (Inicio, Buscar, Biblioteca), reproductor ampliado, cola, dispositivos y letra a pantalla completa.
- Reproducción en segundo plano y con la pantalla apagada: servicio en primer plano, notificación y controles de la pantalla de bloqueo.
- Aviso si el ahorro de energía está activo o faltan las notificaciones, porque pueden cortar la música.

## Por qué GeckoView y no el WebView del sistema
En el WebView de Android el reproductor web de Spotify no llega a pedir el audio. En GeckoView sí, con tres ajustes
(permiso de DRM, autoplay y Service Workers desactivados, que rompían las portadas); están comentados en `MainActivity.java`.

## Licencia
MIT. GeckoView (MPL 2.0) se descarga como dependencia y no se incluye en este repositorio.

## Instalar
Descarga el APK de [Releases](../../releases). Solo arm64 (casi todos los teléfonos actuales).

## Compilar
Requiere JDK 17 y el SDK de Android (plataforma 36).

```sh
./gradlew assembleDebug     # app-debug.apk, con depuración y diagnóstico
./gradlew assembleRelease   # app-release.apk
```

La build debug se instala como "Spotiboost Debug" (`com.ufoex.spotiboost.gecko`), así que convive con la release (`com.ufoex.spotiboost`).

## Publicar una versión
Crea una etiqueta y súbela; GitHub Actions compila y publica el APK en Releases:

```sh
git tag v0.1.0 && git push origin v0.1.0
```

Para firmar con tu propia clave, define en el repo los secretos `KEYSTORE_BASE64` (`base64 -w0 release.jks`), `KEYSTORE_PASSWORD`,
`KEY_ALIAS` y `KEY_PASSWORD`. Sin ellos, el release se firma con la clave de depuración: instala, pero no actualiza encima de otra firma.
