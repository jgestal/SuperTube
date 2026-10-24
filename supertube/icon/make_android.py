import sys, os
sys.path.insert(0, '.')
from parts import *
res = sys.argv[1]

def group(children):
    return ('    <group android:pivotX="54" android:pivotY="54" android:scaleX="0.88" android:scaleY="0.88">\n'
            + children + '    </group>\n')

def stroke(d, w, cap="round"):
    return (f'        <path android:pathData="{d}" android:strokeColor="#FFFFFFFF" android:strokeWidth="{w}"\n'
            f'            android:strokeLineCap="{cap}" android:strokeLineJoin="round" android:fillColor="#00000000" />\n')

def fill(d, color):
    return f'        <path android:pathData="{d}" android:fillColor="{color}" />\n'

def circle(cx, cy, r):
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0"

def vector(comment, body):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- {comment} -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n' + body + '</vector>\n')

common = stroke(ANTENNAS, 3) + fill(circle(45, 25, 2.2), "#FFFFFFFF") + fill(circle(63, 25, 2.2), "#FFFFFFFF") + stroke(LEGS, 3)
letters = stroke(LETTER_S, 4.2) + stroke(LETTER_T, 4.2)

fg = common + fill(BODY, "#FFFFFFFF") + fill(SCREEN_PATH, "#FF1C1B22") + letters
mono = common + stroke(BODY, 4, "butt") + letters

d = os.path.join(res, 'drawable')
open(os.path.join(d, 'ic_launcher_foreground.xml'), 'w').write(vector("SuperTube icon: TV with \"ST\" on the screen (generated from the SVG master)", group(fg)))
open(os.path.join(d, 'ic_launcher_monochrome.xml'), 'w').write(vector("SuperTube themed icon (Android 13+): outline TV with \"ST\"", group(mono)))
