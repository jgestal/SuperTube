# SuperTube branding for the TV app (smarttubetv) and its home screen channels (leanbackassistant).
# Usage (from supertube/icon): python3 make_tv.py   (needs rsvg-convert)
import os, subprocess, sys
sys.path.insert(0, '.')
from parts import *

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
FLAVORS = ['stbeta', 'stfdroid', 'ststable']
RED, GREY = '#E53935', '#212121'


def glyph():
    s = [f'<path d="{ANTENNAS}" fill="none" stroke="{WHITE}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>',
         f'<circle cx="45" cy="25" r="2.2" fill="{WHITE}"/><circle cx="63" cy="25" r="2.2" fill="{WHITE}"/>',
         f'<path d="{LEGS}" fill="none" stroke="{WHITE}" stroke-width="3" stroke-linecap="round"/>',
         f'<path d="{BODY}" fill="{WHITE}"/>', f'<path d="{SCREEN_PATH}" fill="{SCREEN}"/>']
    for d in (LETTER_S, LETTER_T):
        s.append(f'<path d="{d}" fill="none" stroke="{WHITE}" stroke-width="4.2" stroke-linecap="round" stroke-linejoin="round"/>')
    return ''.join(s)


GRADIENT = (f'<linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{BG_START}"/>'
            f'<stop offset="1" stop-color="{BG_END}"/></linearGradient>')


def icon(x, y, size, shape='square', fill='url(#g)'):
    """The SuperTube icon (72x72 visible area of the adaptive grid) placed at x,y with the given size."""
    k = size / 72.0
    if shape == 'round':
        bg = f'<circle cx="54" cy="54" r="36" fill="{fill}"/>'
    elif shape == 'full':
        bg = f'<rect x="18" y="18" width="72" height="72" fill="{fill}"/>'
    else:
        bg = f'<rect x="18" y="18" width="72" height="72" rx="16" ry="16" fill="{fill}"/>'
    return (f'<g transform="translate({x} {y}) scale({k}) translate(-18 -18)">{bg}'
            f'<g transform="translate(54 54) scale(0.88) translate(-54 -54)">{glyph()}</g></g>')


def svg(w, h, body):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><defs>{GRADIENT}</defs>{body}</svg>'


def render(svg_text, out):
    os.makedirs(os.path.dirname(out), exist_ok=True)
    subprocess.run(['rsvg-convert', '-o', out], input=svg_text.encode(), check=True)
    print(out)


def banner(w, h):
    # Android TV launcher banner (16:9): icon + wordmark on a dark background
    s = h / 360.0
    bg = (f'<defs><linearGradient id="b" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#26232E"/>'
          f'<stop offset="1" stop-color="#121117"/></linearGradient></defs><rect width="{w}" height="{h}" fill="url(#b)"/>')
    size = 150 * s
    text = (f'<text x="{222 * s}" y="{h / 2 + 26 * s}" font-family="Helvetica Neue, Helvetica, Arial, sans-serif" '
            f'font-weight="bold" font-size="{72 * s}" fill="#FFFFFF" letter-spacing="{-1 * s}">Super<tspan fill="{BG_START}">Tube</tspan></text>')
    return svg(w, h, bg + icon(50 * s, (h - size) / 2, size) + text)


def logo(w, h, fill):
    # Top right corner logo of the browse screen (one per theme)
    size = min(w, h)
    return svg(w, h, icon((w - size) / 2, (h - size) / 2, size, fill=fill))


res = lambda module, flavor, folder, name: os.path.join(ROOT, module, 'src', flavor, 'res', folder, name)

for flavor in ['main'] + FLAVORS:
    main = flavor == 'main'
    lw, lh = (320, 180) if main else (180, 180)
    render(logo(lw, lh, 'url(#g)'), res('smarttubetv', flavor, 'mipmap-nodpi', 'app_logo.png'))
    render(logo(lw, lh, RED), res('smarttubetv', flavor, 'mipmap-nodpi', 'app_logo_semi_red.png'))
    render(logo(lw, lh, GREY), res('smarttubetv', flavor, 'mipmap-nodpi', 'app_logo_semi_grey.png'))
    render(svg(320, 320, icon(0, 0, 320)), res('smarttubetv', flavor, 'mipmap-nodpi', 'app_icon.png'))
    if main:
        render(svg(320, 320, icon(0, 0, 320, 'round')), res('smarttubetv', flavor, 'mipmap-nodpi', 'app_icon_alt.png'))
        render(banner(320, 180), res('smarttubetv', flavor, 'mipmap-nodpi', 'app_banner.png'))
    else:
        render(svg(320, 320, icon(0, 0, 320)), res('smarttubetv', flavor, 'mipmap-nodpi-v30', 'app_icon.png'))
        render(banner(640, 360), res('smarttubetv', flavor, 'mipmap-nodpi', 'app_banner.png'))
    # Android TV home screen channel logo
    render(svg(300, 300, icon(0, 0, 300, 'full')), res('leanbackassistant', flavor, 'drawable', 'generic_channels.png'))


def ambient(out, w=1280, h=720):
    """Default TV background (no video focused yet): soft brand-colored light over a dark base."""
    from PIL import Image, ImageDraw, ImageFilter
    import random
    img = Image.new('RGB', (w, h), (18, 16, 24))
    glow = Image.new('RGB', (w, h), (0, 0, 0))
    d = ImageDraw.Draw(glow)
    # (center x, center y, radius, color) relative to the image size
    for cx, cy, r, color in [(0.70, 0.18, 0.42, (255, 61, 46)), (0.98, 0.55, 0.40, (194, 24, 91)),
                             (0.40, -0.05, 0.30, (90, 40, 140)), (0.85, 0.95, 0.25, (120, 20, 70))]:
        R = r * h
        d.ellipse([cx * w - R, cy * h - R, cx * w + R, cy * h + R], fill=color)
    glow = glow.filter(ImageFilter.GaussianBlur(h * 0.16))
    img = Image.blend(img, glow, 0.55)
    # Thin diagonal light beams for some depth
    beams = Image.new('L', (w, h), 0)
    bd = ImageDraw.Draw(beams)
    for x, width in [(0.55, 0.05), (0.68, 0.025), (0.80, 0.07)]:
        bd.polygon([(x * w, 0), ((x + width) * w, 0), ((x + width - 0.25) * w, h), ((x - 0.25) * w, h)], fill=26)
    beams = beams.filter(ImageFilter.GaussianBlur(h * 0.03))
    img = Image.composite(Image.new('RGB', (w, h), (255, 200, 210)), img, beams)
    # Fine grain against color banding on 8-bit TV panels
    random.seed(7)
    noise = Image.effect_noise((w, h), 6).convert('RGB')
    img = Image.blend(img, noise, 0.03)
    img.save(out, 'JPEG', quality=90, optimize=True, progressive=False)
    print(out)


ambient(res('smarttubetv', 'main', 'drawable-nodpi', 'supertube_ambient.jpg'))
