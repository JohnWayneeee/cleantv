#!/usr/bin/env python3
"""Find which settings key a menu item writes: snapshot, change one item on the TV, diff.

  snapshot.py save before
  (change ONE item in the TV's picture/sound menu, by hand or with tv.py --keys)
  snapshot.py diff before          # compares with the TV right now
  snapshot.py diff before after    # compares two saved snapshots

Snapshots live in backup/snapshots/. Keys that change on their own (timestamps, counters) are filtered.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tvlib import BACKUP, sh  # noqa: E402

DIR = os.path.join(BACKUP, "snapshots")
NOISE = re.compile(r"(time|timestamp|_ms$|count|last_|boot|uptime|elapsed|ntp|_seq|nonce|token|sync_)", re.I)


def current():
    data = {}
    for ns in ("global", "secure", "system"):
        for line in sh(f"settings list {ns}").splitlines():
            if "=" in line:
                k, v = line.split("=", 1)
                data[f"{ns} {k}"] = v
    return data


def load(name):
    data = {}
    for line in open(os.path.join(DIR, name + ".txt"), encoding="utf-8"):
        k, _, v = line.rstrip("\n").partition("=")
        data[k] = v
    return data


def main(a):
    if len(a) >= 2 and a[0] == "save":
        os.makedirs(DIR, exist_ok=True)
        with open(os.path.join(DIR, a[1] + ".txt"), "w", encoding="utf-8", newline="\n") as f:
            for k, v in sorted(current().items()):
                f.write(f"{k}={v}\n")
        print(f"saved {a[1]}")
    elif len(a) >= 2 and a[0] == "diff":
        old = load(a[1])
        new = load(a[2]) if len(a) > 2 else current()
        for k in sorted(set(old) | set(new)):
            if old.get(k) != new.get(k) and not NOISE.search(k):
                print(f"{k}: {old.get(k, '(none)')} -> {new.get(k, '(none)')}")
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
