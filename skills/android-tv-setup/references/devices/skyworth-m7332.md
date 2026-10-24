# Skyworth m7332 (MediaTek), Google TV (Android 14)

Seen on: HARPER `SWTV-22AE` (`ro.sky.config.brand=HARPER`, `ro.product.brand=SW`, manufacturer `skyworth`),
device `SW6H`, build `UTT1.250517.001`, 1.8 GB RAM, armeabi-v7a, 1080p. ODM packages `com.smartdevice.*`,
`com.tianci.*`, `com.coocaa.*`. `ro.adb.secure=1`.

## Connect

Wireless debugging showed a random TLS port (`_adb-tls-connect`), but mDNS also advertised `_adb._tcp` on 5555:
`adb connect IP:5555` worked straight away (prompt on screen, no pairing code). Toybox `grep` has no `\s`.

## Result

Debloat, speed and Home takeover worked. MemAvailable 721 MB -> 888 MB. Wi-Fi owner was `android.uid.system`, so
`tungsten.setupwraith` + `launcherx` were disabled without re-joining Wi-Fi.

## Packages

| Package | What it is | Verdict |
|---|---|---|
| `com.smartdevice.livetv` | TV app: tuner, inputs, OSD, sound menu; Live TV launcher entry | keep |
| `com.smartdevice.bleconnectservice` | BLE remote connection, remote battery | keep |
| `com.smartdevice.dailyshortcut` | Art Time, karaoke, AI sense, IP control (SDDP), Firebase; catches `GLOBAL_BUTTON`, `SHOW_GAME_CONSOLE` | kept: may own remote keys |
| `com.smartdevice.myapp` | "My apps" list, `MY_APP_KEY_EVENT` receiver (an app key) | kept |
| `com.smartdevice.dailymanage` | "Daily Manage", boot receiver only | kept, unclear |
| `com.smartdevice.aftersale` | service tool: USB-RS232, install APP, boot + net receivers | kept (service/factory adjacent) |
| `com.smartdevice.setupwraith` | vendor first-run + privacy screens | kept |
| `com.smartdevice.leanbacklauncher.customizer` | partner config for the stock launcher | kept |
| `com.smartdevice.factory`, `mediatek.factorymenu.ui` | factory | never touch |
| `com.smartdevice.recommendation` | content rows, Sonos, mail notifications | was already disabled |
| `com.smartdevice.epopplayer` | in-store ePOP promo | disabled |
| `com.coocaa.demonow.lightining`, `fusion.android.tv.demo` | store demo | disabled |
| `com.smartdevice.dtv.icast` | AirPlay + DLNA renderer | disabled on request |
| `com.mediatek.androidbox` | Miracast | disabled on request |
| `com.tianci.localmedia` | USB media player | disabled on request |
| `com.smartdevice.duplebluetooth` | two BT audio outputs | disabled on request |
| `com.Smartdevice.emanual` | e-manual | disabled on request |
| `com.mediatek.OledFuctionService`, `com.mstar.android.tv.disclaimercustomization`, `com.mediatek.android.boot.appsplashscreen` | small helpers | kept |

## Out of the box

Animations all `0.0`. YouTube, YouTube Music, Netflix, Prime Video were disabled before us (owner/previous setup).
`wifi_scan_always_enabled=0`.

## Home takeover

Candidates: `launcherx` (2), `tungsten.setupwraith` RecoveryActivity (1), Settings FallbackHome (-1000).
Disabling both Google packages + `build-install.sh` -> `mFocusedApp` = our launcher. Live TV app for config:
`com.smartdevice.livetv`.

## Picture

Menu: `am start -n com.smartdevice.livetv/com.product.osd.AndroidTvSettings.PictureActivity`, then OK on
"Изображение". It is a side panel that closes after a few seconds idle: send keys in one `adb shell` line
(`input keyevent 23; for i in $(seq 1 N); do input keyevent 20; done`) and dump with uiautomator right after.
Do not tap: a tap went through the panel into Settings behind it. Before opening, `am force-stop com.android.tv.settings`
so no stale Settings page sits under the panel.

Keys live in `settings global` and the TV app reacts to writes at once (menu shows the new value).

- **Any change in a preset switches the TV to "Пользователь" (`picture_mode=0`)**, which has its own advanced
  values (DNR, MPEG NR, adaptive luma, local contrast all "Средний"=2). After writing, set those to 0 too.
- Values are **per source**: apps vs Live TV tuner have separate sets. Calibrate each with that source open.
  `setting.py` keeps one original per key, so save the tuner's originals in a separate file.
- `picture_mode`: 9 Фильм, 0 Пользователь (others in the list: Яркий, Стандарт, Спорт, Энергосбережение - not mapped).
- `picture_color_temperature`: 3 Тёплый, 2 not Warm (tuner default). `picture_gamma`: 2 Средняя (Тёмная/Средняя/Яркая).
- `tv_picture_advance_video_dnr`, `..._mpeg_nr`, `tv_picture_video_adaptive_luma_control`,
  `tv_picture_video_local_contrast_control`: 0 Выкл, 1 Низкий, 2 Средний.
- `tv_picture_video_di_film_mode`: 0 Выкл, 1 Авто. `tv_picture_video_color_space`: 0 Авто.
- `picture_backlight` = "Яркость экрана" (light output), 0-100. `picture_sharpness` 0 = neutral. No MEMC item in the menu.
- Out of the box, apps: Фильм, Тёплый, Средняя, backlight 100, sharpness 8, saturation 40, all processing off.
  Tuner: Пользователь, colour temp 2, backlight 100, sharpness 10, saturation 50, processing Средний.

## Sound

Same panel: from PictureActivity press DOWN, OK on "Звук". Items: Звуковой режим, Баланс, НЧ, ВЧ, Студийный звук DTS
(submenu: Включить / Объемный / TruVolume), Автогромкость (switch), Динамик, Аудио тип, Для слабовидящих,
Усиление диалогов, Звук и изображение.

- The menu does **not** write `settings global`: `snapshot.py diff` was empty after toggling Автогромкость and
  moving НЧ. The `sound_*` keys there (`sound_auto_volume_leveling=1`, `sound_bass=50`) did not match the menu.
  Set sound through the menu with keys and verify by uiautomator (`checked` on the switch, slider value).
  `restore.py` cannot undo these: list them in the TV's README.
- Автогромкость and Усиление диалогов are global; НЧ and the sound mode are per source (apps: Фильм, tuner: Стандарт).
- Out of the box: Фильм, НЧ/ВЧ 50, DTS off, Автогромкость off, Усиление диалогов on.
- Slider step = 1 per RIGHT press. A switch toggles with OK (safe, no radio list).

## Power / slow start

Out of the box every power-off from the remote is a full shutdown (`persist.sys.boot.reason.history` =
`shutdown,userrequested`, `boot_count` ~1600): ~1 min to Home. Google's Energy modes ("Оптимизированное") alone did
not change it. The fix is the vendor switch **Настройка ТВ > Настройка > "Быстрая загрузка"** (PictureActivity panel,
DOWN x3, OK, then item 2): with it on the owner reported a fast start. It writes no `settings`/`getprop` key we could
see (`persist.sys.fast_standby` stayed 0). The same menu holds "Сброс настроек": screenshot the focus before OK.

## DPI bypass

ByeByeDPI 1.8.2 armeabi-v7a. Best strategy on the owner's ISP: `-d1 -s4 -d8 -s1+s -d5+s -s10+s -d20+s -a1` (27/32).
VPN up ~460 ms after `only_start`. 1080p video played through `tun0` (~5 Mbit/s steady); the first video took ~20 s
to start.
