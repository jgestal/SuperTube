#!/usr/bin/env bash
# End-to-end test of the player fullscreen and rotation on an Android emulator: the accelerometer is simulated with
# `adb emu sensor`, the real screen rotation is read from the window manager.
#
#   supertube/tools/rotation_e2e.sh path/to/SuperTube.apk [emulator-serial]
#
# Requires a running emulator with Google APIs and network (it plays a Creative Commons video).
# Restores the auto-rotate setting at the end. Exit code: number of failed checks.
set -u

APK=${1:?usage: rotation_e2e.sh path/to/SuperTube.apk [emulator-serial]}
export ANDROID_SERIAL=${2:-emulator-5554}
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
VIDEO="https://www.youtube.com/watch?v=WhWc3b3KhnY" # Spring, Blender Studio (CC BY)
FAILED=0

case "$ANDROID_SERIAL" in emulator-*) ;; *) echo "Only for emulators (the sensor is simulated)"; exit 1 ;; esac

# Gravity on the device axes (m/s²)
portrait()  { "$ADB" emu sensor set acceleration 0:9.81:0 >/dev/null; }
landscape() { "$ADB" emu sensor set acceleration 9.81:0:0 >/dev/null; }
flat()      { "$ADB" emu sensor set acceleration 0:0:9.81 >/dev/null; }

rotation() { "$ADB" shell dumpsys window | grep -m1 -oE 'mCurrentRotation=ROTATION_[0-9]+' | grep -oE '[0-9]+$'; }

expect() { # expect <portrait|landscape> <description>
    local r; r=$(rotation)
    local actual=portrait; [ "$r" = 90 ] || [ "$r" = 270 ] && actual=landscape
    if [ "$actual" = "$1" ]; then echo "  ok    $2"; else echo "  FAIL  $2 (expected $1, screen is $actual)"; FAILED=$((FAILED + 1)); fi
}

# Taps a control of the player overlay by its accessibility label, showing the overlay first if needed
tap_control() {
    local label=$1 bounds
    for _ in 1 2 3; do
        bounds=$("$ADB" exec-out uiautomator dump /dev/tty 2>/dev/null | tr '>' '\n' | grep "content-desc=\"$label\"" | grep -m1 -oE 'bounds="[^"]+"')
        if [ -n "$bounds" ]; then
            read -r x1 y1 x2 y2 <<<"$(echo "$bounds" | grep -oE '[0-9]+' | tr '\n' ' ')"
            "$ADB" shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
            return 0
        fi
        # Overlay hidden: tap the video (top of the screen) to show it
        "$ADB" shell input tap 540 450
        sleep 1
    done
    echo "  FAIL  control \"$label\" not found"; FAILED=$((FAILED + 1)); return 1
}

AUTO_ROTATE=$("$ADB" shell settings get system accelerometer_rotation)
trap '"$ADB" shell settings put system accelerometer_rotation "$AUTO_ROTATE"; portrait' EXIT

"$ADB" uninstall app.supertube >/dev/null 2>&1
"$ADB" install "$APK" >/dev/null || exit 1
"$ADB" shell cmd locale set-app-locales app.supertube --locales en-US
"$ADB" shell am force-stop app.supertube
portrait
"$ADB" shell settings put system accelerometer_rotation 1
"$ADB" shell monkey -p app.supertube -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 6
"$ADB" shell am start -a android.intent.action.VIEW -d "$VIDEO" -p app.supertube >/dev/null
sleep 10

echo "Auto-rotate on"
expect portrait "opens in portrait"
landscape; sleep 3
expect landscape "turning the phone enters fullscreen"
"$ADB" shell input keyevent BACK; sleep 3
expect portrait "back leaves fullscreen"
sleep 2
expect portrait "stays in portrait while still held in landscape"
flat; sleep 2; landscape; sleep 3
expect landscape "phone left on a table, then picked up in landscape: fullscreen again"
portrait; sleep 3
expect portrait "turning back to portrait leaves fullscreen"
tap_control "Fullscreen"; sleep 3
expect landscape "fullscreen button rotates to landscape"
landscape; sleep 3; portrait; sleep 3
expect portrait "after holding it in landscape, the screen follows the phone again"

echo "Auto-rotate off"
"$ADB" shell settings put system accelerometer_rotation 0
portrait; sleep 2
tap_control "Fullscreen"; sleep 3
expect landscape "fullscreen button rotates to landscape"
landscape; sleep 4
expect landscape "turning the phone keeps the fullscreen (rotation lock doesn't undo it)"
tap_control "Exit fullscreen"; sleep 3
expect portrait "exit fullscreen button goes back to portrait"

echo
[ "$FAILED" -eq 0 ] && echo "All checks passed" || echo "$FAILED check(s) failed"
exit "$FAILED"
