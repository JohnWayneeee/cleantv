# Finish

## 1. Reboot check

```sh
python <skill>/scripts/tv.py reboot
# wait, reconnect (adb kill-server if "offline"), accept the prompt if asked
python <skill>/scripts/tv.py 'getprop sys.boot_completed; dumpsys activity activities | grep -m1 mFocusedApp'
python <skill>/scripts/tv.py 'pm list packages -d'          # compare as a set with backup/disabled-by-us.txt
python <skill>/scripts/tv.py 'ping -c1 -W2 8.8.8.8 | tail -1'
python <skill>/scripts/tv.py --screenshot shots/90-after-reboot.png
```

Re-read a few `settings-changes.tsv` keys: firmware can reset picture/sound keys at boot.

## 2. The TV's README (`tvs/<model>/README.md`)

Write it for the owner, short:

- Device: brand, model, board, Android, RAM, IP.
- What was disabled, per batch, with one line per package (from `debloat/*.txt`), and what was kept on purpose.
- Performance, picture and sound changes with old -> new values (from `backup/*-changes.tsv`).
- Known limits: dead remote buttons and the workaround, anything the owner declined.
- How to re-enable one app: `adb shell pm enable <pkg>`.
- Full rollback: `python <skill>/scripts/restore.py` from the work directory, then `adb uninstall com.cleantv.launcher`.
- How to change the dock and the wallpaper (launcher.md section 6).
- Rebuild/install: `bash <skill>/scripts/build-install.sh`.

## 3. Close network ADB

1. The owner switches off **USB debugging** and **Network/ADB debugging** in Developer options.
2. **Revoke USB debugging authorisations** in the same menu.
3. Reboot the TV.
4. `python <skill>/scripts/adb-closed.py` must print `CLOSED` (connection refused).

`UNKNOWN` (timeout) proves nothing: wake the TV, check the IP. On macOS do not trust `nc`: Local Network privacy
gives "No route to host" there too; only "Connection refused" proves the port is closed.

If the TV reached the network through a USB Ethernet adapter, the port may only return with debugging off:
unplug and replug the adapter, and have the owner confirm the TV is on Ethernet again, not Wi-Fi.

Port 5555 is plaintext; on firmware with `ro.adb.secure=0` anyone on the LAN gets a shell without a prompt.

## 4. Device notes

Add what you learned to `references/devices/<board-or-brand>.md`: package meanings, vendor settings keys and
enum values, Home takeover path, remote buttons, quirks. Next time the same family goes faster.
