# Privacy policy and legal notice (English and Spanish; the other languages link to the English pages).
# Plain HTML fragments. Keep them in sync with what the apps really do (see the README).

UPDATED = "2026-11-03"
EMAIL = "juan@gestal.es"

LIBS = [
    ("SmartTube, MediaServiceCore, SharedModules (yuliskov)", "MIT", "https://github.com/yuliskov/SmartTube"),
    ("ExoPlayer 2.10 (Amazon port)", "Apache 2.0", "https://github.com/amzn/exoplayer-amazon-port"),
    ("AndroidX, Leanback, Jetpack Compose, Material 3", "Apache 2.0", "https://developer.android.com/jetpack/androidx"),
    ("Kotlin, kotlinx.coroutines", "Apache 2.0", "https://github.com/JetBrains/kotlin"),
    ("OkHttp, Retrofit", "Apache 2.0", "https://square.github.io/okhttp/"),
    ("Glide", "BSD, MIT, Apache 2.0", "https://github.com/bumptech/glide"),
    ("RxJava, RxAndroid", "Apache 2.0", "https://github.com/ReactiveX/RxJava"),
    ("Gson, Guava", "Apache 2.0", "https://github.com/google/guava"),
    ("Cronet (Chromium)", "BSD-3-Clause", "https://chromium.googlesource.com/chromium/src/+/main/components/cronet/"),
    ("Conscrypt", "Apache 2.0", "https://github.com/google/conscrypt"),
    ("J2V8", "EPL 1.0", "https://github.com/eclipsesource/J2V8"),
    ("dnsjava", "BSD-2-Clause", "https://github.com/dnsjava/dnsjava"),
    ("NetCipher", "Apache 2.0", "https://github.com/guardianproject/NetCipher"),
    ("JsonPath, nanojson", "Apache 2.0", "https://github.com/json-path/JsonPath"),
    ("Java Regex (florianingerl)", "GPL 2.0", "https://github.com/florianingerl/com.florianingerl.util.regex"),
    ("ProcessPhoenix, DiskLruCache", "Apache 2.0", "https://github.com/JakeWharton/ProcessPhoenix"),
]

MIT = """MIT License

Copyright (c) 2020-present yuliskov (SmartTube)
Copyright (c) 2026-present Juan Gestal (SuperTube modifications)

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."""

GOOGLE_PRIVACY = "https://policies.google.com/privacy"
YT_TERMS = "https://www.youtube.com/t/terms"
GITHUB_PRIVACY = "https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement"

PRIVACY = {}
LEGAL = {}

