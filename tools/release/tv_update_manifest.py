#!/usr/bin/env python3
"""Writes update/supertube_tv.json, the manifest the TV app (stable build) reads to find updates.

The TV app inherits the SmartTube update checker (SharedModules/appupdatechecker2): it compares its versionCode with
the manifest, shows the changelog and downloads the APK for the device ABI. Run this when publishing a release, after
setting versionName/versionCode in smarttubetv/build.gradle and writing the CHANGELOG.md section, then commit the JSON
and attach the TV APKs (SuperTubeTV_stable_<version>_<abi>.apk) to the GitHub release v<version>.

    python3 tools/release/tv_update_manifest.py            # version from smarttubetv/build.gradle
    python3 tools/release/tv_update_manifest.py --check    # only print it
"""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPO = "https://github.com/jgestal/SuperTube"
ABIS = ["armeabi-v7a", "arm64-v8a", "x86"]  # the splits in smarttubetv/build.gradle
OUT = ROOT / "update" / "supertube_tv.json"


def gradle_version():
    text = (ROOT / "smarttubetv" / "build.gradle").read_text()
    name = re.search(r'versionName "([^"]+)"', text).group(1)
    code = int(re.search(r"versionCode (\d+)", text).group(1))
    return name, code


def changelog(version):
    """Bullets of the CHANGELOG.md section, as plain text (the TV dialog shows one line per item)."""
    text = (ROOT / "CHANGELOG.md").read_text()
    section = re.search(r"^## " + re.escape(version) + r"\b.*?$(.*?)(?=^## |\Z)", text, re.M | re.S)
    if not section:
        raise SystemExit(f"CHANGELOG.md has no '## {version}' section")
    items, indent = [], None
    for line in section.group(1).splitlines():
        bullet = re.match(r"( *)- (.*)", line)
        if bullet:
            indent = len(bullet.group(1)) + 2
            items.append(bullet.group(2).strip())  # nested bullets become items of their own
        elif indent is not None and line.startswith(" " * indent) and line.strip():
            items[-1] += " " + line.strip()  # wrapped line of the same item
        else:
            indent = None  # headings and blank lines
    items = [i for i in items if not i.endswith(":")]  # "Feature, with:" headers of nested lists
    plain = [re.sub(r"[*`]", "", re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", i)) for i in items]
    return [p for p in plain if p]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="print the manifest without writing it")
    args = parser.parse_args()

    name, code = gradle_version()
    base = f"{REPO}/releases/download/v{name}/SuperTubeTV_stable_{name}"
    package = {f"downloadUrlList_{abi}": [f"{base}_{abi}.apk"] for abi in ABIS}
    package["downloadUrlList"] = [f"{base}_universal.apk"]
    manifest = {"package": package, name: {"versionCode": code, "changelog": changelog(name)}}

    text = json.dumps(manifest, indent=2, ensure_ascii=False) + "\n"
    if args.check:
        print(text)
        return
    OUT.parent.mkdir(exist_ok=True)
    OUT.write_text(text)
    print(f"{OUT.relative_to(ROOT)}: {name} ({code}), {len(manifest[name]['changelog'])} changelog items")


if __name__ == "__main__":
    main()
