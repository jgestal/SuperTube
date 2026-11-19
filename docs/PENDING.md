# Pending work

Known issues that are understood but not fixed yet, with what was already tried.

## Phone: SuperTube shows up twice in Recents

**Symptom.** After opening a video, the system Recents screen lists two SuperTube cards: the browsing screens
(home, search, channel...) and the player.

**Cause.** `PlaybackActivity` runs in a task of its own (`android:taskAffinity="${applicationId}.player"` in
`supertube/src/main/AndroidManifest.xml`). The mini player relies on it: the browsing task comes to the front while the
player task keeps playing behind it. Android lists every task as a separate Recents entry.

**Tried and reverted (2026-11-19): hiding the task that isn't in front.** Each screen called
`ActivityManager.AppTask.setExcludeFromRecents()` so that only the visible task was listed. It looked right on the
emulator, but Android *trims* excluded tasks: once another task comes to the front, the hidden one is destroyed
(events log: `wm_destroy_activity ... finish-imm:recent-task-trimmed`, `wm_task_removed_from_tasklist ... trimed`).
On a Xiaomi (HyperOS 3) the browsing task died while the player was in front, so back from the player had nowhere
to go and the app vanished from Recents. The same would happen to the player task (stopping background playback)
when going Home from the mini player. **Never hide one of the app's live tasks from Recents.**

**The real fix: one task for the whole app.** Move the player into the main task (drop its `taskAffinity`). Things
to solve:
- The browsing screens are `singleTask`: starting one while the player is above it in the same task clears the
  player (`clear top`). Navigation (`ViewManager.startParentView`, the mini player) has to bring screens to the front
  without destroying the player, e.g. standard launch mode plus `FLAG_ACTIVITY_REORDER_TO_FRONT`.
- PIP from an activity that isn't alone in its task: Android moves it to a pinned task of its own. Android 12+ puts
  it back in the original task when expanded; Android 8-11 may leave it apart (two Recents entries again).
- Re-test: mini player, back from the player, PIP (Home / Recents button / HyperOS), background audio with the
  screen off, links opened from other apps (player only mode), the media notification, Android Auto.

Cosmetic only (both entries work), so it was left out of 1.5.5.
