# Connect

Owner prepares the TV: Settings > Device Preferences > About > press OK on "Build" 7 times; back >
Developer options > USB debugging on, and Network/ADB debugging if listed. Keep the TV on.

## Find the TV

1. Create the work directory `tvs/<brand-model>/` and `cd` into it.
2. `python <skill>/scripts/scan.py`. A line marked `ADB open` is the TV. Write it to `.tv`: `echo 192.168.1.50:5555 > .tv`.
3. `adb connect IP:5555`. Tell the owner the "Allow debugging?" prompt is on screen now and to tick
   **"Always allow from this computer"**. Without the tick the prompt returns on every reconnect.
4. Poll `adb -s IP:5555 get-state` until `device` (up to ~90 s).

## When nothing answers

- **Nothing answers at all, not even the router** (`ping` the gateway): a VPN on the PC captures the LAN.
  `route print` (Windows) / `netstat -rn` shows the home subnet routed via the VPN interface.
  The fix is in the VPN client: exclude the home subnet (split tunnelling, "addresses not through VPN") or
  allow LAN access. Adding a more specific route does not help with clients that mirror every route into the
  tunnel (some WireGuard-based clients do). Kill switches also block the LAN.
- **Router answers, 5555 refused**: debugging is off, or the TV only offers Wireless debugging (Google TV 12+).
  Find `_adb-tls-connect._tcp` with `adb mdns services`, ask the owner for the pairing code and port
  (Developer options > Wireless debugging > Pair device), then `adb pair`, `adb connect`, `adb tcpip 5555`.
  Android switches Wireless debugging off whenever Wi-Fi drops.
- **5555 times out on a known TV IP**: the TV is in standby or the IP changed. Look the TV's MAC up in
  `arp -a` and rescan.
- **macOS, `adb` says "No route to host" while the port is open**: Local Network privacy blocks the binary.
  Use the Python `adb-shell` package in a venv. If its push hangs on large files, send base64 chunks through
  the shell and check the md5.
- **A sandbox blocks the local network while an ordinary terminal reaches the TV**: ask the owner to run a
  TCP relay in a terminal (`socat TCP-LISTEN:15555,fork TCP:<tv>:5555` or similar) and use `TV_HOST=127.0.0.1 TV_PORT=15555`.
- **TV in another home**: use the Tailscale address the owner gives; never scan their LAN.

## ADB states

- `unauthorized`: the prompt is on the TV screen. Ask the owner to accept with "Always allow".
- `offline`: `adb kill-server`, then connect again. Usually followed by the prompt.
- `adb install` hangs (Android 14): it waits for an on-screen confirmation. Push and `pm install -r` instead
  (`build-install.sh` does this).

## Windows notes

- Git Bash rewrites `/data/local/tmp/...` into `C:/Program Files/Git/data/...`. Set `MSYS_NO_PATHCONV=1`
  for adb commands with device paths, and convert local paths with `cygpath -m`. The Python scripts are not affected.
- `JAVA_HOME` often points to a JRE without `javac`. `build-install.sh` falls back to Android Studio's `jbr`.

## IP changes

Re-joining Wi-Fi (needed on Google TV, see launcher.md) can give the TV a new address. Find it by MAC in
`arp -a`, rescan, update `.tv`, reconnect, accept the prompt again. Suggest a DHCP reservation in the router.
