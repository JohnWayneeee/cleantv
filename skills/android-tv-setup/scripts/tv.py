#!/usr/bin/env python3
"""Run a shell command or press keys on the TV, optionally capture a screenshot.

  tv.py 'getprop ro.build.version.sdk'           # shell command
  tv.py --screenshot shots/01.png DOWN OK        # keys, then screenshot
  tv.py --screenshot shots/now.png               # screenshot only
  tv.py --keys HOME sleep:2 RIGHT                # keys only

Key names are KEYCODE_* without the prefix; OK/UP/DOWN/LEFT/RIGHT are D-pad aliases.
sleep:N waits N seconds. A 0-byte screenshot means protected content (Live TV, HDMI, DRM).
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tvlib import adb, sh  # noqa: E402

KEYS = {"OK": "DPAD_CENTER", "UP": "DPAD_UP", "DOWN": "DPAD_DOWN", "LEFT": "DPAD_LEFT", "RIGHT": "DPAD_RIGHT"}


def press(keys):
    for k in keys:
        if k.startswith("sleep:"):
            time.sleep(float(k[6:]))
            continue
        k = KEYS.get(k, k)
        sh(f"input keyevent {k if k.isdigit() else 'KEYCODE_' + k}")
        time.sleep(0.6)


def screenshot(out):
    time.sleep(0.8)
    sh("screencap -p /data/local/tmp/shot.png")
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    adb("pull", "/data/local/tmp/shot.png", out)
    sh("rm /data/local/tmp/shot.png")
    if not os.path.exists(out) or os.path.getsize(out) == 0:
        print("empty screenshot: protected content (Live TV/HDMI/DRM). "
              "Check: dumpsys activity activities | grep topResumedActivity")
        return 1
    print(out)
    return 0


def main(argv):
    if argv[:1] == ["--screenshot"]:
        press(argv[2:])
        return screenshot(argv[1])
    if argv[:1] == ["--keys"]:
        press(argv[1:])
        return 0
    print(sh(" ".join(argv)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
