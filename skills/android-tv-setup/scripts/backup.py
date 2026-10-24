#!/usr/bin/env python3
"""Save the TV's original state to ./backup before anything is changed.

  backup.py          # refuses to overwrite an existing backup
  backup.py --force

Prints a one-screen device summary at the end.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tvlib import BACKUP, ensure_backup_dir, packages, sh  # noqa: E402

HOME_Q = "-a android.intent.action.MAIN -c android.intent.category.HOME"


def save(name, text):
    with open(os.path.join(BACKUP, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(text.rstrip("\n") + "\n")


def main(argv):
    if os.path.exists(os.path.join(BACKUP, "getprop.txt")) and "--force" not in argv:
        sys.exit("backup/ already exists; it holds the original state. Use --force only on purpose.")
    ensure_backup_dir()
    save("packages-full.txt", sh("pm list packages -f -U"))
    save("packages-system.txt", "\n".join(packages("-s")))
    save("packages-3rdparty.txt", "\n".join(packages("-3")))
    save("packages-disabled-before.txt", "\n".join(packages("-d")))
    save("getprop.txt", sh("getprop"))
    for ns in ("system", "secure", "global"):
        save(f"settings-{ns}.txt", sh(f"settings list {ns}"))
    save("home-activities.txt", sh(f"cmd package query-activities --brief {HOME_Q}"))
    stock = sh(f"cmd package resolve-activity --brief {HOME_Q}").splitlines()[-1].strip()
    save("stock-home.txt", stock)
    save("launchable.txt", sh("cmd package query-activities --brief -a android.intent.action.MAIN "
                              "-c android.intent.category.LEANBACK_LAUNCHER"))
    owners = sorted(set(re.findall(r"cname=(\S+)", sh("dumpsys wifi"))))
    save("wifi-owner.txt", "\n".join(owners))
    save("display.txt", sh("wm size; wm density; echo forced=$(settings get secure display_density_forced)"))
    save("meminfo.txt", sh("cat /proc/meminfo | head -3; dumpsys meminfo | sed -n '/Total PSS by process/,/Total PSS by OOM/p' | head -40"))

    p = {}
    for line in open(os.path.join(BACKUP, "getprop.txt"), encoding="utf-8"):
        m = re.match(r"\[(.+?)\]: \[(.*)\]", line)
        if m:
            p[m.group(1)] = m.group(2)
    ram = sh("grep MemTotal /proc/meminfo")
    summary = [
        f"brand/model : {p.get('ro.product.brand')} {p.get('ro.product.model')} ({p.get('ro.product.manufacturer')})",
        f"device/board: {p.get('ro.product.device')} / {p.get('ro.board.platform') or p.get('ro.hardware')}",
        f"android     : {p.get('ro.build.version.release')} (SDK {p.get('ro.build.version.sdk')})",
        f"fingerprint : {p.get('ro.build.fingerprint')}",
        f"abi         : {p.get('ro.product.cpu.abi')}",
        f"ram         : {ram}",
        f"adb.secure  : {p.get('ro.adb.secure')}",
        f"stock home  : {stock}",
        f"wifi owner  : {', '.join(owners) or '-'}",
        f"google tv   : {'yes' if 'launcherx' in stock else 'no'}",
        f"vendors     : {', '.join(sorted({'.'.join(x.split('.')[:2]) for x in packages('-s') if x.count('.') >= 2 and not x.startswith(('com.android', 'com.google', 'android'))}))}",
    ]
    save("device.txt", "\n".join(summary))
    print("\n".join(summary))
    print(f"\nbackup saved to {BACKUP}")


if __name__ == "__main__":
    main(sys.argv[1:])