PRIVACY["en"] = dict(title="Privacy policy", body=f"""
<p class="updated">Last updated: {UPDATED}</p>
<h2>In short</h2>
<p>The SuperTube apps (<code>es.gestal.supertube</code> for phones and tablets, <code>es.gestal.supertube.tv</code> for
Android TV) and this website <strong>don't collect any personal data for the developer</strong>. There are no SuperTube
accounts, no analytics, no advertising and no crash reports. SuperTube has no server of its own.</p>

<h2>Who is responsible</h2>
<p>Juan Gestal, developer of SuperTube. Contact: <a href="mailto:{EMAIL}">{EMAIL}</a>. If you write to us, we only use
your email to answer you and delete it when the conversation is over.</p>

<h2>Data that stays on your device</h2>
<p>To work, the app keeps on your device: your settings, favorites and folders, recent searches, playback positions,
downloaded files (in <code>Movies/SuperTube</code> and <code>Music/SuperTube</code>) and, only if you sign in, the session
of your Google account. None of this is sent to us. You can delete it by clearing the app data or uninstalling it.
Backups are files you export yourself or, if you choose it, copies saved in your own Google Drive.</p>

<h2>Services the app connects to</h2>
<p>To show videos, SuperTube connects <strong>directly</strong> from your device to these services. They receive your IP
address and the data needed for each request, and they process it under their own policies; the developer doesn't
receive any of it:</p>
<ul>
<li><strong>YouTube and Google</strong>: videos, thumbnails, search, comments, sign-in and the features of your account
(subscriptions, history, playlists), and the optional Google Drive backup. When you are signed in, Google may record your
activity in your account according to your settings there. <a href="{GOOGLE_PRIVACY}" rel="noopener">Google Privacy Policy</a>.</li>
<li><strong>SponsorBlock and DeArrow</strong> (<code>sponsor.ajay.app</code>): the video identifier, to get the sponsor
segments and the community titles and thumbnails, when these features are on.</li>
<li><strong>Return YouTube Dislike</strong> (<code>returnyoutubedislikeapi.com</code>): the video identifier, to show the
like and dislike counts in the player (it can be turned off in the player settings).</li>
<li><strong>GitHub</strong>: the update check and the download of new versions, and a configuration file used by the
video engine. <a href="{GITHUB_PRIVACY}" rel="noopener">GitHub Privacy Statement</a>.</li>
<li><strong>Images for some menus</strong>: on Android TV, links are shown as QR codes generated by
<code>api.qrserver.com</code> (it receives the link), and country flags may be loaded from <code>flagcdn.com</code>.</li>
</ul>

<h2>Permissions</h2>
<p>Internet access to play videos; notifications for media controls and downloads; access to storage only to save
downloads and backups where Android requires it; and the microphone only if you use voice search.</p>

<h2>This website</h2>
<p>These are static pages with no cookies, no analytics and no resources from other sites. The hosting server may keep
standard access logs (IP address, date and page requested) for security, for a limited time.</p>

<h2>Children</h2>
<p>SuperTube is not directed to children. The content comes from YouTube, which has its own age rules.</p>

<h2>Your rights</h2>
<p>As we don't keep your data, there is nothing we can show, correct or delete. For your data at YouTube, use the tools in
your Google account. You can also complain to your data protection authority (in Spain, the AEPD).</p>

<h2>Changes</h2>
<p>If this policy changes, the new version will be published on this page with its date.</p>
""")

