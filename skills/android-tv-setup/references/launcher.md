# Launcher

A small native launcher, Apple TV 4K style: pitch black background, a dock band at the bottom with the owner's
apps, the focused dock app's name above it with a glow sampled from its banner, all other apps in a grid below
(same columns) that scrolls up on Down, one glass highlight that springs between tiles, greeting + large thin
clock + date top left (Onest, OFL, Cyrillic), frosted pills top right: Inputs (HDMI/AV/Live TV dropdown via the
TV input framework), Wi-Fi, Settings. Deterministic D-pad: Up/Down move one row, Up from the dock goes to the top
bar, the bar's ends don't wrap, Back returns to the dock, Home on Home returns to the dock, the last launched
tile is focused on return.

## 1. Can this TV take it?

- SDK: `getprop ro.build.version.sdk` must be >= `minSdk` in `launcher/app/build.gradle` (26 = Android 8.0).
  Below that, `INSTALL_FAILED_OLDER_SDK`: offer the debloat half only; the stock launcher stays.
- Sideloading: some China-market sets answer every install with `INSTALL_FAILED_VERIFICATION_FAILURE`, others
  refuse any package declaring `HOME` without the vendor's signature. If so, say plainly the launcher half is
  impossible here and stop retrying.
- Requirements on the PC: Android SDK (platforms 35), JDK 17+ (Android Studio's `jbr` works), internet for the
  first Gradle run.

## 2. Configure

Ask Q-L1 and Q-L2. `build-install.sh` copies the source into the work directory on first run; edit
`launcher/app/src/main/res/values/config.xml` there:

- `columns`: dock size; the grid uses the same.
- `dock_apps`: package names, left to right. Get names from `backup/launchable.txt`.
- `live_tv_package`: the app that plays the tuner. Find it:
  `cmd package query-activities --brief -a android.intent.action.VIEW -t vnd.android.cursor.dir/channel`.
- `hidden_apps`: packages to keep out of the grid.
- `dpi_apps`: apps that open with the ByeByeDPI bypass on; it is stopped when Home comes back.
  Empty by default. Set up ByeByeDPI first (`dpi-bypass.md`).

## 3. Build, install, try it before taking Home

```sh
bash <skill>/scripts/build-install.sh --no-home
python <skill>/scripts/tv.py 'am start -n com.cleantv.launcher/.MainActivity'
python <skill>/scripts/tv.py --screenshot shots/10-launcher.png
```

Test the D-pad with a fixed sequence so the result is predictable (ask the owner not to touch the remote):
`UP DOWN LEFT LEFT LEFT LEFT RIGHT RIGHT DOWN` must land on grid column 3. Test the top bar ends and the Inputs
menu; selecting an HDMI input gives an empty screenshot and `topResumedActivity` = the TV app. Fix what you see.

## 4. Take over Home

```sh
bash <skill>/scripts/build-install.sh
```

It runs `set-home-activity`, presses Home and prints `mFocusedApp`. If that is our launcher, done. Otherwise list
the candidates with their priority:

```sh
python <skill>/scripts/tv.py 'cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME' | grep -E "priority|/"
```

A third-party app's HOME filter is capped at priority 0, so any system candidate with a higher priority wins
regardless of the preferred setting. The stock launcher has to be disabled.

### Google TV (stock home `com.google.android.apps.tv.launcherx`)

Candidates are typically: `launcherx` (priority 2), `setupwraith/.RecoveryActivity` (priority 1), Settings'
`FallbackHome` (-1000). With only `launcherx` disabled, `RecoveryActivity` takes Home, tries to re-enable
`launcherx`, fails and loops on a black screen. The shell cannot disable that single component
(`SecurityException: Shell cannot change component state`), so the whole setup wizard has to go - and the wizard
usually owns the Wi-Fi network saved during first setup.

1. `dumpsys wifi | grep -o 'cname=[^ ]*'`. If it names `setupwraith`, ask Q-L3.
2. Option A: the owner opens Settings > Network, **forgets** the network and joins it again. Wait, then find the
   TV again (the IP may change), reconnect, accept the debug prompt. The owner must now be `com.android.tv.settings`
   or `android.uid.system`.
3. Write `debloat/batch3-launcher-takeover.txt` with `com.google.android.tungsten.setupwraith` and
   `com.google.android.apps.tv.launcherx`, run `disable.py` (it re-checks the Wi-Fi owner), then
   `build-install.sh` again and check `mFocusedApp`.
4. Black screen at any point: `pm enable com.google.android.apps.tv.launcherx` and press Home.

On Google TV the stock launcher also handled some remote buttons. Known: the gear/dashboard button sends
`TOGGLE_NOTIFICATION_HANDLER_PANEL`, which SystemUI only resolves to a system app, so it goes dead and no
third-party app can take it; the Input button keeps working (the TV app handles it). Record the dead buttons for
the remote module and point the owner to the Settings pill.

### Other launchers

- CVTE boards: keep `com.cvte.tv.launcher.*` enabled, make ours Home with `set-home-activity`, reboot once and
  confirm Home and ADB come back. A JVC with both a launcher swap and a key helper in one pass hung at boot.
- `set-home-activity` printing `Success` while `mFocusedApp` shows the old launcher: same as Google TV, find the
  higher-priority candidate.

## 5. Display problems

If the screen looks enlarged or clipped after install or reboot, read `wm size`, `wm density` and
`settings get secure display_density_forced` (see `backup/display.txt`) before touching the layout. An empty
`display_density_forced` is normal. Put back the original values if something overrode them.

## 5a. Skeleton tiles with shimmer on cold start

Owner feedback (Skyworth m7332, 1.8 GB): after power-on the screen stays black, then the launcher pops in at once and
"feels like the TV was off". Implemented (`ShimmerDrawable`, `SkeletonAdapter`); first verified on CHiQ AI PONT:
cold start (`force-stop` + Home) gave `Displayed +776ms`, then one frame of chrome + 4 plates, then the tiles.
Quick check without a reboot: `am force-stop com.cleantv.launcher`, start `screenrecord`, press Home, pull the mp4,
make a contact sheet (`ffmpeg -i cold.mp4 -vf "fps=10,scale=480:-1,tile=5x5" -frames:v 1 sheet.png`).
The black frames before the first one are the process start; skeletons cannot cover them.

- First frame shows the chrome (clock, date, pills, dock band) plus placeholder tiles, never an empty band.
- Placeholders: one `ShimmerDrawable` per tile = rounded plate `#1C1C1E` (the banner tile background, so the real tile
  lands on the same plate) + a soft white band (`#00FFFFFF -> #12FFFFFF -> #00FFFFFF`, 60% of the tile width)
  sweeping left to right. One shared `ValueAnimator` (1.4 s, repeat, ease in-out) drives all plates in sync.
- Count: the last real dock/grid sizes saved in prefs (`dock_n`, `grid_n`), `columns` on the first run. Hide
  `gridTitle` when `grid_n` is 0.
- `SkeletonAdapter` (inflates `item_tile`, plate as card background, label alpha 0) set in `onCreate` before
  `reloadApps()`. No `applyFocus` on placeholders; `activate()` already ignores an empty repo.
- On the first `showApps`: cancel the animator, swap in `TileAdapter`, fade only the images in (alpha 0 -> 1, 300 ms)
  so the plates stay and the content appears on them. Later reloads (package changes) swap without animation.
- Keep it light: stop the animator as soon as the apps are shown; no blur, no per-frame allocations in `draw`.
- Verify: reboot, record the screen from power-on (`screenrecord`, then pull and look at the frames), and check
  `ActivityTaskManager: Displayed com.cleantv.launcher` in logcat. Also check what the black gap before the first
  frame is (boot animation end -> launcher process start): skeletons only cover the time after the first frame.

## 6. Wallpaper and changes later

- Wallpaper: push a JPG to `/sdcard/Android/data/com.cleantv.launcher/files/wallpaper.jpg`, force-stop the
  launcher, press Home. Darkening: `wallpaper_dim` in config.xml.
- Dock or order: edit config.xml, run `build-install.sh`.
- Wi-Fi name on the pill (Q-L4): needs location. `pm grant com.cleantv.launcher android.permission.ACCESS_FINE_LOCATION`
  and `setting.py put secure location_mode 3`. Without it the pill reads "Wi-Fi".
