#!/usr/bin/env bash
# Publishes a SuperTube release with both apps, in one go:
#   1. checks: same versionName in both apps, CHANGELOG section, signing configs, clean git tree, gh login
#   2. unit tests, then signed release builds of the TV app (ststable) and the phone app (stbeta)
#   3. verifies that every APK is signed with the release certificate listed in the README
#   4. writes the TV update manifest (update/supertube_tv.json) and commits it
#   5. creates the GitHub release (draft until every APK is uploaded), then pushes the manifest: TVs only see the
#      update once the APKs can be downloaded
#   6. regenerates the website so its download buttons point to the new APKs
#
#   tools/release/release.sh            # asks before anything is pushed or published
#   tools/release/release.sh --dry-run  # stops after building and checking the APKs
#
# Needs JAVA_HOME (JDK 17) and ANDROID_HOME, keystore.properties (TV) and supertube/keystore.properties (phone).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
DRY_RUN=0
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=1

die() { echo "ERROR: $*" >&2; exit 1; }
step() { echo; echo "==> $*"; }

# ---------- 1. checks ----------
step "Checks"
TV_VERSION=$(sed -n 's/.*versionName "\(.*\)"/\1/p' smarttubetv/build.gradle | head -1)
PHONE_VERSION=$(sed -n 's/.*versionName "\(.*\)"/\1/p' supertube/build.gradle | head -1)
[[ "$TV_VERSION" == "$PHONE_VERSION" ]] || die "versionName differs: TV $TV_VERSION, phone $PHONE_VERSION"
VERSION="$TV_VERSION"
TAG="v$VERSION"
V_RE="${VERSION//./\\.}"
grep -Eq "^## $V_RE( |$)" CHANGELOG.md || die "CHANGELOG.md has no '## $VERSION' section"
if grep -Eq "^## $V_RE .*unreleased" CHANGELOG.md; then
  (( DRY_RUN )) && echo "warning: CHANGELOG.md still says '$VERSION — unreleased'" \
    || die "CHANGELOG.md still says '## $VERSION — unreleased': set the date"
fi
[[ -f keystore.properties ]] || die "missing keystore.properties (TV signing)"
[[ -f supertube/keystore.properties ]] || die "missing supertube/keystore.properties (phone signing)"
[[ -n "${JAVA_HOME:-}" && -n "${ANDROID_HOME:-}" ]] || die "set JAVA_HOME (JDK 17) and ANDROID_HOME"
if (( ! DRY_RUN )); then
  [[ -z "$(git status --porcelain)" ]] || die "uncommitted changes: commit them first"
  [[ "$(git rev-parse --abbrev-ref HEAD)" == "main" ]] || die "release from the main branch"
  gh auth status >/dev/null 2>&1 || die "gh is not logged in (gh auth login)"
  git fetch -q origin main
  [[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/main)" ]] || die "push main first: the release tag is made on GitHub's main"
  gh release view "$TAG" >/dev/null 2>&1 && die "release $TAG already exists"
fi
APKSIGNER=$(ls -d "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
[[ -x "$APKSIGNER" ]] || die "apksigner not found in $ANDROID_HOME/build-tools"
CERT=$(grep -o '[0-9a-f]\{2\}\(:[0-9a-f]\{2\}\)\{31\}' README.md | head -1 | tr -d ':')
[[ -n "$CERT" ]] || die "release certificate SHA-256 not found in README.md"
echo "SuperTube $VERSION ($TAG)"

# ---------- 2. build ----------
step "Unit tests and release builds"
sh ./gradlew -q clean :supertube:testStbetaDebugUnitTest \
  :smarttubetv:assembleStstableRelease :supertube:assembleStbetaRelease

DIST="$ROOT/build/release-$VERSION"
rm -rf "$DIST" && mkdir -p "$DIST"
cp smarttubetv/build/outputs/apk/ststable/release/SuperTubeTV_stable_"$VERSION"_*.apk "$DIST/"
cp supertube/build/outputs/apk/stbeta/release/SuperTube_"$VERSION"_release.apk "$DIST/SuperTube-$VERSION.apk"
for abi in armeabi-v7a arm64-v8a x86 universal; do
  [[ -f "$DIST/SuperTubeTV_stable_${VERSION}_$abi.apk" ]] || die "missing TV APK for $abi"
done

# ---------- 3. signatures ----------
step "Signatures"
for apk in "$DIST"/*.apk; do
  digest=$("$APKSIGNER" verify --print-certs "$apk" 2>/dev/null | sed -n 's/.*certificate SHA-256 digest: //p' | head -1)
  [[ "$digest" == "$CERT" ]] || die "$(basename "$apk") is not signed with the release certificate ($digest)"
  echo "ok  $(basename "$apk")"
done

# Release notes: the CHANGELOG section of this version
NOTES="$DIST/notes.md"
awk -v v="## $VERSION" 'index($0, v) == 1 && (length($0) == length(v) || substr($0, length(v) + 1, 1) == " ") {on=1; next}
  /^## / {on=0} on' CHANGELOG.md > "$NOTES"
cat >> "$NOTES" <<EOF

**Which file?** \`SuperTube-$VERSION.apk\` is the phone and tablet app. \`SuperTubeTV_stable_${VERSION}_universal.apk\`
is the Android TV app for every TV (\`arm64-v8a\` and \`armeabi-v7a\` are smaller builds for one processor type).
EOF

if (( DRY_RUN )); then
  step "Dry run: APKs and notes in ${DIST#$ROOT/}"
  exit 0
fi

# ---------- 4. TV update manifest ----------
step "TV update manifest"
python3 tools/release/tv_update_manifest.py
git add update/supertube_tv.json
git commit -q -m "chore: TV update manifest $VERSION"

echo
ls -1 "$DIST"
read -r -p "Publish $TAG on GitHub with these files and push the manifest? [y/N] " answer
[[ "$answer" == [yY] ]] || die "stopped: the manifest commit is local (git reset HEAD~1 to undo it)"

# ---------- 5. GitHub release ----------
step "GitHub release"
# Draft first so the APKs are uploaded before anyone sees the release. The tag goes on GitHub's main (the release
# code); the manifest is pushed after publishing, when its download links already work.
gh release create "$TAG" "$DIST"/*.apk --draft --target main --title "SuperTube $VERSION" --notes-file "$NOTES"
gh release edit "$TAG" --draft=false --latest
git push -q origin main
gh release view "$TAG" --json url -q .url

# ---------- 6. website ----------
step "Website"
python3 tools/landing/gen_landing.py
echo
echo "Done. Upload landing/ to the web server."
