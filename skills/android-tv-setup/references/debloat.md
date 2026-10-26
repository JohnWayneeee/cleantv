# Debloat

## 1. Sort the packages

From `backup/packages-system.txt`, `backup/packages-3rdparty.txt` and `backup/launchable.txt`, sort into:

1. Manufacturer telemetry and analytics.
2. Manufacturer content and ad services: stores, recommendation rows, content providers, PatchWall-like home rows.
3. Background services the owner likely does not need: duplicate cast/share receivers, cleaners, demo modes, print.
4. Preloaded apps.
5. Streaming apps.
6. Keep: everything below and anything load-bearing.

For every vendor package whose name does not say what it is, read it without starting it:

```sh
python tv.py 'dumpsys package <pkg> | grep -E "^\s+[0-9a-f]+ <pkg>/|versionName|sharedUser|permission\."'
```

Activities, services, receivers and permissions tell the story: `BIND_TV_INPUT` = an input/remote service (keep),
`c2dm`/Firebase + boot receiver = push/telemetry, `hpplay`/`lebo` = Chinese AirPlay SDK. Ask when still unclear.

## 2. Never disable

- Android core: `android`, `com.android.systemui`, `com.android.shell`, `com.android.phone`, `com.android.settings`,
  `com.android.tv.settings`, permission controller, package installer, ext services, ext shared, module metadata
  (each as `com.android.*` or `com.google.android.*`), `com.google.android.overlay.modules*`,
  `com.android.providers.*` except calendar, contacts, user dictionary and blocked numbers.
  An original Mi TV Stick never booted again after a list took six of these.
- What the TV needs: the TV app and input services (HDMI, tuner), the input/source menu, remote and Bluetooth
  pairing (`com.android.bluetooth`, vendor `autopair`), the on-screen keyboard, Play Store and Play Services,
  Android TV Remote service (phone remote).
- Factory packages: never disable, never launch (`*factory*`, `*.fac.*`, `hkcfac`, `com.at.factory.*`,
  `mediatek.factorymenu.ui`). On Haier/CVTE boards opening one flips a factory flag: Input key and CEC die and
  the next boot disables Play services, Netflix and Chromecast for good.
- The owner of the saved Wi-Fi network (`backup/wifi-owner.txt`), until the network is re-joined.
- On CVTE boards (`com.cvte.*`) leave the stock launcher enabled unless a device file says otherwise.

`disable.py` enforces the core list, the factory pattern and the Wi-Fi owner check.

## 3. Ask, then disable in batches

Ask Q-D1..Q-D4 (`questions.md`). Write each batch to `debloat/batchN-<topic>.txt`, one package per line with a
`# comment` saying what it is. Then:

```sh
python <skill>/scripts/disable.py debloat/batch1-vendor.txt
python <skill>/scripts/tv.py --screenshot shots/01-after-batch1.png HOME sleep:2
python <skill>/scripts/tv.py 'ping -c1 -W2 8.8.8.8 | tail -1'
```

Look at the screenshot before the next batch. A batch can take something load-bearing with it, and you can only
tell which batch if they are small.

Typical batches: (1) vendor telemetry/content/ads, (2) the owner's "don't use" choices, (3) ACR and viewing data
(privacy.md, mandatory), (4) launcher takeover (launcher.md), (5) speed (performance.md).

## 4. Reboot check

After the last batch: reboot, wait for `sys.boot_completed`, then compare:

```sh
python <skill>/scripts/tv.py 'pm list packages -d'
```

with `backup/disabled-by-us.txt` (compare as sets; `comm` on Windows trips over sort order and CRLF).
Some firmware re-enables packages at every boot from its own list (TCL Android 9 does this for its user center):
note those in the batch header instead of disabling them again. If the setup wizard or stock launcher came back
and Home is black, check `logcat | grep RecoveryActivity` and re-enable the stock launcher first.

## Firmware that refuses

Fire OS 5 and some others refuse `pm disable-user` with `SecurityException`, `pm uninstall --user 0` with
`DELETE_FAILED_INTERNAL_ERROR`, and `pm hide` reports success but changes nothing. Trust `pm list packages -d`,
not the exit code. If every package is refused, stop: offer the TV's own privacy toggles and DNS blocking.

## Optional: ad-blocking DNS (Q-L5)

Android 9+: `setting.py put global private_dns_specifier dns.adguard-dns.com` then
`setting.py put global private_dns_mode hostname`. Check that an app still plays. It does not block YouTube ads.
Undo: `restore.py` or `private_dns_mode opportunistic`.
