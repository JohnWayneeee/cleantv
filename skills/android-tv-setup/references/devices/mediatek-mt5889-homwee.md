# MediaTek MT5889, Homwee firmware, Google TV (Android 11)

Seen on: CHiQ (Changhong) `AI PONT`, device `ventura`, build `RTM8.240401.221`, 2.8 GB RAM, armeabi-v7a, 1080p.
ODM: Homwee (`com.homwee.*`, code under `com.changhong.*`). `ro.adb.secure=1`.

## Result

Debloat, launcher takeover and speed batches all worked; nothing was re-enabled by the firmware after reboot.
Sideloading works (`pm install -r`). MemAvailable 1.42 GB -> 1.69 GB after the speed batch.

## Packages

| Package | What it is | Verdict |
|---|---|---|
| `com.homwee.store` | store with background `StoreService` | disabled |
| `com.homwee.help` | e-manual | disabled |
| `com.homwee.synckey` | Firebase push + boot receiver + install referrer, ~30 MB | disabled, no side effects seen |
| `com.homwee.mmp.screenshare` | AirPlay receiver, hpplay/Lebo SDK | disabled on request |
| `com.homwee.multiscreenshare` | Miracast | disabled on request |
| `com.homwee.mmp.imageplayer` | USB photo/music player | kept |
| `com.homwee.mmp.fileexplorer` | USB file browser | was already disabled |
| `com.homwee.upgrade`, `com.homwee.dmt.upgrade` | firmware OTA | kept on request |
| `com.homwee.dynamicbacklight`, `com.homwee.lightsensorservice` | picture helpers, tiny | kept |
| `com.homwee.factory`, `com.mediatek.tv.factorykey` | factory | never touch |
| `com.mediatek.wspkdemo` | wireless speaker store demo | disabled |
| `com.mediatek.autopair` | remote pairing, `BIND_TV_INPUT`, volume/global buttons | keep |
| `com.mediatek.speakerservice` | speaker service | keep |
| `com.mediatek.wwtv.setupwizard` | MTK first-run wizard; also the BT voice remote pairing screen opened by the mic button, ~250 MB once started | keep; force-stop if it lingers |
| `com.gdch.gameoptimiser` | game mode, global key receiver | keep |
| `com.mediatek.wwtv.tvcenter` | TV app: inputs, tuner, picture/sound menus, handles the Input key | keep |
| `android.autoinstalls.config.google.gtvpai` | Play auto-install of promoted apps | disabled |
| `com.google.android.apps.nbu.smartconnect.tv` | Google data saver / Smart Connect VPN | disabled |

## Home takeover (Google TV)

HOME candidates: `launcherx` (priority 2), `setupwraith/.RecoveryActivity` (1), `FallbackHome` (-1000).
`set-home-activity` alone: Success, nothing changes. `launcherx` disabled alone: RecoveryActivity black-screen loop
("LauncherX HomeActivity not found on device. Skipping enable."). Disabling the RecoveryActivity component:
`SecurityException`. Working path: Wi-Fi owned by `setupwraith` -> owner forgot and re-joined the network
(owner became `com.android.tv.settings`, then `android.uid.system` after reboot; DHCP gave a new IP) ->
disable `setupwraith` + `launcherx` -> `set-home-activity` -> `mFocusedApp` = our launcher, survives reboot.

## ADB quirks

- `input tap` is ignored in third-party apps: drive them with D-pad keys and read focus from `uiautomator dump`.
  Dumps lag ~1 s behind a key press.
- `adb exec-out screencap -p` prefixes the PNG with vendor log lines (`Init wrapper sys mutex successful...`).
  Use `tv.py --screenshot`.

## Screensaver and power

