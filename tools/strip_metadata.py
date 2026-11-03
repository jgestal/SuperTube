#!/usr/bin/env python3
"""Removes metadata from images without re-encoding them: EXIF, XMP, IPTC, comments, text chunks and C2PA / JUMBF
content credentials (the "made with AI" labels some image generators and editors embed). Pixels are untouched.

Keeps only what is needed to show the image right: colour profile (ICC, sRGB, gamma), transparency and animation.

    python3 tools/strip_metadata.py                 # every project image (not vendored libraries)
    python3 tools/strip_metadata.py promo landing   # only these folders or files
    python3 tools/strip_metadata.py --check         # list what would be removed, change nothing

Invisible watermarks drawn into the pixels themselves (e.g. SynthID) are not metadata and are not removed.
"""
import struct
import subprocess
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXTS = {".png", ".jpg", ".jpeg", ".webp"}
# Third-party code copied into the repo: left as upstream ships it
VENDORED = ("exoplayer-amzn-2.10.6/", "SharedModules/", "MediaServiceCore/", "leanback-1.0.0/", "chatkit/")
# Also images that are generated but not in git (landing/apk is APKs only)
EXTRA_DIRS = ("landing", "promo")

PNG_SIG = b"\x89PNG\r\n\x1a\n"
PNG_KEEP = {b"IHDR", b"PLTE", b"IDAT", b"IEND", b"tRNS", b"gAMA", b"cHRM", b"sRGB", b"iCCP", b"sBIT", b"pHYs", b"bKGD",
            b"acTL", b"fcTL", b"fdAT"}
WEBP_KEEP = {b"VP8 ", b"VP8L", b"VP8X", b"ALPH", b"ICCP", b"ANIM", b"ANMF"}


def strip_png(data):
    out, removed, pos = [PNG_SIG], [], 8
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        end = pos + 12 + length
        (out.append(data[pos:end]) if kind in PNG_KEEP else removed.append(kind.decode("latin-1")))
        pos = end
        if kind == b"IEND":
            break
    return b"".join(out), removed


def strip_jpeg(data):
    out, removed, pos = [data[:2]], [], 2
    while pos < len(data):
        if data[pos] != 0xFF:
            raise ValueError("bad JPEG marker")
        marker = data[pos + 1]
        if marker == 0xFF:  # fill byte
            pos += 1
            continue
        if marker == 0xDA:  # start of scan: the rest is image data
            out.append(data[pos:])
            break
        if 0xD0 <= marker <= 0xD9 or marker == 0x01:  # markers without a length
            out.append(data[pos:pos + 2])
            pos += 2
            continue
        length = struct.unpack(">H", data[pos + 2:pos + 4])[0]
        seg = data[pos:pos + 2 + length]
        app = 0xE0 <= marker <= 0xEF
        keep = (not app and marker != 0xFE) or marker == 0xE0 or marker == 0xEE \
            or (marker == 0xE2 and seg[4:16] == b"ICC_PROFILE\0")
        (out.append(seg) if keep else removed.append("COM" if marker == 0xFE else f"APP{marker - 0xE0}"))
        pos += 2 + length
    return b"".join(out), removed


def strip_webp(data):
    if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise ValueError("bad WebP header")
    chunks, removed, pos = [], [], 12
    while pos + 8 <= len(data):
        kind, size = struct.unpack("<4sI", data[pos:pos + 8])
        end = pos + 8 + size + (size & 1)
        (chunks.append(bytearray(data[pos:end])) if kind in WEBP_KEEP else removed.append(kind.decode("latin-1").strip()))
        pos = end
    if removed:
        for c in chunks:
            if c[:4] == b"VP8X":
                c[8] &= ~0x0C & 0xFF  # clear the EXIF and XMP flags
    body = b"WEBP" + b"".join(chunks)
    return b"RIFF" + struct.pack("<I", len(body)) + body, removed


def strip(path):
    data = path.read_bytes()
    if data.startswith(PNG_SIG):
        return strip_png(data)
    if data[:2] == b"\xff\xd8":
        return strip_jpeg(data)
    if data[:4] == b"RIFF":
        return strip_webp(data)
    raise ValueError("unknown format")


def same_pixels(before, after):
    try:
        import io
        from PIL import Image
    except ImportError:
        return True
    a, b = Image.open(io.BytesIO(before)), Image.open(io.BytesIO(after))
    return a.size == b.size and a.convert("RGBA").tobytes() == b.convert("RGBA").tobytes()


def project_images(targets):
    if targets:
        files = []
        for t in targets:
            p = (ROOT / t) if not Path(t).is_absolute() else Path(t)
            files += [p] if p.is_file() else [f for f in p.rglob("*") if f.is_file()]
    else:
        tracked = subprocess.run(["git", "ls-files"], cwd=ROOT, capture_output=True, text=True, check=True).stdout
        files = [ROOT / f for f in tracked.splitlines() if not f.startswith(VENDORED)]
        for d in EXTRA_DIRS:
            files += [f for f in (ROOT / d).rglob("*") if f.is_file()]
    return sorted({f for f in files if f.suffix.lower() in EXTS and "/build/" not in str(f)})


def main():
    args = sys.argv[1:]
    check = "--check" in args
    files = project_images([a for a in args if a != "--check"])
    changed = saved = 0
    for f in files:
        before = f.read_bytes()
        after, removed = strip(f)
        if not removed:
            continue
        if not same_pixels(before, after):
            sys.exit(f"ERROR {f.relative_to(ROOT)}: pixels would change, left as is")
        changed += 1
        saved += len(before) - len(after)
        print(f"{'would strip' if check else 'stripped'}  {f.relative_to(ROOT)}  [{', '.join(sorted(set(removed)))}]")
        if not check:
            f.write_bytes(after)
    print(f"{len(files)} images checked, {changed} {'with metadata' if check else 'cleaned'}, {saved / 1024:.0f} KB")


if __name__ == "__main__":
    main()
