"""Promotional images for the web and social networks, drawn with Pillow from the real screenshots.

python3 tools/landing/promo.py            -> promo/<lang>/*.jpg|png for every language
Also imported by gen_landing.py for the Open Graph images.
"""
import io
import os
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
from strings import S  # noqa: E402

SHOTS = os.path.join(ROOT, "docs", "screenshots")
AMBIENT = os.path.join(ROOT, "smarttubetv", "src", "main", "res", "drawable-nodpi", "supertube_ambient.jpg")
ICON_SVG = os.path.join(ROOT, "supertube", "icon", "icon_square.svg")
AVENIR = "/System/Library/Fonts/Avenir Next.ttc"
RED, MAGENTA, WHITE = (255, 61, 46), (194, 24, 91), (255, 255, 255)
FACES = {"bold": 0, "demi": 2, "medium": 5, "regular": 7, "heavy": 8}


def font(face, size):
    return ImageFont.truetype(AVENIR, size, index=FACES[face])


def icon(size):
    png = subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), ICON_SVG], capture_output=True, check=True).stdout
    return Image.open(io.BytesIO(png)).convert("RGBA")


def cover(img, w, h):
    s = max(w / img.width, h / img.height)
    img = img.resize((round(img.width * s), round(img.height * s)), Image.LANCZOS)
    x, y = (img.width - w) // 2, (img.height - h) // 2
    return img.crop((x, y, x + w, y + h))


def background(w, h, darken=0.0):
    bg = cover(Image.open(AMBIENT).convert("RGB"), w, h)
    if darken:
        bg = Image.blend(bg, Image.new("RGB", (w, h), (12, 10, 16)), darken)
    return bg.convert("RGBA")


def rounded(img, r):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, img.width - 1, img.height - 1), r, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def shadow(size, box, radius, blur, alpha=150):
    sh = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle(box, radius, fill=(0, 0, 0, alpha))
    return sh.filter(ImageFilter.GaussianBlur(blur))


