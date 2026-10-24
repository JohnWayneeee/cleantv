# Picture

Goal: an accurate picture (D65 white, correct gamma, no artificial processing), adjusted to the room and the
owner's taste where taste is legitimate (motion, brightness). Ask Q-V1..Q-V5 first.

## 1. Find where the TV keeps picture settings

```sh
python <skill>/scripts/setting.py list global picture_ tv_picture_ key_picture_
```

- **Keys found** (MediaTek/Homwee, many TCL/Hisense/CHiQ builds): the TV app mirrors its menu into `settings global`.
  Values are vendor enums. Map them before writing (step 2).
- **No keys** (Sony, Philips, some Xiaomi): settings live in a vendor provider. Drive the menu (step 4).

Open the picture menu for screenshots: on Google TV Settings > Display & Sound > Picture; on MediaTek the TV app's
menu. Find activities without starting anything:

```sh
python <skill>/scripts/tv.py 'cmd package query-activities --brief -a android.intent.action.MAIN' | grep -iE "pictur|display|sound"
python <skill>/scripts/tv.py 'dumpsys package com.android.tv.settings | grep -iE "pictur|display" | head'
```

## 2. Map a vendor key (snapshot diff)

```sh
python <skill>/scripts/snapshot.py save pic0
# change ONE item in the menu: by hand, or tv.py --keys RIGHT on the focused item
python <skill>/scripts/snapshot.py diff pic0
```

The diff names the key and the new value. Repeat for each enum you need (for picture mode: step through every
mode and write down the value per name). Record the mapping in `references/devices/<board>.md`.

## 3. Apply and verify

```sh
python <skill>/scripts/setting.py put global <key> <value> [<key> <value> ...]
```

Then reopen the menu and take a screenshot: the item must show the new value, and the picture must change.
If `settings` changed but the menu or the picture did not, the TV app does not observe that key: restore it
(`restore.py` restores all; or `setting.py put` the old value) and use step 4 for that item.

Read `picture_mode` again after every write. Some TV apps (Skyworth) move to the User preset as soon as a value in
another preset changes, and User brings its own noise reduction / dynamic contrast values: diff all picture keys
against `backup/settings-global.txt` and switch those off too. Do not open radio lists (mode, gamma) with OK when
mapping: read the summary line instead, a stray key there changes the preset.

Picture settings are usually **per input** (apps/internal, each HDMI, tuner) and sometimes per picture mode and
per SDR/HDR. Apply for each input the owner uses. HDMI and Live TV give black screenshots: verify those by the
key's value and by asking the owner to look.

## 4. Drive the menu (universal fallback)

Open the menu, then one key per step with a screenshot after each:

```sh
python <skill>/scripts/tv.py --screenshot shots/pic-01.png DOWN
```

Read the screenshot, decide the next key. Slow, but works on every TV. Ask the owner to keep the remote down.

## 5. Targets

### SDR

| Item | Target | Why |
|---|---|---|
| Picture mode | Filmmaker Mode > Movie / Cinema / Calibrated / Expert | closest to D65 and the source |
| Colour temperature | Warm (Warm2 / Expert on some brands) | D65; Standard/Cool is blue |
| Gamma | 2.2 bright or mixed room, 2.4 (BT.1886) dark room | |
| Backlight (light output) | dark 25-45%, dim 50-70%, bright 80-100% | the brightness control the owner should use |
| Brightness (black level), contrast | vendor default for the mode | moving them clips shadows/highlights without test patterns |
| Sharpness | the neutral value: 0 on MediaTek/Samsung/LG, 50 on Sony | above neutral adds halos |
| Colour / saturation, tint | default (50, 0) | |
| Dynamic contrast, adaptive luma, dynamic range expansion, black/blue stretch, flesh tone, auto/live colour, super resolution/AISR, edge/precision detail | off | artificial processing |
| Noise reduction, MPEG NR | off; low only for poor SD broadcast | smears detail |
| Light sensor, energy saving, auto backlight, low blue light | off (Q-V5 = accuracy) | changes brightness/colour behind the owner's back |
| Local dimming | medium (dark room) / high (bright, HDR); off if blooming bothers the owner | only on sets with zones |
| Motion smoothing (MEMC/MJC) | Q-V3: off films, low sport/mixed | soap-opera effect |
| Film mode / cadence detection | on/auto | removes 3:2 judder, not an effect |
| Aspect / overscan | Just Scan / Screen Fit / Pixel-to-pixel / "Full" | overscan crops and rescales |
| Colour space | Auto | |
| HDMI RGB range | Auto | |

### HDR (own preset on most TVs)

HDR Movie / Dolby Vision Dark (dark room) or Bright (daylight); backlight 100%; dynamic tone mapping on;
local dimming high; motion as for SDR.

### Per HDMI device (Q-V4)

| Device | Settings |
|---|---|
| Console | Game mode on (or ALLM on), MEMC off, HDMI "enhanced"/EDID 2.0+ on that port |
| PC | PC mode / Game mode, enhanced HDMI for 4:4:4, overscan off |
| Set-top box / old devices | standard HDMI/EDID 1.4 if the enhanced mode gives no picture |
| Soundbar/receiver | see sound.md |

### Room presets (Q-V1, Q-V2)

- Dark + films: Filmmaker/Movie, Warm, gamma 2.4, backlight ~35%, MEMC off.
- Dim/mixed + films and series: Movie, Warm, gamma 2.2, backlight ~60%, MEMC off.
- Bright daylight: Movie or Standard with Warm, gamma 2.2, backlight 90-100%, local dimming high.
- Sport: Movie or Sport with Warm, MEMC low/medium.
- If the owner wants different day and night looks, set two picture modes (e.g. Movie for night, User for day) and
  tell them which one is which.

Tell the owner the new picture may look warmer (less blue) and less "punchy" at first; that is the accurate look.
