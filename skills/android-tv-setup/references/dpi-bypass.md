# DPI bypass for YouTube (ByeByeDPI)

For owners whose ISP throttles or blocks YouTube with DPI (Russia and similar). ByeByeDPI
(`io.github.romanvht.byedpi`, https://github.com/romanvht/ByeByeDPI) runs ByeDPI locally and routes traffic through
it as a VPN. Nothing leaves the home network: no external server, no account.

## When to offer it

Ask Q-B1 during the Launcher phase when any of these is true: `getprop persist.sys.timezone` is a Russian zone
(`Europe/Moscow`, `Asia/Yekaterinburg`, ...), `getprop persist.sys.locale` is `ru-RU`, the owner speaks Russian,
or the owner says YouTube is slow. Do not offer it when the owner already runs a VPN on the TV: two VPNs cannot run
at once.

## 1. Install

Pick the APK by `getprop ro.product.cpu.abi` (`armeabi-v7a` on most TVs, even 64-bit boards) from the latest
release, put it in its own scratch directory, then:

```sh
adb push ByeByeDPI-<ver>-<abi>-release.apk /data/local/tmp/bbdpi.apk
python <skill>/scripts/tv.py 'pm install -r /data/local/tmp/bbdpi.apk'
python <skill>/scripts/tv.py 'rm /data/local/tmp/bbdpi.apk'
python <skill>/scripts/tv.py 'cmd appops set io.github.romanvht.byedpi ACTIVATE_VPN allow'
```

The `ACTIVATE_VPN` app op is the VPN consent the system dialog would ask for. Without it `ToggleActivity` silently
does nothing in VPN mode (`VpnService.prepare` returns an intent and the activity just finishes).

## 2. Settings (in the app, driven with `uiautomator dump` + `input tap`)

- Mode: **VPN** (default). The YouTube app has no proxy setting.
- DNS: **Google (8.8.8.8)**. Field report, not verified on our TVs: with Cloudflare (1.1.1.1, the default) the
  strategy test gave bad results; set Google DNS before the test. In 1.8.2 source the test runs in proxy mode and
  does not read `dns_ip`; the DNS server is used by VPN mode, so Google DNS matters for real playback either way.
- App filter: **Whitelist** -> select only `com.google.android.youtube.tv`. Other apps keep the direct route.
- Automation: autostart on boot **off**, auto-connect on app open **off**. The launcher starts and stops it.
- Do not restrict ByeByeDPI's background work (`appops.py`): it must keep running while YouTube plays.

## 3. Strategy test

Main screen > Подбор (Strategy test) > Start. About 60 strategies, 10-15 s each, so 10-15 minutes; the owner must
not touch the remote. By default it checks `youtube` and `googlevideo` domains. Apply the best one (most sites
passed) with the app's own "apply" action, or pass it to the toggle: `am start -n
io.github.romanvht.byedpi/.activities.ToggleActivity --es strategy '<args>' --ez only_update true`.

## 4. Start only with YouTube (launcher)

`ToggleActivity` is exported and takes `only_start` / `only_stop` / `only_update` booleans and a `strategy` string.
The launcher uses it (config.xml):

```xml
<string-array name="dpi_apps">
    <item>com.google.android.youtube.tv</item>
</string-array>
```

Opening a `dpi_apps` tile starts the bypass, waits `dpi_delay_ms` (700 ms) for the VPN, then opens the app.
Home coming back stops it. Limits: YouTube opened another way (a YouTube key on the remote, a deep link, recents)
does not start the bypass. Then either open it from the launcher, or leave ByeByeDPI always on: with the whitelist
only YouTube's traffic goes through it.

## 5. Verify

```sh
python <skill>/scripts/tv.py 'dumpsys connectivity | grep -c "VPN CONNECTED"'   # 1 while YouTube is open, 0 at Home
python <skill>/scripts/tv.py 'pidof io.github.romanvht.byedpi:native'          # empty at Home
adb shell dumpsys connectivity | grep "VPN CONNECTED" | grep -o "Uids: <[^>]*>"  # only YouTube's appId
```

Android 11 prints the VPN network differently: no `VPN CONNECTED` line, but `type: VPN[], state: CONNECTED` with
`Uid: <appId> OwnerUid: <byedpi appId>` in `nc{...}` for a single-app whitelist, or `Uids: <{0-10097, 10099-99999}>`
when there is no filter (everything goes through the VPN). `ifconfig tun0` works on every version.

ByeByeDPI ignores Settings while the service runs (the OK press does nothing): stop it with `only_stop` first.
Upgrading over an older version with `pm install -r` keeps its settings, strategy and the `ACTIVATE_VPN` app op
(1.7.5 -> 1.8.2 on CHiQ AI PONT).

Measured on a Skyworth m7332 (1.8 GB): the VPN is up ~460 ms after `only_start`; `Uids: <{10067-10067, 20067-20067}>`
matched YouTube's `appId=10067`. Toybox `grep` on the TV has no `\s` and fails on long pipes: grep on the PC.

Play a video at 1080p for a minute. Then press Home and check the VPN is gone.

## Undo

`pm uninstall io.github.romanvht.byedpi` (an app we installed, not a system package) and empty `dpi_apps`.
