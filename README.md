# RegionVisualizer NEW
Nueva versión de mi mod anterior, una versión super mejorada a la anterior, utilizando regiones con músicas desde tu .minecraft/music, puedes crear regiones, añadirles su música, loop y transición con fade in/fade-out en (true/false). Todo esto está hecho sin el motor de sonido de Minecraft, el cual es OpenAL LWJGL. En este caso, se utiliza el paquete de Java Sound Sampled API, así evitando romper el sistema de sonidos de Minecraft.

## ¿Por qué desarrolle esté mod?
Yo deseaba tener un mod que manejase regiones, tenga opción de música, añadirla desde .minecraft/music (o cualquier nombre de carpeta) y que no utilizase el mismo sistema de sonido de Minecraft, que por lo tanto ninguno de los mods que estaban en Curseforge o Modrinth estaban disponibles, no existian, solo existían la música a partir de detección de eventos, entrada de biomas, agua, combates y eso. Tal cosa que yo no quería, deseo un mapa personalizado el cual tenga su propia música y no de algo tan laborioso de detección de X evento. Por lo tanto, conoci un mod llamado Dynamic Ambience and Music, el cual era de la 1.12.2. Tuve una idea, y es reimplementar todo sobre ese mod a mi forma, crear el mod desde cero y basarme a su idea, todo se manejaría por comandos, desde una carpeta almacenada en .minecraft/music y unas configuraciones personalizables en .minecraft/config, que por lo tanto, lo quisé hacer en realidad, y aquí estamos. Si gustas leer este texto, te doy el gusto y lo agradezco. Esta fue mi motivación de crear el mod, crear algo que existia antes, pero que nadie lo quizó HACER, el que yo hice en realidad.

PD: El mod funciona tanto para cliente como servidor, por lo tanto: **Clientside & Serverside**


## COMANDOS

### /region
– **/region add (add) (filemusic) (loop-> true/false) (fade-> true/false)** - Defines una región con música, con sus propiedades tipo fade y loop opcional.

– **/region cancel** - Se cancela la selección hecha con el item Region Selector.

– **/region here** - Muestra en qué región estás actualmente en la posición del mundo.

– **/region info (name)** - Muestra información de X región del mundo.

– **/region list** - Muestra las regiones creadas en el mundo.

– **/region remove (nameregion)** - Elimina una región del mundo añadida.

– **/region tp (region)** - Teletransportación hacia X región en el mundo.

### /playmusic
– **/playmusic (player) (volume -> 1.0 to 0.0)** - Cambia el volumen de X jugador.

– **/playmusic (player) stop** -> Para la música actual activada.

– **/playmusic (player) (filemusic.wav)** - Reproduce la música almacenada en .minecraft/music

– **/playmusic (player) list** - Envía la lista de música almacenada al jugador.

– **/playmusic (player) getvolume** - Volumen del jugador, la envía hacia el jugador.

### /regedit
– **/regedit clear** -> Se limpia la visualización de X región.

– **/regedit music (region) (filemusic)** -> Cambia la música indicada anteriormente en la región.

– **/regedit pos (region)** -> Permite reposicionar la región (cualquier sugerencia avisen, falta pulir esta parte).

– **/regedit regname (region) (newRegionTag)** -> Cambia el nombre de X región.

– **/regedit view (region)** -> Permite visualizar X región (solo se permite uno a la vez).


## ACTUALIZACIONES
### 1.0.1
1. Músicas en regiones añadidas.
2. Nuevos comandos añadidos -> /playmusic
3. Nuevos subcomandos añadidos -> /region
4. Configuración de música añadida en "Opciones.../Música y sonido.../Configuración de RegionVisualizer"

  – Control de volumen añadido

5. Bugs de regiones solucionados para el multijugador.
 
### 1.1.0
1. Nuevos comandos añadidos -> /regedit
2. Mejoras entre la compatibilidad de formatos de audio -> .OGG añadido
3. Sistema de cache añadido -> Todo el cache se almacena en .minecraft/music/cache, puedes eliminarlo sin problema alguno.

  – Capacidad de Disco -> 0/100 archivos .ogg

  – Capacidad de RAM -> 0/4 archivos .ogg

  – Filtrado de música: .WAV

5. Bugs solucionados (bug visual, bug ), mejoras añadidas y comandos ordenados -> "/playmusic" ordenado, "/region" ordenado.
6. Permitir visualizar X región solo al sostener la varita Region Selector.
7. Mejora de configuración -> Se ubica en: Opciones.../Música y sonido.../Configuración de RegionVisualizer:

  – Reproducción de música añadida.

  – Control de volumen mejorado.

  – Control de Fade mejorado.

  – Control del sonido verdadero añadido.

  – Control del comienzo de Fade añadido (45% por defecto).

  – Debug intenso.

  – Refresh de música añadido.

  – (En mantenimiento el abierto automático de la carpeta "music" del botón Info...).

8. Mensajes DEBUG en el chat eliminados -> Por una queja de molestia en el chat a la pantalla.
9. Mensajes mejorados por "sendOverlayMessage" añadido.
10. Comandos mejorados con más información.
11. Comandos con sugerencia añadidas (las músicas no se muestran en multijugador).

### FALTA POR AÑADIR/MODIFICAR
1. Superposición de regiones (Música no detectada) -> Solución temporal: Añadir primero las regiones pequeñas y luego la más grande.
2. Mejora visual en la configuración del mod (lista por hacerla más visual).
3. Permitir que la opción "📁 Info" te lleve a la carpeta principal del mod (.minecraft/music).
4. Mejoras en el Fade.
5. Mejoras en los textos con Emoji.
6. Eliminar DEBUGs (consola) innecesarios.
7. Mejorar la visualización de la región con una capa transparente con color verde o rojo según el jugador esté dentro o fuera.
8. Permitir al operador desactivar las opciones en cliente desde: "Opciones.../Música y sonido.../Configuración de RegionVisualizer" solo a comando -> /configmusic (PARA SERVIDORES DEDICADOS).


## Agradecimientos
**Liray** – Autor del mod "Dynamic Ambience and Music" y creador del sprite animado utilizado en este mod (Region Selector).
"La idea original de música dinámica por regiones me inspiró a reimplementarla en la versión 1.20.1 con mi propio enfoque y desde cero"

**Página en CurseForge:** [Dynamic Ambience and Music](https://www.curseforge.com/minecraft/mc-mods/dynamic-ambience-and-music)

**GitHub:** [liray-dev/Dynamic-Ambience-And-Music](https://github.com/liray-dev/Dynamic-Ambience-And-Music/tree/master)



**Arisitva Studio** – Estudio de desarrollo de proyectos tipo servidor en Minecraft, Terraria y de diferentes tipos de juegos incluidos.
"Gracias al equipo del desarrollo me motivaron a terminar este mod hasta el final, cualquier error o bugs me reportaron ellos, el cual me permitio perfeccionar este mod casi al 100%." 

**Discord:** [Aristiva Studio](https://discord.gg/u9pq3raUXe)

**Twitter:** [Aristiva Studio](https://x.com/aristivastudio)

## LICENCIA
Todo esto esta reservado por la propiedad de Aristiva Studio y por Riburitu.

Cualquier copia, distribución, modificación o uso comercial de este mod sin autorización expresa del propietario o del estudio está prohibido. Cometer alguno de estos casos puede conllevar a consecuencias legales.


