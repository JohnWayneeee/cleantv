#!/usr/bin/env python3
"""Stop apps from running in the background (they still work when opened).

  appops.py restrict com.vk.tv ru.kinopoisk.tv
  appops.py allow com.vk.tv

Original modes are kept in backup/appops-changes.tsv for restore.py.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tvlib import BACKUP, ensure_backup_dir, sh  # noqa: E402

LOG = os.path.join(BACKUP, "appops-changes.tsv")
OP = "RUN_ANY_IN_BACKGROUND"


def mode(pkg):
    out = sh(f"cmd appops get {pkg} {OP}")
    return out.split(":", 1)[1].split(";")[0].strip() if ":" in out else "default"


def main(a):
    if len(a) < 2 or a[0] not in ("restrict", "allow"):
        sys.exit(__doc__)
    ensure_backup_dir()
    seen = {l.split("\t")[0] for l in open(LOG, encoding="utf-8")} if os.path.exists(LOG) else set()
    with open(LOG, "a", encoding="utf-8") as log:
        for pkg in a[1:]:
            old = mode(pkg)
            if pkg not in seen:
                log.write(f"{pkg}\t{OP}\t{old}\n")
            sh(f"cmd appops set {pkg} {OP} {'ignore' if a[0] == 'restrict' else 'allow'}")
            print(f"{pkg}: {old} -> {mode(pkg)}")


if __name__ == "__main__":
    main(sys.argv[1:])