def phone(shot, height):
    """Phone with a thin black frame around a tall screenshot."""
    img = Image.open(os.path.join(SHOTS, shot)).convert("RGB")
    sw = round(height * 0.92 * img.width / img.height)
    sh = round(height * 0.92)
    screen = rounded(img.resize((sw, sh), Image.LANCZOS), round(sw * 0.07))
    pad = round(height * 0.02)
    body = Image.new("RGBA", (sw + 2 * pad, sh + 2 * pad), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle((0, 0, body.width - 1, body.height - 1), round(sw * 0.11), fill=(16, 15, 20), outline=(70, 66, 78), width=max(2, pad // 6))
    body.alpha_composite(screen, (pad, pad))
    return body


def fade_bottom(img, start=0.72):
    """Fades the last row of the TV screenshots to black (it shows suggestions that aren't Blender open movies)."""
    img = img.convert("RGB")
    grad = Image.new("L", (1, img.height), 0)
    for y in range(img.height):
        k = (y / img.height - start) / (1 - start)
        grad.putpixel((0, y), max(0, min(255, round(255 * k * 1.7))))
    return Image.composite(Image.new("RGB", img.size, (8, 8, 10)), img, grad.resize(img.size))


def tv_shot(shot):
    img = Image.open(os.path.join(SHOTS, shot)).convert("RGB")
    return fade_bottom(img) if "backdrop" in shot else img


def tv(shot, width):
    """Flat TV: thin bezel and a small stand."""
    img = tv_shot(shot)
    sw, sh = width, round(width * img.height / img.width)
    bez = max(6, width // 90)
    stand_h = round(width * 0.07)
    out = Image.new("RGBA", (sw + 2 * bez, sh + 2 * bez + stand_h), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    d.rounded_rectangle((0, 0, sw + 2 * bez - 1, sh + 2 * bez - 1), bez * 2, fill=(14, 13, 18), outline=(60, 56, 66), width=2)
    out.alpha_composite(rounded(img.resize((sw, sh), Image.LANCZOS), bez), (bez, bez))
    cx = out.width // 2
    d.polygon([(cx - width * 0.06, sh + 2 * bez), (cx + width * 0.06, sh + 2 * bez), (cx + width * 0.03, sh + 2 * bez + stand_h * 0.75), (cx - width * 0.03, sh + 2 * bez + stand_h * 0.75)], fill=(30, 28, 36))
    d.rounded_rectangle((cx - width * 0.16, sh + 2 * bez + stand_h * 0.72, cx + width * 0.16, out.height - 1), stand_h // 5, fill=(38, 35, 44))
    return out


def paste_shadowed(canvas, img, xy, blur=28, alpha=160):
    x, y = xy
    sh = shadow(canvas.size, (x + 10, y + 24, x + img.width - 10, y + img.height + 10), 40, blur, alpha)
    canvas.alpha_composite(sh)
    canvas.alpha_composite(img, (round(x), round(y)))


def wrap(draw, text, fnt, max_w):
    words, lines, line = text.split(), [], ""
    for w in words:
        test = (line + " " + w).strip()
        if draw.textlength(test, font=fnt) <= max_w or not line:
            line = test
        else:
            lines.append(line)
            line = w
    lines.append(line)
    return lines


def text_block(draw, xy, text, fnt, max_w, fill=WHITE, spacing=1.12):
    x, y = xy
    for ln in wrap(draw, text, fnt, max_w):
        draw.text((x, y), ln, font=fnt, fill=fill)
        y += round(fnt.size * spacing)
    return y


def gradient_text(canvas, xy, text, fnt):
    """Text filled with the brand gradient (red to magenta)."""
    x, y = xy
    d = ImageDraw.Draw(canvas)
    l, t, r, b = d.textbbox((x, y), text, font=fnt)
    mask = Image.new("L", canvas.size, 0)
    ImageDraw.Draw(mask).text((x, y), text, font=fnt, fill=255)
    grad = Image.new("RGBA", canvas.size)
    gd = ImageDraw.Draw(grad)
    for i in range(int(l), int(r) + 1):
        k = (i - l) / max(1, r - l)
        c = tuple(round(RED[j] + (MAGENTA[j] + 40 - RED[j]) * k) for j in range(3))
        gd.line([(i, t - 4), (i, b + 4)], fill=c + (255,))
    canvas.paste(grad, (0, 0), mask)
    return b


def brand(canvas, xy, size, label=True):
    x, y = xy
    canvas.alpha_composite(icon(size), (round(x), round(y)))
    if label:
        d = ImageDraw.Draw(canvas)
        f = font("bold", round(size * 0.52))
        d.text((x + size * 1.22, y + size * 0.2), "Super", font=f, fill=WHITE)
        w = d.textlength("Super", font=f)
        d.text((x + size * 1.22 + w, y + size * 0.2), "Tube", font=f, fill=RED)


def pill(canvas, xy, text, fnt, pad=(22, 10)):
    x, y = xy
    draw = ImageDraw.Draw(canvas)
    w = draw.textlength(text, font=fnt)
    h = fnt.size + 2 * pad[1]
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))  # translucent glass pill blended over the background
    ImageDraw.Draw(layer).rounded_rectangle((x, y, x + w + 2 * pad[0], y + h), h // 2, fill=(255, 255, 255, 34), outline=(255, 255, 255, 90), width=2)
    canvas.alpha_composite(layer)
    ImageDraw.Draw(canvas).text((x + pad[0], y + pad[1] - fnt.size * 0.08), text, font=fnt, fill=WHITE)
    return x + w + 2 * pad[0]


def chips(canvas, xy, items, size, max_w):
    d = ImageDraw.Draw(canvas)
    f = font("demi", size)
    x0, (x, y) = xy[0], xy
    for it in items:
        w = d.textlength(it, font=f) + 44
        if x + w > x0 + max_w:
            x, y = x0, y + size + 40
        x = pill(canvas, (x, y), it, f) + 14
    return y + size + 22


# ---------- compositions ----------

def og(lang, w=1200, h=630):
    s = S[lang]
    c = background(w, h, 0.15)
    paste_shadowed(c, tv("tv_backdrop.webp", 560), (600, 120))
    paste_shadowed(c, phone("grid.webp", 420), (500, 190))
    brand(c, (60, 54), 64)
    d = ImageDraw.Draw(c)
    y = text_block(d, (60, 170), s["h1a"], font("bold", 52), 440)
    y = text_block(d, (60, y + 6), s["h1b"], font("medium", 30), 430, fill=(255, 210, 214))
    chips(c, (60, y + 26), s["chips"][:3], 22, 460)
    return c.convert("RGB")


def square(lang, w=1080, h=1080):
    s = S[lang]
    c = background(w, h, 0.1)
    brand(c, (70, 70), 84)
    d = ImageDraw.Draw(c)
    y = text_block(d, (70, 200), s["h1a"], font("bold", 78), 940)
    y = text_block(d, (70, y + 4), s["h1b"], font("medium", 44), 940, fill=(255, 210, 214))
    paste_shadowed(c, tv("tv_backdrop.webp", 760), (300, y + 70))
    paste_shadowed(c, phone("grid.webp", 470), (90, y + 150))
    return c.convert("RGB")


def portrait(lang, w=1080, h=1350):
    s = S[lang]
    c = background(w, h, 0.12)
    brand(c, (70, 70), 84)
    d = ImageDraw.Draw(c)
    y = text_block(d, (70, 200), s["tv_h"], font("bold", 72), 940)
    y = text_block(d, (70, y + 6), s["tv_p"], font("medium", 38), 920, fill=(240, 228, 232))
    paste_shadowed(c, tv("tv_backdrop_spring.webp", 900), (90, y + 60))
    y2 = y + 60 + round(900 * 1080 / 1920) + 120
    for p in s["tv_points"]:
        d.ellipse((76, y2 + 12, 100, y2 + 36), fill=RED)
        y2 = text_block(d, (124, y2), p, font("demi", 36), 880) + 14
    return c.convert("RGB")


def story(lang, w=1080, h=1920):
    s = S[lang]
    c = background(w, h, 0.05)
    brand(c, (80, 140), 96)
    d = ImageDraw.Draw(c)
    y = text_block(d, (80, 300), s["h1a"], font("bold", 92), 920)
    y = text_block(d, (80, y + 6), s["h1b"], font("medium", 52), 920, fill=(255, 210, 214))
    paste_shadowed(c, tv("tv_backdrop.webp", 860), (110, y + 80))
    paste_shadowed(c, phone("grid.webp", 700), (330, y + 300))
    y3 = h - 300
    chips(c, (80, y3), s["chips"], 34, 920)
    d.text((80, h - 150), "gestal.es/supertube", font=font("demi", 40), fill=(255, 255, 255, 220))
    return c.convert("RGB")


def banner(lang, w=1500, h=500):
    s = S[lang]
    c = background(w, h, 0.18)
    brand(c, (60, 60), 72)
    d = ImageDraw.Draw(c)
    y = text_block(d, (60, 170), s["h1a"], font("bold", 50), 620)
    text_block(d, (60, y + 4), s["h1b"], font("medium", 32), 620, fill=(255, 210, 214))
    paste_shadowed(c, tv("tv_backdrop.webp", 640), (780, 40))
    paste_shadowed(c, phone("favorites.webp", 380), (700, 120))
    return c.convert("RGB")


def features(lang, w=1080, h=1350):
    """Carousel slide: the six features as a list."""
    s = S[lang]
    c = background(w, h, 0.35)
    brand(c, (70, 70), 84)
    d = ImageDraw.Draw(c)
    y = text_block(d, (70, 200), s["feat_h"], font("bold", 76), 940) + 30
    for title, txt in s["feats"]:
        d.rounded_rectangle((70, y, w - 70, y + 6), 3, fill=RED)
        y = text_block(d, (70, y + 22), title, font("bold", 40), 940)
        y = text_block(d, (70, y + 2), txt, font("regular", 29), 940, fill=(232, 224, 228), spacing=1.25) + 22
    return c.convert("RGB")


OUTPUTS = [
    ("og_1200x630.jpg", og),
    ("instagram_post_1080x1080.jpg", square),
    ("instagram_tv_1080x1350.jpg", portrait),
    ("instagram_features_1080x1350.jpg", features),
    ("story_1080x1920.jpg", story),
    ("x_header_1500x500.jpg", banner),
]


def main():
    for lang in S:
        out = os.path.join(ROOT, "promo", lang)
        os.makedirs(out, exist_ok=True)
        for name, fn in OUTPUTS:
            fn(lang).save(os.path.join(out, name), "JPEG", quality=90, optimize=True, progressive=True)
        print(out)
    # Pillow writes none today; cheap insurance for images edited or added by hand
    subprocess.run([sys.executable, os.path.join(ROOT, "tools", "strip_metadata.py"), "promo"], check=True)


if __name__ == "__main__":
    main()