PRIVACY["es"] = dict(title="Política de privacidad", body=f"""
<p class="updated">Última actualización: {UPDATED}</p>
<h2>En resumen</h2>
<p>Las apps SuperTube (<code>es.gestal.supertube</code> para móviles y tablets y <code>es.gestal.supertube.tv</code> para
Android TV) y esta web <strong>no recogen ningún dato personal para el desarrollador</strong>. No hay cuentas de SuperTube,
ni analíticas, ni publicidad, ni informes de errores. SuperTube no tiene servidor propio.</p>

<h2>Responsable</h2>
<p>Juan Gestal, desarrollador de SuperTube. Contacto: <a href="mailto:{EMAIL}">{EMAIL}</a>. Si nos escribes, solo usamos
tu correo para responderte y lo borramos al terminar la conversación.</p>

<h2>Datos que se quedan en tu dispositivo</h2>
<p>Para funcionar, la app guarda en tu dispositivo: tus ajustes, favoritos y carpetas, las búsquedas recientes, por dónde
ibas en cada vídeo, los archivos descargados (en <code>Movies/SuperTube</code> y <code>Music/SuperTube</code>) y, solo si
inicias sesión, la sesión de tu cuenta de Google. Nada de esto se nos envía. Puedes borrarlo limpiando los datos de la app
o desinstalándola. Las copias de seguridad son archivos que exportas tú o, si lo eliges, copias en tu propio Google Drive.</p>

<h2>Servicios a los que se conecta la app</h2>
<p>Para mostrar los vídeos, SuperTube se conecta <strong>directamente</strong> desde tu dispositivo a estos servicios.
Reciben tu dirección IP y los datos necesarios para cada petición, y los tratan según sus propias políticas; el
desarrollador no recibe nada:</p>
<ul>
<li><strong>YouTube y Google</strong>: vídeos, miniaturas, búsqueda, comentarios, inicio de sesión y las funciones de tu
cuenta (suscripciones, historial, listas), y la copia opcional en Google Drive. Con la sesión iniciada, Google puede
registrar tu actividad en tu cuenta según tu configuración allí. <a href="{GOOGLE_PRIVACY}" rel="noopener">Política de
privacidad de Google</a>.</li>
<li><strong>SponsorBlock y DeArrow</strong> (<code>sponsor.ajay.app</code>): el identificador del vídeo, para obtener los
segmentos patrocinados y los títulos y miniaturas de la comunidad, si estas funciones están activas.</li>
<li><strong>Return YouTube Dislike</strong> (<code>returnyoutubedislikeapi.com</code>): el identificador del vídeo, para
mostrar el recuento de «me gusta» y «no me gusta» en el reproductor (se puede desactivar en sus ajustes).</li>
<li><strong>GitHub</strong>: la comprobación y descarga de actualizaciones, y un archivo de configuración que usa el
motor de vídeo. <a href="{GITHUB_PRIVACY}" rel="noopener">Declaración de privacidad de GitHub</a>.</li>
<li><strong>Imágenes de algunos menús</strong>: en Android TV los enlaces se muestran como códigos QR generados por
<code>api.qrserver.com</code> (recibe el enlace), y las banderas de países pueden cargarse de <code>flagcdn.com</code>.</li>
</ul>

<h2>Permisos</h2>
<p>Acceso a internet para reproducir vídeos; notificaciones para los controles multimedia y las descargas; acceso al
almacenamiento solo para guardar descargas y copias donde Android lo exige; y el micrófono solo si usas la búsqueda por voz.</p>

<h2>Esta web</h2>
<p>Son páginas estáticas sin cookies, sin analíticas y sin recursos de otros sitios. El servidor de alojamiento puede
guardar registros de acceso habituales (dirección IP, fecha y página) por seguridad y durante un tiempo limitado.</p>

<h2>Menores</h2>
<p>SuperTube no está dirigida a menores. El contenido procede de YouTube, que tiene sus propias normas de edad.</p>

<h2>Tus derechos</h2>
<p>Como no guardamos tus datos, no hay nada que podamos mostrarte, corregir o borrar. Para tus datos en YouTube, usa las
herramientas de tu cuenta de Google. También puedes reclamar ante la Agencia Española de Protección de Datos (AEPD).</p>

<h2>Cambios</h2>
<p>Si esta política cambia, la nueva versión se publicará en esta página con su fecha.</p>
""")


def _libs(lang):
    rows = "".join(f'<li><a href="{u}" rel="noopener">{n}</a> · {l}</li>' for n, l, u in LIBS)
    return f"<ul class=\"libs\">{rows}</ul>"


LEGAL["en"] = dict(title="Legal notice and license", body=f"""
<p class="updated">Last updated: {UPDATED}</p>
<h2>About SuperTube</h2>
<p>SuperTube is a free, non-commercial, open-source project by Juan Gestal (<a href="mailto:{EMAIL}">{EMAIL}</a>),
published at <a href="https://github.com/jgestal/SuperTube" rel="noopener">github.com/jgestal/SuperTube</a>.</p>

<h2>No affiliation and trademarks</h2>
<p>SuperTube is an independent client. It is not affiliated with, endorsed or sponsored by Google LLC or YouTube, nor by
the SmartTube project. YouTube, Android, Google TV and Google Play are trademarks of Google LLC. The SuperTube name and
icon are not those of any of them. Other names are trademarks of their owners and are used only to describe compatibility.</p>

<h2>Content</h2>
<p>SuperTube doesn't host, upload or distribute any video. Everything you watch is served by YouTube and belongs to its
owners. Copyright complaints about a video must be sent to YouTube.</p>

<h2>Terms of use</h2>
<ul>
<li>The software is provided "as is", without warranty of any kind (see the license below).</li>
<li>You are responsible for how you use it. The <a href="{YT_TERMS}" rel="noopener">YouTube Terms of Service</a> don't
allow third-party clients, ad blocking or downloading videos without permission; using SuperTube may be against them,
and YouTube may restrict access.</li>
<li>Only download content you own, that is licensed for it (for example, Creative Commons) or that your local law lets you
copy for private use.</li>
</ul>

<h2>License</h2>
<p>SuperTube is released under the MIT license, the same as SmartTube, on which it is based:</p>
<pre class="license">{MIT}</pre>

<h2>Third-party software</h2>
<p>The apps include these components, each under its own license:</p>
{_libs("en")}
<p>Sponsor segments come from the <a href="https://sponsor.ajay.app" rel="noopener">SponsorBlock</a> community database
(CC BY-NC-SA 4.0).</p>

<h2>Images on this website</h2>
<p>The screenshots show open movies by <a href="https://studio.blender.org" rel="noopener">Blender Studio</a>
(Spring, Sprite Fright, Singularity and others), © Blender Foundation, licensed under CC BY 4.0. Blender is a trademark of
the Blender Foundation. The SuperTube icon and artwork are part of the project (MIT).</p>
""")

