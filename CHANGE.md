## Spotiboost v0.1.5

<!-- lang:en -->
### What's new

- Lyrics: no more big empty gaps above or below the lines, the lines use the full width, and the header fits the screen. Songs without lyrics show "No lyrics" over the dimmed cover instead of a black screen.
- Lyrics full screen now really is full screen: no black bar on top, the header sits clear of the camera, and Back leaves it.
- Fixed the full-screen "now playing" view, which stopped opening after a Spotify change.
- Only one Spotify Connect session: if the previous run is still listed as the active device, playback is pulled to the current one, so "Web Player (Firefox)" no longer shows up twice.
- Sign in with Google, Facebook or Apple is available again, and the system's autofill can fill in saved logins.
- Signed-out home: the "Sample of Spotify" banner is compact and the disabled player is hidden.
- Hover tooltips are gone, they only got in the way on a touch screen.
- A spinner shows on the play button while a song is loading.

<!-- lang:es -->
### Novedades

- Letras: sin grandes espacios vacíos arriba ni abajo de la letra, las líneas usan todo el ancho y la cabecera cabe en la pantalla. Las canciones sin letra muestran "Sin letra" sobre la portada oscurecida en vez de una pantalla negra.
- La pantalla completa de la letra ahora sí es pantalla completa: sin barra negra arriba, la cabecera queda libre de la cámara y Atrás sale de ella.
- Corregida la vista de reproducción a pantalla completa, que dejó de abrirse tras un cambio de Spotify.
- Una sola sesión de Spotify Connect: si la ejecución anterior sigue como dispositivo activo, la reproducción se trae a la actual, así "Web Player (Firefox)" ya no aparece dos veces.
- Vuelve a estar disponible el inicio de sesión con Google, Facebook o Apple, y el autocompletado del sistema puede rellenar los datos guardados.
- Inicio sin sesión: el banner "Muestra de Spotify" es compacto y se oculta el reproductor deshabilitado.
- Se quitaron los globos de ayuda al pasar el cursor, que solo estorbaban en pantalla táctil.
- Aparece un indicador de carga en el botón de play mientras una canción se está cargando.

## Spotiboost v0.1.4

<!-- lang:en -->
### What's new

- Fixed the full-screen "now playing" view: Spotify renamed part of its player bar, so tapping the cover no longer opened it. The view, and its devices, lyrics and queue buttons, work again.
- Lyrics: removed the big empty gap above the lines; the spacing now scales with the screen height.
- The full-screen button on the lyrics now works: the lyrics fill the whole screen and Back leaves full screen.

<!-- lang:es -->
### Novedades

- Corregida la vista de reproducción a pantalla completa: Spotify renombró una parte de su barra del reproductor y al tocar la portada ya no se abría. La vista y sus botones de dispositivos, letra y cola funcionan de nuevo.
- Letras: se quitó el gran espacio vacío sobre la letra; el espaciado ahora se adapta a la altura de la pantalla.
- El botón de pantalla completa de la letra ya funciona: la letra ocupa toda la pantalla y Atrás sale de ella.

## Spotiboost v0.1.3

<!-- lang:en -->
### What's new

- **Experimental:** audio ad blocker. When an ad starts, it is muted and played out at once, so the next song starts within a second or two. It relies on Spotify's internal player objects, so a Spotify update may break it; the app keeps working normally if it does.
- It only covers audio ads. Banners and other visual ads are not hidden yet.
- The update prompt has a new Spotify-style look and shows a progress bar with the percentage while the APK downloads.

<!-- lang:es -->
### Novedades

- **Experimental:** bloqueador de anuncios de audio. Cuando empieza un anuncio se silencia y se reproduce de golpe, así la siguiente canción arranca en uno o dos segundos. Depende de objetos internos del reproductor de Spotify, así que una actualización de Spotify puede romperlo; si pasa, la app sigue funcionando con normalidad.
- Solo cubre anuncios de audio. Los banners y otros anuncios visuales aún no se ocultan.
- El aviso de actualización tiene un nuevo diseño al estilo Spotify y muestra una barra de progreso con el porcentaje mientras se descarga el APK.

## Spotiboost v0.1.2

<!-- lang:en -->
### What's new

- English and Spanish: every text of the app follows the phone's language (update prompt, notification and battery-saver warnings, media notification buttons, bottom bar and expanded player).
- The update prompt shows the release notes in your language, with headings, bullets, **bold**, `code` and links.
- Release notes live in `CHANGE.md` (English and Spanish blocks) and the release workflow publishes the section of the tagged version.

<!-- lang:es -->
### Novedades

- Inglés y español: todos los textos de la app siguen el idioma del teléfono (aviso de actualización, avisos de notificaciones y ahorro de energía, botones de la notificación de música, barra inferior y reproductor ampliado).
- El aviso de actualización muestra las notas del release en tu idioma, con encabezados, viñetas, **negritas**, `código` y enlaces.
- Las notas de cada versión están en `CHANGE.md` (bloques en inglés y español) y el workflow del release publica la sección de la etiqueta.

## Spotiboost v0.1.1

<!-- lang:en -->
### What's new

- Releases are signed with a fixed key, so every version installs over the previous one.
- The app looks for a newer version on GitHub when it opens and offers to download and install it.

<!-- lang:es -->
### Novedades

- Los releases se firman con una clave fija, así cada versión se instala encima de la anterior.
- La app busca una versión nueva en GitHub al abrirse y ofrece descargarla e instalarla.
