# RegionVisualizer NEW
Nueva versión de mi mod anterior, una versión super mejorada a la anterior, utilizando regiones con músicas desde tu .minecraft/music, puedes crear regiones, añadirles su música, loop y transición con fade in/fade-out en (true/false). Todo esto está hecho sin el motor de sonido de Minecraft, el cual es OpenAL LWJGL. En este caso, se utiliza el paquete de Java Sound Sampled API, así evitando romper el sistema de sonidos de Minecraft.

Incluye un control de volumen aparte desde Opciones --> Música y sonidos --> Configuración de RegionVisualizer --> Volumen del Mod: 100%.

## ¿Por qué desarrolle esté mod?
Yo deseaba tener un mod que manejase regiones, tenga opción de música, añadirla desde .minecraft/music (o cualquier nombre de carpeta) y que no utilizase el mismo sistema de sonido de Minecraft, que por lo tanto ninguno de los mods que estaban en Curseforge o Modrinth estaban disponibles, no existian, solo existían la música a partir de detección de eventos, entrada de biomas, agua, combates y eso. Tal cosa que yo no quería, deseo un mapa personalizado el cual tenga su propia música y no de algo tan laborioso de detección de X evento. Por lo tanto, conoci un mod llamado Dynamic Ambience and Music, el cual era de la 1.12.2. Tuve una idea, y es reimplementar todo sobre ese mod a mi forma, crear el mod desde cero y basarme a su idea, todo se manejaría por comandos, desde una carpeta almacenada en .minecraft/music y unas configuraciones personalizables en .minecraft/config, que por lo tanto, lo quisé hacer en realidad, y aquí estamos. Si gustas leer este texto, te doy el gusto y lo agradezco. Esta fue mi motivación de crear el mod, crear algo que existia antes, pero que nadie lo quizó HACER, el que yo hice en realidad.

PD: El mod funciona tanto para cliente como servidor, por lo tanto: **Clientside & Serverside**

## COMANDOS

### /region
– **/region add (add) (filemusic) (loop-> true/false) (fade-> true/false)** - Defines una región con música, con sus propiedades tipo fade y loop opcional.

– **/region remove (nameregion)** - Elimina una región del mundo añadida.

– **/region list** - Muestra las regiones creadas en el mundo.

– **/region tp (region)** - Teletransportación hacia X región en el mundo.

– **/region cancel** - Se cancela la selección hecha con el item Region Selector.

– **/region info (name)** - Muestra información de X región del mundo.

– **/region here** - Muestra en qué región estás actualmente en la posición del mundo.

### /playmusic
– **/playmusic (player) (volume -> 1.0 to 0.0)** - Cambia el volumen de X jugador (falta comprobar si funciona o no)

– **/playmusic (player) stop** -> Para la música actual activada.

– **/playmusic (player) (filemusic.wav)** - Reproduce la música almacenada en .minecraft/music

– **/playmusic (player) list** - Envía la lista de música almacenada al jugador.

– **/playmusic (player) getvolume** - Volumen del jugador (falta comprobar si funciona o no)

– **/playmusic (player) config** - Entra a las opciones del control de volumen (sera descartado a futuro por obvias razones)

## Agradecimientos
**Liray** – Autor del mod "Dynamic Ambience and Music" y creador del sprite animado utilizado en este mod (item X).
La idea original de música dinámica por regiones me inspiró a reimplementarla en la versión 1.20.1 con mi propio enfoque y desde cero.

**Página en CurseForge:** [Dynamic Ambience and Music](https://www.curseforge.com/minecraft/mc-mods/dynamic-ambience-and-music)

**GitHub:** [liray-dev/Dynamic-Ambience-And-Music](https://github.com/liray-dev/Dynamic-Ambience-And-Music/tree/master)



**Arisitva Studio** – Estudio de desarrollo de proyectos tipo servidor en Minecraft, Terraria y de diferentes tipos de juegos incluidos.
Gracias al equipo del desarrollo me motivaron a terminar este mod hasta el final, cualquier error o bugs me reportaron ellos, el cual perfecciono este mod casi al 100%. 

**Discord:** [Aristiva Studio](https://discord.gg/u9pq3raUXe)

**Twitter:** [Aristiva Studio](https://x.com/aristivastudio)

## LICENCIA
Todo esto esta reservado por la propiedad de Aristiva Studio y por Riburitu.

Cualquier copia, distribución, modificación o uso comercial de este mod sin autorización expresa del propietario o del estudio está prohibido. Cometer alguno de esto puede conllevar a consecuencias legales.