- Idle: `system screen_off_timeout=600000` starts the dream `dreamx/.service.Backdrop`. With `dreamx` disabled the
  owner still saw an annoying screensaver/screen-off. Fix: `secure screensaver_enabled 0`,
  `secure screensaver_activate_on_sleep 0`, `system screen_off_timeout 2147483647` (the Energy menu then shows
  «При бездействии: Никогда»). Vendor auto-off «Выключение ТВ при бездействии 4 ч.» = `secure attentive_timeout`, kept.
- Settings > Система > Питание и энергопотребление > «Состояние при включении» is a slice from `launcherx`; with
  `launcherx` disabled the Settings app crashes (`IllegalStateException ... PowerAndEnergyFragment`). Harmless, avoid it.
- No quick-start toggle in Settings; `ro.vendor.mstar.str.enable=user_mode` (suspend-to-RAM).

## ByeByeDPI

Owner had 1.7.5 with a working command-line strategy and no app filter (all traffic through the VPN).
Upgraded to 1.8.2 with `pm install -r` (settings kept), set filter = whitelist YouTube (`appId 10056`),
autostart and auto-connect off. Launcher starts it for YouTube only; VPN `Uid: 10056 OwnerUid: 10098` verified.

## Remote (IR, vendor 3697 product 0001, `/vendor/usr/keylayout/Vendor_3697_Product_0001.kl` not readable)

| Scancode name | Key | After takeover |
|---|---|---|
| `KEY_FIND` (136) | `KEYCODE_TV_INPUT` | works: tvcenter opens its input panel |
| `KEY_BRL_DOT2` | gear / dashboard | dead: SystemUI `TvNotificationPanel` cannot resolve `TOGGLE_NOTIFICATION_HANDLER_PANEL` (system apps only) |
| `KEY_JOURNAL` | microphone | opens `com.mediatek.wwtv.setupwizard/.BluetoothActivityVoice` (pair a BT voice remote) |
| `KEY_F` | unknown app/shortcut key | dead |

## Inputs (TvInputManager)

`com.mediatek.tvinput/.hdmi.HDMIInputService/HW5..HW8` = HDMI 1-4, `.composite.CompositeInputService/HW2` = AV,
`.tuner.TunerInputService/HW0,HW1` = tuner. `HDMI400004` = CEC child device (has a parent id, skip).
Passthrough `ACTION_VIEW content://android.media.tv/passthrough/<id>` resolves to
`com.mediatek.wwtv.tvcenter/.nav.TurnkeyUiMainActivity` and switches the source.

## Settings keys (`settings global`)

The TV app mirrors its menus here. Writing them has not been verified on this board yet: verify per picture.md.
Values seen on the original state:

- Picture: `picture_mode=9`, `picture_backlight=15`, `picture_brightness=50`, `picture_contrast=99`,
  `picture_saturation=50`, `picture_hue=-2`, `picture_sharpness=0`, `picture_color_temperature=3`,
  `picture_gamma=2`, `picture_film_maker_mode=0`, `picture_film_mode=0`, `picture_format=7`,
  `picture_auto_backlight=0`, `picture_global_dimming=0`, `picture_blue_light=0`, `picture_list_hdr=1`,
  `picture_pixel_shift=1`, white balance `picture_red_gain/green_gain/blue_gain`, `picture_white_balance11_*`.
- Advanced: `tv_picture_video_noise_reduction`, `tv_picture_advance_video_mpeg_nr`, `tv_picture_advance_video_dnr`,
  `tv_picture_advance_video_mjc_effect` (MEMC), `tv_picture_video_di_film_mode`, `tv_picture_video_local_dimming=3`,
  `tv_picture_video_adaptive_luma_control`, `tv_picture_video_dynamic_range_expansion`, `tv_picture_video_blue_stretch`,
  `tv_picture_video_flesh_tone`, `tv_picture_video_auto_color`, `tv_picture_video_live_tone`,
  `tv_picture_video_precision_detail`, `tv_picture_video_aisr`, `tv_picture_video_4k_uhd_upscaling`,
  `tv_picture_video_light_sense`, `tv_picture_video_low_bluelight`, `tv_picture_video_game_mode`,
  `tv_picture_video_allm`, `tv_picture_video_pc_mode`, `tv_picture_video_hdmi_rgb_range`,
  `tv_picture_video_color_space=2`, `tv_picture_color_tune_*` (CMS), `tv_input_hdmi_edid_version=1`.
