"""SuperTube landing: static, self-contained (HTML + CSS + JS, no external requests), in 6 languages.

python3 tools/landing/gen_landing.py   -> landing/
Needs Pillow and rsvg-convert. Texts in strings.py, privacy/legal in legal.py, images from docs/screenshots.
"""
import html
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import promo  # noqa: E402
from legal import LEGAL, PRIVACY, UPDATED  # noqa: E402
from strings import S  # noqa: E402

BASE = "https://gestal.es/supertube/"
OUT = os.path.join(ROOT, "landing")
REPO = "https://github.com/jgestal/SuperTube"
DOWNLOAD = REPO + "/releases/latest"
TV_URL = BASE.split("://", 1)[1] + "tv"  # short address to type on a TV remote (tv/index.html redirects to the APK)
# Filled by find_downloads(): direct links to the newest release that carries each app
DIST = os.path.join(ROOT, "dist")  # signed APKs written by tools/release/release.sh (not in git)
# Filled by find_downloads(). "href" (site-relative) when the APK is hosted on this website (landing/apk/),
# else a GitHub release link. "src" is the local APK to copy.
PHONE = {"url": DOWNLOAD, "href": "", "name": "", "version": "", "src": "", "mb": 0}
# Release signing certificate, as in the README. Shown so visitors can check the APK is the original.
CERT_SHA256 = "f6:fb:28:69:7f:24:d1:bf:21:ff:12:b8:20:e2:64:ca:4a:94:2f:3e:e8:4c:75:f2:01:6d:eb:3c:e6:07:70:41"
TV = None  # None until there is a TV APK: then the "Coming soon" badges go away
LANGS = ["en", "es", "fr", "de", "pt", "it"]
HREFLANG = {"en": "en", "es": "es", "fr": "fr", "de": "de", "pt": "pt-BR", "it": "it"}
LEGAL_LANGS = ["en", "es"]  # the rest link to the English legal pages
PHONE_SHOTS = ["grid", "save_to", "favorites", "download_sheet", "search_mini", "layout_menu"]
TV_SHOTS = ["tv_backdrop", "tv_backdrop_spring", "tv_settings", "tv_lite_mode"]
# Hero simulator: thumbnails of Blender Studio open movies (CC BY 4.0) cropped from the TV app screenshots
CARDS = [("spring", "Spring", "7:45"), ("sprite_fright", "Sprite Fright", "10:30"), ("charge", "CHARGE", "4:23"),
         ("agent327", "Agent 327: Operation Barbershop", "3:52"), ("coffee_run", "Coffee Run", "3:05"),
         ("overgrown", "Overgrown", "1:10")]


def version():
    m = re.search(r'versionName "([^"]+)"', open(os.path.join(ROOT, "supertube", "build.gradle")).read())
    return m.group(1) if m else ""


def vkey(v):
    return [int(x) if x.isdigit() else 0 for x in re.split(r"[.-]", v)]


def local_apk(pattern):
    """Newest APK matching `pattern` in dist/release-<version>/: hosted on the website, in landing/apk/."""
    if not os.path.isdir(DIST):
        return None
    for d in sorted(os.listdir(DIST), key=lambda n: vkey(n.replace("release-", "")), reverse=True):
        folder = os.path.join(DIST, d)
        for name in sorted(os.listdir(folder)) if os.path.isdir(folder) else []:
            if re.match(pattern, name):
                path = os.path.join(folder, name)
                return {"url": BASE + "apk/" + name, "href": "apk/" + name, "name": name, "src": path,
                        "version": d.replace("release-", ""), "mb": round(os.path.getsize(path) / 1e6)}
    return None


def find_downloads():
    """APK links: the website's own copies (dist/ -> landing/apk/) or else the GitHub releases.

    Phone: SuperTube-<version>.apk. TV: the universal SuperTubeTV_stable_<version>_universal.apk (works on every TV)."""
    global TV
    PHONE["version"] = version()
    phone, tv = local_apk(r"SuperTube-[\d.]+\.apk$"), local_apk(r"SuperTubeTV_stable_.*_universal\.apk$")
    if phone:
        PHONE.update(phone)
    TV = tv
    if phone and tv:
        print("phone:", phone["name"], "| tv:", tv["name"], "(hosted in landing/apk/)")
        return
    try:
        req = urllib.request.Request("https://api.github.com/repos/jgestal/SuperTube/releases?per_page=20",
                                     headers={"Accept": "application/vnd.github+json", "User-Agent": "supertube-landing"})
        with urllib.request.urlopen(req, timeout=15) as r:
            releases = json.load(r)
    except Exception as ex:  # noqa: BLE001
        print("WARNING: GitHub releases not reachable, generic download links:", ex)
        return
    for rel_ in releases:  # newest first
        if rel_.get("draft") or rel_.get("prerelease"):
            continue
        ver = rel_["tag_name"].lstrip("v")
        for a in rel_["assets"]:
            name, url = a["name"], a["browser_download_url"]
            if not PHONE["name"] and name.endswith(".apk") and not name.startswith("SuperTubeTV"):
                PHONE.update(url=url, name=name, version=ver)
            if TV is None and re.match(r"SuperTubeTV_stable_.*_universal\.apk$", name):
                TV = {"url": url, "name": name, "version": ver}
    print("phone:", PHONE["href"] or PHONE["name"] or "-", "| tv:", (TV.get("href") or TV["name"]) if TV else "not released")


