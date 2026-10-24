#!/usr/bin/env python3
"""Change Android settings and remember the original values for restore.py.

  setting.py put global window_animation_scale 0.5 [key value ...]
  setting.py get global picture_mode picture_backlight
  setting.py list global picture_        # keys starting with a prefix

The first original value of each key is kept in backup/settings-changes.tsv
(namespace, key, original value; "null" means the key did not exist).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tvlib import BACKUP, ensure_backup_dir, sh  # noqa: E402

LOG = os.path.join(BACKUP, "settings-changes.tsv")
NAMESPACES = ("global", "secure", "system")


def recorded():
    if not os.path.exists(LOG):
        return set()
    return {tuple(l.rstrip("\n").split("\t")[:2]) for l in open(LOG, encoding="utf-8") if l.strip()}


def put(ns, pairs):
    ensure_backup_dir()
    seen = recorded()
    with open(LOG, "a", encoding="utf-8") as log:
        for key, value in pairs:
            old = sh(f"settings get {ns} {key}")
            if (ns, key) not in seen:
                log.write(f"{ns}\t{key}\t{old}\n")
                seen.add((ns, key))
            sh(f"settings put {ns} {key} {value}")
            new = sh(f"settings get {ns} {key}")
            print(f"{ns} {key}: {old} -> {new}" + ("" if new == value else "  (MISMATCH)"))


def main(a):
    if len(a) < 3 or a[1] not in NAMESPACES:
        sys.exit(__doc__)
    cmd, ns, rest = a[0], a[1], a[2:]
    if cmd == "put" and len(rest) % 2 == 0:
        put(ns, list(zip(rest[::2], rest[1::2])))
    elif cmd == "get":
        for k in rest:
            print(f"{k}={sh(f'settings get {ns} {k}')}")
    elif cmd == "list":
        for line in sh(f"settings list {ns}").splitlines():
            if any(line.startswith(p) for p in rest):
                print(line)
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