- Per-mode: `key_picture_mode_<mode>_auto_backlight` for energy_saving, game, movie, others, sport, standard, user, vivid.
- Sound: `sound_style=4`, `sound_advanced_sound_mode=10`, `sound_digital_output=7`, `sound_spdif_type=7`,
  `sound_earc=1`, `sound_speaker_status=1`, `sound_auto_volume_leveling=1`, `sound_auto_volume_control=0`,
  `sound_dialog_enhancement`, `sound_advanced_dialogue_enhancer`, `sound_advanced_surround_virtualizer`,
  `sound_advanced_volume_leveler`, `sound_advanced_dolby_ap`, `sound_advanced_dolby_atmos`, `sound_dts_*`,
  `sound_equalizer` + `sound_equalizer_detail_<band>hz`, `sound_bass`, `sound_treble`, `sound_bass_enhancement`,
  `sound_speaker_delay`, `sound_spdif_delay`, `sound_downmixmode=11`, `sound_surround`.
- CEC/ARC: `hdmi_control_enabled=1`, `hdmi_system_audio_control_enabled=1`, `hdmi_arc_control_enabled=1`.
- Animations were `window 0`, `transition 0.5`, `animator 0` out of the box.

Picture menu: `am start -n com.android.tv.settings/.partnercustomizer.picture.PictureActivity`
(Google's Display & Sound only has game mode). The TV app observes these keys; menu changes write them.

| Key | Values (menu, ru) |
|---|---|
| `picture_mode` | 9 = Кино (Movie) |
| `picture_color_temperature` | 3 = Тёплый |
| `picture_gamma` | 1 = Тёмная (≈2.4), 2 = Средний, 3 = Светлая (assumed) |
| `tv_picture_video_di_film_mode` | 0 = Выкл, 1 = Авто |
| `tv_picture_video_color_space` | 0 = Автоматически, 1 = Выкл, 2 = Вкл (native gamut) |
| `tv_picture_video_local_dimming` | 0 = Выкл, 1 = Низкий, 2 = Средний, 3 = Высокий |
| `tv_picture_advance_video_mjc_effect` | 0 = Отключено |

Radio lists open with focus on the first item, not on the checked one: check focus before OK.
Applied on CHiQ AI PONT (dark room, films): gamma 1, di_film_mode 1, color_space 0, local_dimming 2.

Sound menu: `am start -n com.android.tv.settings/.device.sound.SoundActivity`.

| Key | Values (menu, ru) |
|---|---|
| `sound_style` | 4 = Фильмы (vendor EQ +25/-24/-24/+41/+25), 0 = Пользовательский |
| `sound_advanced_dolby_ap` | «Обработка Dolby Audio»; 1 greys out the vendor style/EQ/surround and flattens the EQ |
| `sound_advanced_sound_mode` | Dolby mode: 2 = Кино (fixed, hides the toggles), 10 = Пользователь |
| `sound_advanced_volume_leveler` | Dolby «Авто уровень громкости» (User mode only) |
| `sound_advanced_surround_virtualizer` | Dolby Virtual Surround; also sets Netflix `nrdp_audio_platform_capabilities` atmos virtualization |
| `sound_advanced_dolby_atmos` | Dolby Atmos |
| `sound_advanced_dialogue_enhancer` (+ `sound_dialogue_enhancer`) | «Усиление звука речи»: 0 Выкл, 1 Низкий, 2 Средний, 3 Высокий |

Applied on CHiQ AI PONT (TV speakers; dialogue, volume jumps, night): Dolby on, User, leveler on,
virtualizer on, Atmos on, dialogue 2.
