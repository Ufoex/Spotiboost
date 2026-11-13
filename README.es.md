# Spotiboost

[English](README.md) · **Español**

## 🙋 ¿Tienes un problema o una sugerencia?

Cuéntanos: abre un issue y describe qué pasó o qué te gustaría.

[![Abrir un issue](https://img.shields.io/badge/Abrir%20un%20issue-D73A4A?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Ufoex/Spotiboost/issues/new/choose)

Spotiboost es un cliente de Android para el reproductor web de Spotify (`open.spotify.com`). Carga la web dentro de [GeckoView](https://mozilla.github.io/geckoview/) (el motor de Firefox) y le inyecta una extensión que adapta la interfaz a un teléfono.

[![Última versión](https://img.shields.io/github/v/release/Ufoex/Spotiboost?label=descargar)](https://github.com/Ufoex/Spotiboost/releases/latest)

<p align="middle">
    <a href='https://grev.shehryar.ae/?owner=Ufoex&repo=Spotiboost'><img alt='Conteo de descargas' height='40' src='https://img.shields.io/github/downloads/Ufoex/Spotiboost/total?style=for-the-badge&logo=github&label=Downloads&color=1DB954&labelColor=24292F'/></a>
</p>

📘 También disponible: [**Faceboost**](https://github.com/Ufoex/Faceboost), la misma idea para Facebook.
📸 Y [**Instaboost**](https://github.com/Ufoex/Instaboost), para Instagram.

## Funciones

- Interfaz móvil: barra inferior (Inicio, Buscar, Biblioteca), reproductor ampliado, cola, dispositivos y letra a pantalla completa.
- Reproducción en segundo plano y con la pantalla apagada: servicio en primer plano, notificación y controles de la pantalla de bloqueo.
- Inglés y español, según el idioma del teléfono.
- Aviso si el ahorro de energía está activo o faltan las notificaciones, porque pueden cortar la música.
- Búsqueda de actualizaciones: la app busca versiones nuevas aquí y muestra las notas en español o inglés.

No está afiliado a Spotify. Necesitas tu propia cuenta; el audio se reproduce con el DRM Widevine del propio dispositivo.

## Por qué GeckoView y no el WebView del sistema

En el WebView de Android el reproductor web de Spotify no llega a pedir el audio. En GeckoView sí, con tres ajustes (permiso de DRM, autoplay y Service Workers desactivados, que rompían las portadas); están comentados en `MainActivity.java`.

## Instalar

Descarga el APK de la [última versión](https://github.com/Ufoex/Spotiboost/releases/latest) y ábrelo. Solo arm64 (casi todos los teléfonos actuales). Las actualizaciones posteriores se pueden instalar desde la propia app.

## Compilar

Requiere JDK 17 y el SDK de Android (plataforma 36).

```sh
./gradlew assembleDebug     # app-debug.apk, con depuración y diagnóstico
./gradlew assembleRelease   # app-release.apk
```

La build debug se instala como "Spotiboost Debug" (`com.ufoex.spotiboost.gecko`), así que convive con la release (`com.ufoex.spotiboost`).

## Publicar una versión

1. Añade arriba de `CHANGE.md` una sección `## Spotiboost vX.Y.Z` con los bloques `<!-- lang:en -->` y `<!-- lang:es -->` (son las notas que muestra el release y el aviso de actualización de la app).
2. Crea la etiqueta y súbela; GitHub Actions pasa los tests, compila y publica el APK en Releases:

```sh
git tag vX.Y.Z && git push origin vX.Y.Z
```

La app busca al abrirse la última versión en GitHub y, si es más nueva que la instalada, ofrece descargarla e instalarla.

Para firmar con tu propia clave, define en el repo los secretos `KEYSTORE_BASE64` (`base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD`. Sin ellos, el release se firma con la clave de depuración: instala, pero no actualiza encima de otra firma.

## Licencia

MIT. GeckoView (MPL 2.0) se descarga como dependencia y no se incluye en este repositorio.

Spotiboost no está afiliado ni respaldado por Spotify. Spotify es una marca de Spotify AB.