def publish_apks():
    """Copies the hosted APKs to landing/apk/ (the server must send them as application/vnd.android.package-archive)."""
    for app in (PHONE, TV):
        if app and app.get("src"):
            os.makedirs(os.path.join(OUT, "apk"), exist_ok=True)
            shutil.copy2(app["src"], os.path.join(OUT, app["href"]))
    # Apache: right type for the APKs (otherwise some browsers save them as .zip) and the 404 page
    root = "/" + BASE.split("://", 1)[1].split("/", 1)[1]
    write(".htaccess", "AddType application/vnd.android.package-archive .apk\n"
                       f"ErrorDocument 404 {root}404.html\n")


def link(app, here):
    return rel(here, app["href"]) if app.get("href") else app["url"]


def e(text):
    return html.escape(text, quote=True)


def path_of(lang, page=""):
    p = "" if lang == "en" else lang + "/"
    return p + (page + "/" if page else "")


def rel(here, target):
    """Relative link from page directory `here` to `target` (both relative to the site root)."""
    depth = here.count("/")
    return "../" * depth + target if target or depth else "./"


def write(rel_path, content):
    p = os.path.join(OUT, rel_path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        f.write(content)


# ---------- images ----------

def images():
    img = os.path.join(OUT, "assets", "img")
    for d in ["shots/phone", "shots/tv", "og", "cards"]:
        os.makedirs(os.path.join(img, d), exist_ok=True)
    shutil.copy(promo.ICON_SVG, os.path.join(img, "icon.svg"))
    for name, size in [("favicon.png", 64), ("apple-touch-icon.png", 180), ("icon-192.png", 192), ("icon-512.png", 512)]:
        promo.icon(size).save(os.path.join(img, name))
    Image.open(promo.AMBIENT).convert("RGB").save(os.path.join(img, "ambient.webp"), "WEBP", quality=80)
    for n in PHONE_SHOTS + ["player"]:  # player: the phone app card, not the gallery (cropped, short)
        src = Image.open(os.path.join(promo.SHOTS, n + ".webp")).convert("RGB")
        for w in (328, 656):
            src.resize((w, round(src.height * w / src.width)), Image.LANCZOS).save(
                os.path.join(img, "shots/phone", f"{n}-{w}.webp"), "WEBP", quality=82, method=6)
    for n in TV_SHOTS:
        src = promo.tv_shot(n + ".webp")
        for w in (800, 1600):
            src.resize((w, round(src.height * w / src.width)), Image.LANCZOS).save(
                os.path.join(img, "shots/tv", f"{n}-{w}.webp"), "WEBP", quality=82, method=6)
    for n, _, _ in CARDS:
        shutil.copy(os.path.join(HERE, "cards", n + ".webp"), os.path.join(img, "cards", n + ".webp"))
    for lang in LANGS:
        promo.og(lang).save(os.path.join(img, "og", f"og-{lang}.jpg"), "JPEG", quality=86, optimize=True, progressive=True)


def size_of(kind, name, w):
    p = os.path.join(OUT, "assets", "img", "shots", kind, f"{name}-{w}.webp")
    return Image.open(p).size


# ---------- shared parts ----------

ICONS = {
    "noads": '<path d="M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm-6.4 9a6.4 6.4 0 0 1 10.3-5L6.9 15.9A6.3 6.3 0 0 1 5.6 12Zm6.4 6.4c-1.4 0-2.8-.5-3.9-1.3l9-9A6.4 6.4 0 0 1 12 18.4Z"/>',
    "play": '<path d="M4 6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5v7a2.5 2.5 0 0 1-2.5 2.5H13v2h3v2H8v-2h3v-2H6.5A2.5 2.5 0 0 1 4 13.5v-7Zm6 1.3v4.4l4-2.2-4-2.2Z"/>',
    "download": '<path d="M11 3h2v9.2l3.3-3.3 1.4 1.4L12 16l-5.7-5.7 1.4-1.4 3.3 3.3V3ZM5 18h14v2H5v-2Z"/>',
    "folder": '<path d="M3 6.5A2.5 2.5 0 0 1 5.5 4H10l2 2h6.5A2.5 2.5 0 0 1 21 8.5v9a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 17.5v-11Zm9 3.2-1.1 2.3-2.5.3 1.9 1.7-.5 2.5 2.2-1.3 2.2 1.3-.5-2.5 1.9-1.7-2.5-.3L12 9.7Z"/>',
    "user": '<path d="M12 4a4 4 0 1 1 0 8 4 4 0 0 1 0-8Zm0 10c4.4 0 8 1.8 8 4v2H4v-2c0-2.2 3.6-4 8-4Z"/>',
    "feather": '<path d="M20 4c-6 0-11 3.5-12.6 9.4L4 20h2l2-3.4c4.8.2 9-2.7 10.4-7.6H14l5-2c.4-1 .7-2 1-3Z"/>',
    "shield": '<path d="M12 2 4 5v6c0 5 3.4 9.7 8 11 4.6-1.3 8-6 8-11V5l-8-3Zm-1.2 14.2-3.5-3.5 1.4-1.4 2.1 2.1 4.6-4.6 1.4 1.4-6 6Z"/>',
    "phone": '<path d="M8 2h8a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Zm0 3v13h8V5H8Zm4 14.2a.9.9 0 1 0 0 1.8.9.9 0 0 0 0-1.8Z"/>',
    "tv": '<path d="M4 5h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-6v2h3v1H7v-1h3v-2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Zm0 2v9h16V7H4Z"/>',
    "check": '<path d="m9.5 16.2-4.2-4.2-1.4 1.4 5.6 5.6L21.1 7.4 19.7 6 9.5 16.2Z"/>',
    "github": '<path d="M12 2a10 10 0 0 0-3.2 19.5c.5.1.7-.2.7-.5v-1.7c-2.8.6-3.4-1.3-3.4-1.3-.4-1.2-1.1-1.5-1.1-1.5-.9-.6.1-.6.1-.6 1 .1 1.5 1 1.5 1 .9 1.6 2.4 1.1 3 .9.1-.7.4-1.1.6-1.4-2.2-.2-4.6-1.1-4.6-5 0-1.1.4-2 1-2.7-.1-.3-.4-1.3.1-2.7 0 0 .8-.3 2.8 1a9.6 9.6 0 0 1 5 0c1.9-1.3 2.8-1 2.8-1 .5 1.4.2 2.4.1 2.7.6.7 1 1.6 1 2.7 0 3.9-2.4 4.8-4.6 5 .4.3.7.9.7 1.9V21c0 .3.2.6.7.5A10 10 0 0 0 12 2Z"/>',
    "globe": '<path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm6.9 6h-3a15.7 15.7 0 0 0-1.4-3.6A8 8 0 0 1 18.9 8ZM12 4a14 14 0 0 1 1.9 4h-3.8C10.6 6.6 11.2 5.2 12 4ZM4.3 14a8 8 0 0 1 0-4h3.4a16.5 16.5 0 0 0 0 4H4.3Zm.8 2h3a15.7 15.7 0 0 0 1.4 3.6A8 8 0 0 1 5.1 16Zm3-8h-3a8 8 0 0 1 4.4-3.6C8 5.5 7.6 6.7 7.9 8ZM12 20a14 14 0 0 1-1.9-4h3.8c-.5 1.4-1.1 2.8-1.9 4Zm2.3-6H9.7a14.7 14.7 0 0 1 0-4h4.6a14.7 14.7 0 0 1 0 4Zm.2 5.6c.6-1.1 1.1-2.3 1.4-3.6h3a8 8 0 0 1-4.4 3.6Zm1.8-5.6a16.5 16.5 0 0 0 0-4h3.4a8 8 0 0 1 0 4h-3.4Z"/>',
}
FEATURE_ICONS = ["noads", "play", "download", "folder", "user", "feather"]


def svg(name, cls="i"):
    return f'<svg class="{cls}" viewBox="0 0 24 24" aria-hidden="true" fill="currentColor">{ICONS[name]}</svg>'


def head(lang, here, title, desc, page, extra_ld=""):
    s = S[lang]
    alts = ""
    pages_langs = LEGAL_LANGS if page else LANGS
    for l in pages_langs:
        alts += f'<link rel="alternate" hreflang="{HREFLANG[l]}" href="{BASE}{path_of(l, page)}">\n'
    alts += f'<link rel="alternate" hreflang="x-default" href="{BASE}{path_of("en", page)}">\n'
    r = lambda t: rel(here, t)  # noqa: E731
    return f"""<!doctype html>
<html lang="{HREFLANG[lang]}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<title>{e(title)}</title>
<meta name="description" content="{e(desc)}">
<link rel="canonical" href="{BASE}{here}">
{alts}<meta name="theme-color" content="#120f18">
<meta name="color-scheme" content="dark">
<meta name="author" content="Juan Gestal">
<meta name="robots" content="index,follow">
<meta property="og:type" content="website">
<meta property="og:site_name" content="SuperTube">
<meta property="og:title" content="{e(title)}">
<meta property="og:description" content="{e(desc)}">
<meta property="og:url" content="{BASE}{here}">
<meta property="og:image" content="{BASE}assets/img/og/og-{lang}.jpg">
<meta property="og:image:width" content="1200"><meta property="og:image:height" content="630">
<meta property="og:image:alt" content="{e(s['h1a'] + ' ' + s['h1b'])}">
<meta property="og:locale" content="{HREFLANG[lang].replace('-', '_')}">
<meta name="twitter:card" content="summary_large_image">
<link rel="icon" type="image/svg+xml" href="{r('assets/img/icon.svg')}">
<link rel="icon" type="image/png" href="{r('assets/img/favicon.png')}">
<link rel="apple-touch-icon" href="{r('assets/img/apple-touch-icon.png')}">
<link rel="stylesheet" href="{r('assets/css/style.css')}">
<script src="{r('assets/js/main.js')}" defer></script>
{extra_ld}</head>
"""


def header(lang, here, page=""):
    s = S[lang]
    home = rel(here, path_of(lang))
    anchors = ["apps", "features", "screens", "faq"]
    nav = "".join(f'<a href="{home}#{a}">{e(t)}</a>' for a, t in zip(anchors, s["nav"]))
    langs_for = LEGAL_LANGS if page else LANGS
    items = "".join(
        f'<li><a href="{rel(here, path_of(l, page))}" hreflang="{HREFLANG[l]}" lang="{HREFLANG[l]}"'
        f'{" aria-current=\"page\"" if l == lang else ""}>{e(S[l]["lang_name"])}</a></li>' for l in langs_for)
    return f"""<body>
<a class="skip" href="#main">{e(s['skip'])}</a>
<div class="aurora" aria-hidden="true"><i></i><i></i><i></i></div>
<header class="top"><div class="wrap">
<a class="brand" href="{home}" aria-label="SuperTube"><img src="{rel(here, 'assets/img/icon.svg')}" alt="" width="34" height="34"><b>Super<span>Tube</span></b></a>
<nav class="nav" aria-label="SuperTube">{nav}</nav>
<details class="lang"><summary aria-label="{e(s['menu_lang'])}">{svg('globe')}<span>{lang.upper()}</span></summary><ul>{items}</ul></details>
<a class="btn small primary hide-sm" href="{home}#install">{svg('download')}{e(s['dl'])}</a>
</div></header>
"""


def footer(lang, here):
    s = S[lang]
    ll = lang if lang in LEGAL_LANGS else "en"
    return f"""<footer class="foot"><div class="wrap">
<div class="foot-top">
<a class="brand" href="{rel(here, path_of(lang))}"><img src="{rel(here, 'assets/img/icon.svg')}" alt="" width="28" height="28"><b>Super<span>Tube</span></b></a>
<nav aria-label="{e(s['legal'])}"><a href="{rel(here, path_of(ll, 'privacy'))}">{e(s['privacy'])}</a><a href="{rel(here, path_of(ll, 'legal'))}">{e(s['legal'])}</a><a href="{REPO}" rel="noopener">{svg('github')}{e(s['source'])}</a></nav>
</div>
<p>© 2026 Juan Gestal · {e(s['foot_lic'])} {e(s['foot_based'])}</p>
<p class="fine">{e(s['disclaimer'])}</p>
</div></footer>
</body>
</html>
"""


def dl_button(lang, here, kind, size="big"):
    """Download button of one app: phone and TV are different APKs, each button says which one it is."""
    s = S[lang]
    if kind == "phone":
        app, label = PHONE, s["dl_phone"]
    elif TV:
        app, label = TV, s["dl_tv"]
    else:
        return (f'<span class="btn {size} ghost soon-btn" aria-disabled="true">{svg("tv")}'
                f'<span><b>{e(s["dl_tv"])}</b><small>{e(s["soon"])}</small></span></span>')
    ver = app["version"]
    sub = f'APK{" · v" + ver if ver else ""} · ' + (f'{app["mb"]} MB' if app.get("href") else "GitHub")
    return (f'<a class="btn primary {size}" href="{link(app, here)}"{" download" if app.get("href") else ""} rel="noopener">{svg(kind)}'
            f'<span><b>{e(label)}</b><small>{e(sub)}</small></span></a>')


def cta(lang, here):
    return f'<div class="dl-pair">{dl_button(lang, here, "phone")}{dl_button(lang, here, "tv")}</div>'


def file_line(lang, kind):
    app = PHONE if kind == "phone" else TV
    if not app or not app["name"]:
        return ""
    return f'<p class="file">{svg("download")}{e(S[lang]["dl_file"])}: <code>{e(app["name"])}</code></p>'


def phone_img(here, name, cls="", lazy=True, w=656):
    W, H = size_of("phone", name, w)
    return (f'<img class="{cls}" src="{rel(here, f"assets/img/shots/phone/{name}-{w}.webp")}" '
            f'srcset="{rel(here, f"assets/img/shots/phone/{name}-328.webp")} 328w, {rel(here, f"assets/img/shots/phone/{name}-656.webp")} 656w" '
            f'sizes="(max-width: 700px) 60vw, 300px" width="{W}" height="{H}" alt=""{" loading=\"lazy\"" if lazy else ""} decoding="async">')


def tv_img(here, name, cls="", lazy=True, alt=""):
    W, H = size_of("tv", name, 1600)
    return (f'<img class="{cls}" src="{rel(here, f"assets/img/shots/tv/{name}-1600.webp")}" '
            f'srcset="{rel(here, f"assets/img/shots/tv/{name}-800.webp")} 800w, {rel(here, f"assets/img/shots/tv/{name}-1600.webp")} 1600w" '
            f'sizes="(max-width: 900px) 92vw, 720px" width="{W}" height="{H}" alt="{e(alt)}"{" loading=\"lazy\"" if lazy else ""} decoding="async">')


def simulator(lang, here):
    """Playable mock of the TV app: rows of cards, the focused video as backdrop, a player that skips the sponsor."""
    s = S[lang]
    cards = "".join(
        f'<button type="button" class="sim-card" data-img="{rel(here, f"assets/img/cards/{n}.webp")}" data-title="{e(t)}">'
        f'<span class="th"><img src="{rel(here, f"assets/img/cards/{n}.webp")}" alt="" width="448" height="252" decoding="async"><b>{d}</b></span>'
        f'<span class="tt">{e(t)}</span><span class="mt">Blender Studio</span></button>' for n, t, d in CARDS)
    return f"""<div class="sim" id="sim" tabindex="0" role="region" aria-roledescription="demo" aria-label="{e(s['sim_label'])}">
  <div class="sim-bg" aria-hidden="true"><img alt=""><img alt=""></div>
  <div class="sim-top" aria-hidden="true"><i class="orb red"></i><i class="orb"></i><span class="sim-clock">20:30</span><img src="{rel(here, 'assets/img/icon.svg')}" alt=""></div>
  <p class="sim-row-title">{e(s['sim_row'])}</p>
  <div class="sim-row"><div class="sim-track">{cards}</div></div>
  <div class="sim-player" hidden>
    <img alt="">
    <span class="sim-badge">{svg('noads')}{e(s['sim_noads'])}</span>
    <span class="sim-toast" role="status">{svg('check')}{e(s['sim_skip'])}</span>
    <div class="sim-info"><b></b><small>Blender Studio</small>
      <div class="sim-progress"><i class="seg"></i><i class="done"></i><i class="head"></i></div></div>
    <button type="button" class="sim-back" aria-label="{e(s['sim_back'])}">‹</button>
  </div>
</div>"""


# ---------- home ----------

def home(lang):
    s = S[lang]
    here = path_of(lang)
    soon = "" if TV else f'<span class="badge">{e(s["soon"])}</span>'
    ld_app = {
        "@context": "https://schema.org", "@type": "SoftwareApplication", "name": "SuperTube",
        "operatingSystem": "Android 8.0+", "applicationCategory": "MultimediaApplication",
        "description": s["desc"], "url": BASE + here, "downloadUrl": PHONE["url"], "softwareVersion": PHONE["version"],
        "license": "https://opensource.org/licenses/MIT", "isAccessibleForFree": True,
        "inLanguage": [HREFLANG[l] for l in LANGS], "image": BASE + "assets/img/icon-512.png",
        "screenshot": [BASE + f"assets/img/shots/tv/{n}-1600.webp" for n in TV_SHOTS[:2]] +
                      [BASE + f"assets/img/shots/phone/{n}-656.webp" for n in PHONE_SHOTS[:3]],
        "offers": {"@type": "Offer", "price": "0", "priceCurrency": "EUR"},
        "author": {"@type": "Person", "name": "Juan Gestal", "url": "https://gestal.es"},
        "codeRepository": REPO,
    }
    ld_faq = {"@context": "https://schema.org", "@type": "FAQPage", "mainEntity": [
        {"@type": "Question", "name": q, "acceptedAnswer": {"@type": "Answer", "text": a}} for q, a in s["faqs"]]}
    ld = (f'<script type="application/ld+json">{json.dumps(ld_app, ensure_ascii=False)}</script>\n'
          f'<script type="application/ld+json">{json.dumps(ld_faq, ensure_ascii=False)}</script>\n')
    out = head(lang, here, s["title"], s["desc"], "", ld) + header(lang, here)

    chips = "".join(f"<li>{svg('check')}{e(c)}</li>" for c in s["chips"])
    feats = "".join(
        f'<article class="card feat reveal"><span class="ic">{svg(FEATURE_ICONS[i])}</span><h3>{e(t)}</h3><p>{e(p)}</p></article>'
        for i, (t, p) in enumerate(s["feats"]))
    phone_pts = "".join(f"<li>{svg('check')}{e(p)}</li>" for p in s["phone_points"])
    tv_pts = "".join(f"<li>{svg('check')}{e(p)}</li>" for p in s["tv_points"])

    def figures(kind, names):
        items = ""
        for i, n in enumerate(names):
            img = phone_img(here, n) if kind == "phone" else tv_img(here, n)
            full = rel(here, f"assets/img/shots/{kind}/{n}-{656 if kind == 'phone' else 1600}.webp")
            items += (f'<figure class="shot {kind}"><button type="button" data-full="{full}" data-cap="{e(s["caps"][n])}" '
                      f'aria-label="{e(s["caps"][n])}">{img}</button><figcaption>{e(s["caps"][n])}</figcaption></figure>')
        return items

    steps = "".join(f'<li class="reveal"><h3>{e(h)}</h3><p>{e(p)}</p></li>' for h, p in s["steps"])
    tv_steps = "".join(f'<li class="reveal"><h3>{e(h)}</h3><p>{e(p).replace(TV_URL, f"<b class=url>{TV_URL}</b>")}</p></li>'
                       for h, p in s["tv_steps"])
    warn_items = "".join(
        f'<li><span class="w-src">{e(src)}</span><p class="w-msg">{e(msg)}</p>'
        f'<p>{re.sub(r"[*][*](.+?)[*][*]", lambda m: f"<b>{m[1]}</b>", e(fix))}</p></li>' for src, msg, fix in s["warn_items"])
    faqs = "".join(f"<details class=\"reveal\"><summary>{e(q)}</summary><p>{e(a)}</p></details>" for q, a in s["faqs"])
    priv_pts = "".join(f"<li>{svg('check')}{e(p)}</li>" for p in s["priv_points"])
    ll = lang if lang in LEGAL_LANGS else "en"
    suggest = json.dumps({l: {"name": S[l]["lang_name"], "href": rel(here, path_of(l))} for l in LANGS}, ensure_ascii=False)

    out += f"""<main id="main" data-lang="{lang}" data-langs='{e(suggest)}'>
<section class="hero"><div class="wrap">
  <div class="copy">
    <p class="kicker"><i></i>{e(s['kicker'])}</p>
    <h1><span>{e(s['h1a'])}</span> <span class="grad">{e(s['h1b'])}</span></h1>
    <p class="lead">{e(s['lead'])}</p>
    <div class="ctas">{cta(lang, here)}</div>
    <ul class="chips">{chips}</ul>
  </div>
  <div class="stage">
    <div class="dev-tv tilt">{simulator(lang, here)}<i class="stand" aria-hidden="true"></i></div>
    <div class="dev-phone tilt deep" aria-hidden="true">{phone_img(here, 'grid', '', False)}</div>
    <div class="sim-bar"><span class="sim-hint">{e(s['sim_hint'])}</span>
      <label class="switch"><input type="checkbox" id="sim-lite" role="switch"><i aria-hidden="true"></i>{e(s['sim_lite'])}</label></div>
  </div>
</div></section>

<section id="apps"><div class="wrap">
  <div class="head reveal"><h2>{e(s['apps_h'])}</h2><p>{e(s['apps_p'])}</p></div>
  <div class="apps">
    <article class="card app reveal"><div class="app-media phone-media">{phone_img(here, 'player')}</div>
      <div class="app-copy"><h3>{svg('phone')}{e(s['phone_h'])}</h3><p>{e(s['phone_p'])}</p><ul class="ticks">{phone_pts}</ul>
      <div class="app-dl">{dl_button(lang, here, 'phone', 'small')}{file_line(lang, 'phone')}</div></div></article>
    <article class="card app reveal"><div class="app-media tv-media">{tv_img(here, 'tv_lite_mode')}</div>
      <div class="app-copy"><h3>{svg('tv')}{e(s['tv_h'])}{soon}</h3><p>{e(s['tv_p'])}</p><ul class="ticks">{tv_pts}</ul>
      <div class="app-dl">{dl_button(lang, here, 'tv', 'small')}{file_line(lang, 'tv')}</div></div></article>
  </div>
</div></section>

<section id="features"><div class="wrap">
  <div class="head reveal"><h2>{e(s['feat_h'])}</h2><p>{e(s['feat_p'])}</p></div>
  <div class="grid">{feats}</div>
</div></section>

<section id="screens"><div class="wrap">
  <div class="head reveal"><h2>{e(s['screens_h'])}</h2><p>{e(s['screens_p'])}</p></div>
  <div class="tabs" role="tablist">
    <button type="button" role="tab" id="t-phone" aria-controls="p-phone" aria-selected="true">{svg('phone')}{e(s['tab_phone'])}</button>
    <button type="button" role="tab" id="t-tv" aria-controls="p-tv" aria-selected="false" tabindex="-1">{svg('tv')}{e(s['tab_tv'])}</button>
  </div>
  <div class="rail" id="p-phone" role="tabpanel" aria-labelledby="t-phone">{figures('phone', PHONE_SHOTS)}</div>
  <div class="rail tv-rail" id="p-tv" role="tabpanel" aria-labelledby="t-tv" hidden>{figures('tv', TV_SHOTS)}</div>
</div></section>

<section id="privacy"><div class="wrap">
  <div class="privacy card reveal">
    <span class="ic big">{svg('shield')}</span>
    <div><h2>{e(s['priv_h'])}</h2><p>{e(s['priv_p'])}</p><ul class="ticks inline">{priv_pts}</ul>
    <a class="more" href="{rel(here, path_of(ll, 'privacy'))}">{e(s['priv_link'])} →</a></div>
  </div>
</div></section>

<section id="install"><div class="wrap">
  <div class="head reveal"><h2>{e(s['inst_h'])}</h2><p>{e(s['inst_p'])}</p></div>
  <h3 class="steps-h reveal">{svg('phone')}{e(s['inst_phone_h'])}</h3>
  <ol class="steps">{steps}</ol>
  <h3 class="steps-h reveal">{svg('tv')}{e(s['inst_tv_h'])}{soon}</h3>
  <ol class="steps">{tv_steps}</ol>
  <div class="warn card reveal" id="warning">
    <h3>{svg('shield')}{e(s['warn_h'])}</h3><p class="w-why">{e(s['warn_p'])}</p>
    <ul class="w-list">{warn_items}</ul>
    <p class="w-fp">{e(s['warn_fp'])} <code>{CERT_SHA256}</code></p>
  </div>
</div></section>

<section id="faq"><div class="wrap narrow">
  <div class="head reveal"><h2>{e(s['faq_h'])}</h2></div>
  <div class="faqs">{faqs}</div>
</div></section>

<section class="final"><div class="wrap">
  <div class="final-card reveal"><img src="{rel(here, 'assets/img/icon.svg')}" alt="" width="88" height="88" loading="lazy">
  <h2>{e(s['final_h'])}</h2><p>{e(s['final_p'])}</p>{cta(lang, here)}</div>
</div></section>
</main>

<dialog class="lightbox" aria-label="{e(s['screens_h'])}">
  <button type="button" class="lb-close" aria-label="{e(s['close'])}">×</button>
  <button type="button" class="lb-prev" aria-label="{e(s['prev'])}">‹</button>
  <figure><img alt=""><figcaption></figcaption></figure>
  <button type="button" class="lb-next" aria-label="{e(s['next'])}">›</button>
</dialog>
"""
    out += footer(lang, here)
    write(os.path.join(here, "index.html"), out)


def doc_page(lang, page, doc):
    s = S[lang]
    here = path_of(lang, page)
    title = f"{doc['title']} · SuperTube"
    out = head(lang, here, title, s["desc"], page) + header(lang, here, page)
    out += f'<main id="main" class="doc"><div class="wrap narrow"><h1>{e(doc["title"])}</h1>{doc["body"]}</div></main>\n'
    out += footer(lang, here)
    write(os.path.join(here, "index.html"), out)


def sitemap():
    urls = [path_of(l) for l in LANGS] + [path_of(l, p) for l in LEGAL_LANGS for p in ("privacy", "legal")]
    body = "".join(f"<url><loc>{BASE}{u}</loc><lastmod>{UPDATED}</lastmod></url>\n" for u in urls)
    write("sitemap.xml", f'<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n{body}</urlset>\n')
    write("robots.txt", f"User-agent: *\nAllow: /\nSitemap: {BASE}sitemap.xml\n")


def llms_txt():
    """llms.txt (llmstxt.org): a plain summary so AI assistants describe and cite the app correctly."""
    s = S["en"]
    tv = "" if TV else f" ({s['soon'].lower()})"
    lines = [
        "# SuperTube", "",
        f"> {s['desc']}", "",
        f"{s['lead']} It is an independent fork of SmartTube by yuliskov (MIT license), not affiliated with Google or "
        "YouTube.", "",
        "## Apps", "",
        f"- {s['phone_h']}: {s['phone_p']} " + ". ".join(s["phone_points"]) + ".",
        f"- {s['tv_h']}{tv}: {s['tv_p']} " + ". ".join(s["tv_points"]) + ".", "",
        "## Features", "",
        *[f"- {t}: {d}" for t, d in s["feats"]], "",
        "## Privacy", "",
        s["priv_p"], "",
        f"## Install on a phone or tablet", "",
        *[f"{i}. {t}: {d}" for i, (t, d) in enumerate(s["steps"], 1)], "",
        f"## Install on a TV", "",
        *[f"{i}. {t}: {d}" for i, (t, d) in enumerate(s["tv_steps"], 1)], "",
        "## FAQ", "",
        *[f"- {q} {a}" for q, a in s["faqs"]], "",
        "## Links", "",
        f"- [Website]({BASE}): " + ", ".join(f"[{S[l]['lang_name']}]({BASE}{path_of(l)})" for l in LANGS),
        f"- [Download for phones and tablets]({PHONE['url']})" + (f" ({PHONE['name']})" if PHONE["name"] else ""),
        (f"- [Download for Android TV]({TV['url']}) ({TV['name']}), or type {TV_URL} in the Downloader app on the TV"
         if TV else "- Android TV app: coming soon"),
        f"- [All releases]({DOWNLOAD})",
        f"- [Source code and changelog]({REPO})",
        f"- [Privacy policy]({BASE}{path_of('en', 'privacy')})",
        f"- [Legal notice and licenses]({BASE}{path_of('en', 'legal')})", "",
    ]
    write("llms.txt", "\n".join(lines))


def tv_page():
    """{TV_URL}: short address for the TV remote. Redirects to the universal TV APK (or to the install steps)."""
    target = link(TV, "tv/") if TV else "../#install"
    texts = {l: [S[l]["tv_page_h"], S[l]["tv_page_p"], S[l]["dl_tv"]] for l in LANGS}
    write("tv/index.html", f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex"><meta http-equiv="refresh" content="1;url={e(target)}">
<title>SuperTube · Android TV</title><link rel="stylesheet" href="../assets/css/style.css"></head>
<body class="solo"><main class="wrap narrow"><img src="../assets/img/icon.svg" alt="" width="96" height="96">
<h1 id="h">{e(texts["en"][0])}</h1><p id="p">{e(texts["en"][1])}</p>
<a class="btn primary big" id="a" href="{e(target)}">{svg("tv")}<span><b id="b">{e(texts["en"][2])}</b><small>{e(TV["name"] if TV else S["en"]["soon"])}</small></span></a></main>
<script>(function(){{var t={json.dumps(texts, ensure_ascii=False)},l=(navigator.language||"en").slice(0,2);
if(t[l]&&l!=="en"){{document.documentElement.lang=l;["h","p","b"].forEach(function(id,i){{document.getElementById(id).textContent=t[l][i];}});}}}})();</script>
</body></html>
""")


def not_found():
    """404.html: served by the web server for any missing path, so its links are absolute (from BASE)."""
    root = "/" + BASE.split("://", 1)[1].split("/", 1)[1]  # "/supertube/"
    texts = {l: [S[l]["nf_h"], S[l]["nf_p"], S[l]["nf_home"], root + path_of(l)] for l in LANGS}
    en = texts["en"]
    write("404.html", f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex"><title>{e(en[0])} · SuperTube</title>
<link rel="icon" href="{root}assets/img/icon.svg" type="image/svg+xml"><link rel="stylesheet" href="{root}assets/css/style.css"></head>
<body class="solo"><main class="wrap narrow"><img src="{root}assets/img/icon.svg" alt="" width="96" height="96">
<p class="kicker"><i></i>404</p><h1 id="h">{e(en[0])}</h1><p id="p">{e(en[1])}</p>
<a class="btn primary" id="a" href="{en[3]}">{e(en[2])}</a></main>
<script>(function(){{var t={json.dumps(texts, ensure_ascii=False)},l=(navigator.language||"en").slice(0,2);
if(t[l]&&l!=="en"){{document.documentElement.lang=l;document.title=t[l][0]+" · SuperTube";
document.getElementById("h").textContent=t[l][0];document.getElementById("p").textContent=t[l][1];
var a=document.getElementById("a");a.textContent=t[l][2];a.href=t[l][3];}}}})();</script>
</body></html>
""")


def main():
    find_downloads()
    for s in S.values():  # the TV address goes in the TV steps and the FAQ
        s["tv_steps"] = [(h, p.replace("{tv_url}", TV_URL)) for h, p in s["tv_steps"]]
        s["faqs"] = [(q, a.replace("{tv_url}", TV_URL)) for q, a in s["faqs"]]
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)
    shutil.copytree(os.path.join(HERE, "static"), os.path.join(OUT, "assets"), dirs_exist_ok=True)
    publish_apks()
    images()
    for lang in LANGS:
        home(lang)
    for lang in LEGAL_LANGS:
        doc_page(lang, "privacy", PRIVACY[lang])
        doc_page(lang, "legal", LEGAL[lang])
    sitemap()
    llms_txt()
    tv_page()
    not_found()
    subprocess.run([sys.executable, os.path.join(ROOT, "tools", "strip_metadata.py"), str(OUT)], check=True)
    print("landing ->", OUT)


if __name__ == "__main__":
    main()
