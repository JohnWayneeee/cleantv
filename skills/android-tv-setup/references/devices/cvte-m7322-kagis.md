# CVTE / MediaTek m7322, Android TV 11 (KAGIS)

Seen on: KAGIS `KAGIS_4K_ATV` (manufacturer Expressluck), device `yeongdeungpo`, build `RTO3.250807.001`, 1.35 GB RAM,
armeabi-v7a, 1080p UI. Classic Android TV (stock home `com.google.android.tvlauncher`), not Google TV.
ODM packages `com.cvte.*`, `com.at.*`, `com.cltv.*`, MediaTek `com.mediatek.*`. `ro.adb.secure=1`.
`adb connect IP:5555` worked directly (prompt on screen).

## Result

Debloat, privacy, speed and Home takeover worked. MemAvailable 243 MB -> 543 MB. The Wi-Fi owner was
`android.uid.system`, so nothing had to re-join.

## Packages

| Package | What it is | Verdict |
|---|---|---|
| `tv.anoki.acr.controller` | Anoki ACR 3.2.5 (`/system/priv-app/AnokiController`): boot receiver, REAL_GET_TASKS, AD_ID, INTERNET, `FastSupportProvider` | disabled; see quirk below |
| `com.cltv.fast` | FAST channels (CLTV) | was already disabled |
| `fusion.android.tv.demo` | store demo | disabled |
| `com.cvte.tv.cvte_help` | help / e-manual | disabled |
| `com.mediatek.dmp.mediaplayer` | USB media player ("Digitalmedia Player") | disabled on request |
| `com.mediatek.partner.netflix` | Netflix partner helper | disabled with Netflix |
| `com.cvte.tv.at` | `/system/app/tv-fac-at`, system uid, boot receiver, `TopFloatService` | factory-adjacent: never touch |
| `com.at.factory.factorymanager` | factory manager; its BootReceiver opens `MainActivity` at every boot ("AddAccessoryActivity"), finishes in ~2 s | stock behaviour: never touch |
| `com.cvte.fac.menu`, `com.cvte.caixun.factory.at`, `com.mediatek.testpq` | factory / PQ test | never touch |
| `android.autoinstalls.config.caixun` | Play auto-install config (DummyReceiver) | kept |
| `com.airoha.android.bluetoothlegatt`, `com.airoha.android.btd` | Airoha BT remote, OTA tool ("1600 OTA" tile) | keep |
| `teatro_tv_browser` / `hbbtv_content_shell` | native HbbTV browser in `/mnt/vendor/3rd/teatro`, root, started by init | not a package; leave |

## Quirks

- **Persistent ACR survives disable-user.** Anoki has `PERSISTENT`; on this build AMS starts it at boot even with
  `enabled=3` ("Start proc ... for added application"). `force-stop` and `kill` cannot stop it, and `pm hide` is refused
  (shell lacks MANAGE_USERS). After boot it held no TCP sockets and logged nothing. Rely on DNS blocking.
- **App-op uid modes reset at reboot**: `appops set --uid com.android.vending RECORD_AUDIO ignore` was gone after a
  reboot (package-mode CAMERA ignore and RUN_ANY_IN_BACKGROUND survived). `privacy.py deny` could not set
  RECORD_AUDIO for the Play Store at package level ("No operations"). `pm revoke ... RECORD_AUDIO` worked and persisted.
- zsh: the `T="python3 ..."; $T` pattern does not word-split. Use a shell function.

## Home takeover

HOME candidates: `tvlauncher` (priority 2), Settings `FallbackHome` (-1000); no setup-wizard RecoveryActivity.
Disable `tvlauncher` (+ `tvrecommendations`), then `build-install.sh`, and `mFocusedApp` = our launcher. The first Home
press right after the disable opened the TV app once; after that it was stable across two reboots.
Boot order: FallbackHome -> our launcher -> factorymanager (2 s) -> our launcher.
Live TV package: `com.mediatek.wwtv.tvcenter`. Inputs: HDMI = `com.mediatek.tvinput/.hdmi.HDMIInputService/HW4..HW6`,
AV `.composite.../HW1`, tuner `HW0`, `HW8`.

## Picture

Menu: Settings > Geräteeinstellungen > Bild (main list: down x4 to Geräteeinstellungen, then down x8 to Bild). Google's
"Display & Ton" only has game mode. No PictureActivity component; navigate with keys and read with `uiautomator`
(screenshots are blank while Live TV/HDMI is underneath). The TV app's MENU key gives a quick panel (Quelle, Bildmodus,
Anzeigemodus, Lautsprecher, Energie, Erweiterte Optionen).

**`settings put` applies live but does not persist**: after a reboot every picture key was back. Values must be set
through the menu (sliders: 1 step per LEFT/RIGHT; radio lists open with focus on the checked item, except
Farbtemperatur, which opens on the first item). Menu-set values survived a reboot.

- `picture_mode`: 0 Nutzer, 9 Film, 11 Energiesparen (others in the list: Standard, Lebhaft, Sport, Spiel). Leaving
  Energiesparen shows an EU "higher energy use" dialog, focus on Abbrechen; UP to OK.
- Changing any value in a preset switches to Nutzer, which loads its own values.
- `picture_color_temperature`: 2 Standard, 3 Warm (list: Nutzer, Kühl, Standard, Warm).
- `picture_gamma`: 1 Dunkel, 2 Mittel, 3 Hell.
- `tv_picture_advance_video_dnr`, `..._mpeg_nr`, `tv_picture_video_adaptive_luma_control`: 0 Aus, 1 Niedrig,
  2 Mittel, 3 Stark (DNR also has Automatisch).
- `tv_picture_video_di_film_mode`: 1 Automatisch. No MEMC item in the menu (`..._mjc_effect` key exists, no UI).
- Values looked shared across apps and HDMI (the HDMI menu showed the same Nutzer values).
- Out of the box: Energiesparen, backlight 63, Standard colour temperature, gamma Mittel, sharpness 10, NR Mittel.

## Sound

Settings > Geräteeinstellungen > Ton: Klangstil, Balance, Surround-Sound, Equalizerdetails, Lautsprecher,
Digitaler Ausgang (PCM), SPDIF-Verzögerung, Automatische Lautstärkeregelung. Not changed (owner had no complaints).