LEGAL["es"] = dict(title="Aviso legal y licencia", body=f"""
<p class="updated">Última actualización: {UPDATED}</p>
<h2>Sobre SuperTube</h2>
<p>SuperTube es un proyecto de código abierto, gratuito y sin ánimo de lucro de Juan Gestal
(<a href="mailto:{EMAIL}">{EMAIL}</a>), publicado en
<a href="https://github.com/jgestal/SuperTube" rel="noopener">github.com/jgestal/SuperTube</a>.</p>

<h2>Sin afiliación y marcas</h2>
<p>SuperTube es un cliente independiente. No está afiliado, respaldado ni patrocinado por Google LLC ni por YouTube, ni
por el proyecto SmartTube. YouTube, Android, Google TV y Google Play son marcas de Google LLC. El nombre y el icono de
SuperTube no son los de ninguno de ellos. Otros nombres son marcas de sus titulares y se usan solo para indicar
compatibilidad.</p>

<h2>Contenidos</h2>
<p>SuperTube no aloja, sube ni distribuye ningún vídeo. Todo lo que ves lo sirve YouTube y pertenece a sus titulares. Las
reclamaciones de derechos de autor sobre un vídeo deben dirigirse a YouTube.</p>

<h2>Condiciones de uso</h2>
<ul>
<li>El software se ofrece «tal cual», sin garantía de ningún tipo (consulta la licencia más abajo).</li>
<li>Eres responsable de cómo lo usas. Las <a href="{YT_TERMS}" rel="noopener">condiciones de servicio de YouTube</a> no
permiten clientes de terceros, bloquear anuncios ni descargar vídeos sin permiso; usar SuperTube puede ir en contra de
ellas y YouTube puede limitar el acceso.</li>
<li>Descarga solo contenido que sea tuyo, que tenga una licencia que lo permita (por ejemplo, Creative Commons) o que tu
legislación te permita copiar para uso privado.</li>
</ul>

<h2>Licencia</h2>
<p>SuperTube se publica con licencia MIT, la misma que SmartTube, en la que se basa. El texto oficial, en inglés:</p>
<pre class="license">{MIT}</pre>

<h2>Software de terceros</h2>
<p>Las apps incluyen estos componentes, cada uno con su propia licencia:</p>
{_libs("es")}
<p>Los segmentos patrocinados proceden de la base de datos comunitaria de
<a href="https://sponsor.ajay.app" rel="noopener">SponsorBlock</a> (CC BY-NC-SA 4.0).</p>

<h2>Imágenes de esta web</h2>
<p>Las capturas muestran películas abiertas de <a href="https://studio.blender.org" rel="noopener">Blender Studio</a>
(Spring, Sprite Fright, Singularity y otras), © Blender Foundation, con licencia CC BY 4.0. Blender es una marca de la
Blender Foundation. El icono y las ilustraciones de SuperTube forman parte del proyecto (MIT).</p>
""")
