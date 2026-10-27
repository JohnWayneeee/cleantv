#!/usr/bin/env python3
"""Find who can watch and listen on the TV, and take the microphone and camera away from them.

  privacy.py scan                       # ACR/ad/analytics candidates, apps holding the mic or the camera
  privacy.py deny com.foo.bar ...       # block RECORD_AUDIO and CAMERA for these apps
  privacy.py allow com.foo.bar ...      # give them back
  privacy.py dns on|off|check           # ad and tracker blocking Private DNS (Android 9+), then prove it blocks

Original modes are kept in backup/appops-changes.tsv, so restore.py undoes them too.
The scan only lists; read each candidate with dumpsys before disabling it (debloat.md).
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from setting import put  # noqa: E402
from tvlib import BACKUP, ensure_backup_dir, sh  # noqa: E402

LOG = os.path.join(BACKUP, "appops-changes.tsv")
OPS = ("RECORD_AUDIO", "CAMERA")
DNS_HOST = "dns.adguard-dns.com"
# An ad host the filter must sink, and a normal host that must still resolve.
DNS_BLOCKED, DNS_OPEN = "doubleclick.net", "google.com"
PERMS = {"android.permission.RECORD_AUDIO": "mic", "android.permission.CAMERA": "camera"}

# Names seen on ACR (automatic content recognition), ad and viewing-data packages. A match is a lead, not a verdict.
SUSPECT = re.compile(
    r"(samba|alphonso|inscape|gracenote|nielsen|tvinteractive|interactivetv|viewing|contentrecogn"
    r"|(^|[._])acr([._]|$)|admob|adservice|adsdk|\.ads?(\.|$)|advert|tracking|analytic|telemetry"
    r"|datacollect|bigdata|userexperience|uxprogram|feedback\.collect)", re.I)
# Granting the mic or camera to these is how the TV works; never block them here.
SYSTEM = re.compile(
    r"^(android|com\.android\.(systemui|shell|phone|bluetooth|permissioncontroller)"
    r"|com\.google\.android\.(permissioncontroller|gms|gsf))$")


def granted():
    """{package: {"mic", "camera"}} for every package with the permission granted."""
    holders, pkg = {}, None
    for line in sh("dumpsys package packages").splitlines():
        m = re.match(r"\s*Package \[(.+?)\]", line)
        if m:
            pkg = m.group(1)
            continue
        m = re.match(r"\s*(android\.permission\.(RECORD_AUDIO|CAMERA)): granted=true", line)
        if m and pkg:
            holders.setdefault(pkg, set()).add(PERMS[m.group(1)])
    return holders


def mode(pkg, op):
    out = sh(f"cmd appops get {pkg} {op}")
    return out.split(":", 1)[1].split(";")[0].strip() if ":" in out else "default"


def scan():
    pkgs = sorted(l.split(":", 1)[1].strip() for l in sh("pm list packages").splitlines() if l.startswith("package:"))
    disabled = set(l.split(":", 1)[1].strip() for l in sh("pm list packages -d").splitlines() if ":" in l)
    print("ACR / ads / analytics candidates (read with dumpsys package before disabling):")
    for p in pkgs:
        if SUSPECT.search(p):
            print(f"  {p}{'  (disabled)' if p in disabled else ''}")
    print("\napps holding the microphone or the camera:")
    for p, what in sorted(granted().items()):
        if SYSTEM.match(p) or p in disabled:
            continue
        ops = ", ".join(f"{op}={mode(p, op)}" for op in OPS)
        print(f"  {p}: {'+'.join(sorted(what))}  [{ops}]")
    print("\nhardware: " + sh("pm list features | grep -iE 'microphone|camera' | tr '\\n' ' '"))


def set_ops(pkgs, value):
    ensure_backup_dir()
    seen = set()
    if os.path.exists(LOG):
        seen = {tuple(l.split("\t")[:2]) for l in open(LOG, encoding="utf-8") if l.strip()}
    with open(LOG, "a", encoding="utf-8") as log:
        for pkg in pkgs:
            if SYSTEM.match(pkg):
                print(f"{pkg}: skipped, the system needs it")
                continue
            for op in OPS:
                old = mode(pkg, op)
                if (pkg, op) not in seen:
                    log.write(f"{pkg}\t{op}\t{old}\n")
                sh(f"cmd appops set {pkg} {op} {value}")
                print(f"{pkg} {op}: {old} -> {mode(pkg, op)}")


def resolve(host):
    """The address the TV resolves host to, or "" when it does not resolve."""
    m = re.search(r"\(([0-9.]+)\)", sh(f"ping -c1 -W2 {host}"))
    return m.group(1) if m else ""


def dns(action):
    if action in ("on", "off"):
        if int(sh("getprop ro.build.version.sdk") or 0) < 28:
            sys.exit("Private DNS needs Android 9+; offer the router's DNS or nothing.")
        if action == "on":
            put("global", [("private_dns_specifier", DNS_HOST), ("private_dns_mode", "hostname")])
        else:
            put("global", [("private_dns_mode", "opportunistic")])
        sh("sleep 3")  # the resolver reconnects after a mode change
    blocked, open_ = resolve(DNS_BLOCKED), resolve(DNS_OPEN)
    print(f"{DNS_BLOCKED} -> {blocked or 'no address'}   {DNS_OPEN} -> {open_ or 'no address'}")
    if not open_:
        print("FAIL: normal sites do not resolve. Turn it off: privacy.py dns off")
    elif blocked in ("", "0.0.0.0"):
        print("OK: ads and trackers are blocked, the rest of the internet works")
    else:
        print("NOT BLOCKING: the TV does not use the filter (mode off, or a VPN app handles DNS)")


def main(a):
    if a[:1] == ["scan"]:
        scan()
    elif len(a) == 2 and a[0] == "dns" and a[1] in ("on", "off", "check"):
        dns(a[1])
    elif len(a) >= 2 and a[0] in ("deny", "allow"):
        set_ops(a[1:], "ignore" if a[0] == "deny" else "allow")
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
