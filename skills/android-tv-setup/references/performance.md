# Performance

Measure, change, measure again. Report numbers to the owner (MemAvailable before/after, top processes).

## 1. Measure

```sh
python <skill>/scripts/tv.py 'grep -E "MemTotal|MemAvailable" /proc/meminfo'
python <skill>/scripts/tv.py 'dumpsys meminfo | sed -n "/Total PSS by process/,/Total PSS by OOM/p" | head -30'
python <skill>/scripts/tv.py 'top -b -n 1 -m 12 -s 9'
python <skill>/scripts/setting.py get global window_animation_scale transition_animation_scale animator_duration_scale
```

Typical findings: a setup wizard or pairing screen left in memory (200+ MB on MediaTek after the mic button),
the assistant (~100 MB) on a TV whose remote has no microphone, vendor background services with Firebase push,
streaming apps kept alive in the background. Idle CPU should be > 80%: if not, find the process in `top`.

## 2. Options (ask Q-P1..Q-P5)

All go through `setting.py` / `appops.py` / `disable.py`, so `restore.py` undoes them.

| Change | Command | Notes |
|---|---|---|
| Animations 0.5x | `setting.py put global window_animation_scale 0.5 transition_animation_scale 0.5 animator_duration_scale 0.5` | `0` = off; the launcher highlight then jumps instead of springing |
| No background Wi-Fi/BLE scanning | `setting.py put global wifi_scan_always_enabled 0 ble_scan_always_enabled 0` | safe on TVs |
| Freeze cached apps (Android 11+) | `setting.py put global cached_apps_freezer enabled` | reboot to apply; cached apps use no CPU |
| Background limit per app | `appops.py restrict <pkg> ...` | app still opens normally; no background work |
| Free a leftover process | `tv.py 'am force-stop <pkg>'` | one-off, nothing to undo |
| More debloat | `disable.py debloat/batchN-speed.txt` | from the meminfo list, owner's choice |
| Compile apps ahead of time | `tv.py 'cmd package bg-dexopt-job'` | 5-15 min, TV on; faster cold starts |
| Trim caches | `tv.py 'pm trim-caches 999G'` | frees storage, apps rebuild caches |

### Low-RAM devices (2 GB or less)

If MemAvailable stays low after debloat and the owner sees playback stop or the player reload its buffer:

- **Background process limit** (Q-P5). The owner sets it on the TV: Developer Options → Background process
  limit → "At most 3 processes" or "At most 4 processes". It is not a `settings` key, so `restore.py` does not
  track it; a reboot resets it to "Standard limit". Side effect: apps the owner switches away from start cold,
  and a VPN/DPI-bypass app or music with the screen off may be killed. Skip it if they use ByeByeDPI.
- **Player app.** A web-wrapper streaming app uses more memory than a native player. Compare on this TV:
  `dumpsys meminfo <pkg>` while the video plays, and suggest the lighter one (e.g. SmartTube for YouTube,
  VLC or Nova Video Player for local files and streams).

Do not restrict background work for: the launcher, the TV app, input services, the keyboard, Play Services,
VPN/DPI-bypass apps (they must stay running), music apps the owner listens to with the screen off.

## 3. Verify

Repeat the measurements. Open two or three of the owner's apps and return Home: no black frames, no reloads of
the launcher. Report MemAvailable before and after.
