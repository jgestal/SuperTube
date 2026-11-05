# Servidor web de SuperTube

Configuración para servir la web en `https://gestal.es/supertube/`. Hace tres cosas:

- Los `.apk` salen como `application/vnd.android.package-archive`, para que el móvil los reconozca como apps.
- Las direcciones que no existen muestran `/supertube/404.html`, la página 404 de SuperTube.
- Esta carpeta `server/` no se puede abrir desde la web (da 404).

No hace falta subir `server/` al servidor. Si se sube, queda oculta igualmente.

## nginx (HestiaCP)

Este es el que usa gestal.es. nginx no lee `.htaccess`.

Como root, en el servidor (escribe el contenido de `nginx/nginx.ssl.conf_supertube` y recarga nginx):

```bash
cat > /home/gestal/conf/web/gestal.es/nginx.ssl.conf_supertube <<'EOF'
# SuperTube website (/supertube/): APK type, 404 page, and server/ not public
location ^~ /supertube/ {
    index index.html;
    error_page 404 /supertube/404.html;

    location ~* \.apk$ {
        types { }
        default_type application/vnd.android.package-archive;
    }
    location ^~ /supertube/server/ {
        return 404;
    }
}
EOF
nginx -t && systemctl reload nginx
```

- Hestia incluye automáticamente cualquier archivo `nginx.ssl.conf_*` de la carpeta del dominio y lo conserva al regenerar las
  plantillas. Se comprueba con `grep -n "nginx.ssl.conf_" /home/gestal/conf/web/gestal.es/nginx.ssl.conf`.
- Si `nginx -t` da error, no se recarga y la web sigue como estaba.
- Si la caché de nginx del dominio está activada en Hestia, vacíala desde el panel.
- Para quitarlo: `rm /home/gestal/conf/web/gestal.es/nginx.ssl.conf_supertube && nginx -t && systemctl reload nginx`.

Sin Hestia, el bloque `location` de `nginx/nginx.ssl.conf_supertube` va dentro del bloque `server` del dominio
(`/etc/nginx/sites-available/...`), y luego `nginx -t && systemctl reload nginx`.

## Apache

Sube `apache/_htaccess` (es el mismo archivo que `landing/_htaccess`) a la carpeta de la web, `/supertube/`, y en el
servidor renómbralo a `.htaccess`.

- Apache tiene que permitir `.htaccess` en esa carpeta: `AllowOverride FileInfo` (o `All`) en su `<Directory>`.
- Si no se permite, pon las mismas líneas dentro de `<Directory /ruta/a/supertube/>` en la configuración del sitio y
  recarga con `apachectl configtest && systemctl reload apache2`.

## Comprobar

```bash
curl -sI https://gestal.es/supertube/apk/SuperTube-<versión>.apk | grep -i content-type   # application/vnd.android.package-archive
curl -s -o /dev/null -w '%{http_code}\n' https://gestal.es/supertube/no-existe            # 404 (página de SuperTube)
curl -s -o /dev/null -w '%{http_code}\n' https://gestal.es/supertube/server/README.md     # 404
```
