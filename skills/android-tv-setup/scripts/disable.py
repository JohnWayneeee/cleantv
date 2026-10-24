#!/usr/bin/env python3
"""Disable the packages listed in a file (one per line, # comments), one pm call each.

  disable.py debloat/batch1.txt

Each package that ends up disabled is appended to backup/disabled-by-us.txt, which restore.py reads.
The script refuses lists with core packages, factory packages, or the owner of a saved Wi-Fi network.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tvlib import BACKUP, ensure_backup_dir, packages, sh  # noqa: E402

LOG = os.path.join(BACKUP, "disabled-by-us.txt")

# Disabling any of these can leave the TV unbootable or without picture, input or remote.
FORBIDDEN = re.compile(
    r"^(android"
    r"|com\.android\.(systemui|shell|phone|settings|tv\.settings|bluetooth|inputdevices|vending)"
    r"|com\.android\.providers\.(?!calendar|contacts|userdictionary|blockednumber).*"
    r"|com\.(google\.)?android\.(permissioncontroller|packageinstaller|ext\.services|ext\.shared|modulemetadata)"
    r"|com\.google\.android\.overlay\.modules.*"
    r"|com\.google\.android\.(gms|gsf|inputmethod\.latin|tv\.remote\.service)"
    r"|com\.mediatek\.(tvinput|tv\.service|wwtv\.tvcenter|autopair)"
    r")$")
# Never touch these, not even to look: some boards flip a factory flag when they start.
FACTORY = re.compile(r"(factory|\.fac\.|facmenu|hkcfac|\.at\.factory)", re.I)


def main(path):
    pkgs = [l.split("#")[0].strip() for l in open(path, encoding="utf-8")]
    pkgs = [p for p in pkgs if p]
    bad = [p for p in pkgs if FORBIDDEN.match(p)]
    if bad:
        sys.exit(f"refusing: core/load-bearing packages in list: {bad}")
    fac = [p for p in pkgs if FACTORY.search(p)]
    if fac:
        sys.exit(f"refusing: factory packages in list: {fac}")
    owners = set(re.findall(r"cname=(\S+)", sh("dumpsys wifi")))
    if owners & set(pkgs):
        sys.exit(f"refusing: a saved Wi-Fi network belongs to {owners & set(pkgs)}; Android deletes it when the "
                 "package is disabled. Forget and re-join the network in Settings first.")

    ensure_backup_dir()
    done = set(open(LOG).read().split()) if os.path.exists(LOG) else set()
    refused = 0
    for p in pkgs:
        out = sh(f"pm disable-user --user 0 {p}")
        refused += "SecurityException" in out or "Exception" in out
        print(f"{p}: {out.splitlines()[0] if out else '(no output)'}")
    disabled = set(packages("-d"))
    ok = [p for p in pkgs if p in disabled]
    print(f"\nverified disabled: {len(ok)}/{len(pkgs)}")
    for p in pkgs:
        if p not in disabled:
            print(f"  NOT disabled: {p}")
    if pkgs and not ok and refused:
        print("every package was refused: this firmware blocks pm disable-user. Stop debloating; "
              "fall back to the TV's privacy toggles and DNS blocking.")
    with open(LOG, "a") as f:
        for p in ok:
            if p not in done:
                f.write(p + "\n")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
