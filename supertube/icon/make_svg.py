import sys
sys.path.insert(0, '.')
from parts import *

def content(mono=False):
    fill_body = WHITE
    s = []
    s.append(f'<path d="{ANTENNAS}" fill="none" stroke="{WHITE}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>')
    s.append(f'<circle cx="45" cy="25" r="2.2" fill="{WHITE}"/><circle cx="63" cy="25" r="2.2" fill="{WHITE}"/>')
    s.append(f'<path d="{LEGS}" fill="none" stroke="{WHITE}" stroke-width="3" stroke-linecap="round"/>')
    if mono:
        s.append(f'<path d="{BODY}" fill="none" stroke="{WHITE}" stroke-width="4"/>')
    else:
        s.append(f'<path d="{BODY}" fill="{fill_body}"/>')
        s.append(f'<path d="{SCREEN_PATH}" fill="{SCREEN}"/>')
    for d in (LETTER_S, LETTER_T):
        s.append(f'<path d="{d}" fill="none" stroke="{WHITE}" stroke-width="4.2" stroke-linecap="round" stroke-linejoin="round"/>')
    return "\n".join(s)

def svg(view, shape, size=1024):
    bg = f'''<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{BG_START}"/><stop offset="1" stop-color="{BG_END}"/></linearGradient>
<clipPath id="c">{shape}</clipPath></defs>'''
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="{view}">{bg}
<g clip-path="url(#c)"><rect x="0" y="0" width="108" height="108" fill="url(#g)"/><g transform="translate(54 54) scale(0.88) translate(-54 -54)">{content()}</g></g></svg>'''

# Legacy icons show the central 72dp area (like a launcher would crop the adaptive icon)
VIEW = "18 18 72 72"
square = '<rect x="18" y="18" width="72" height="72" rx="16" ry="16"/>'
circle = '<circle cx="54" cy="54" r="36"/>'
full = '<rect x="18" y="18" width="72" height="72"/>'
open('icon_square.svg','w').write(svg(VIEW, square))
open('icon_round.svg','w').write(svg(VIEW, circle))
open('icon_playstore.svg','w').write(svg(VIEW, full))
