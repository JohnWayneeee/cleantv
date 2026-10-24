"""Shared helpers: find adb, find the TV, run shell commands.

Scripts run from the TV's work directory (tvs/<model>/). The TV address comes from
TV_HOST[/TV_PORT] or from a file ./.tv holding "IP:PORT".
"""
import os
import shutil
import subprocess
import sys

WORK = os.getcwd()
BACKUP = os.path.join(WORK, "backup")


def find_adb():
    if os.environ.get("ADB"):
        return os.environ["ADB"]
    exe = "adb.exe" if os.name == "nt" else "adb"
    found = shutil.which("adb")
    if found:
        return found
    candidates = []
    for sdk in (os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")):
        if sdk:
            candidates.append(os.path.join(sdk, "platform-tools", exe))
    candidates += [
        os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", exe),
        os.path.expanduser("~/Library/Android/sdk/platform-tools/adb"),
        os.path.expanduser("~/Android/Sdk/platform-tools/adb"),
    ]
    for c in candidates:
        if os.path.isfile(c):
            return c
    sys.exit("adb not found: install Android platform-tools or set ADB=/path/to/adb")


def find_serial():
    if os.environ.get("TV_HOST"):
        return f"{os.environ['TV_HOST']}:{os.environ.get('TV_PORT', '5555')}"
    if os.path.isfile(".tv"):
        s = open(".tv").read().split()[0]
        return s if ":" in s else s + ":5555"
    sys.exit("TV address unknown: write 'IP:5555' to ./.tv or set TV_HOST")


ADB = find_adb()
_serial = None
_connected = False


def serial():
    global _serial
    if _serial is None:
        _serial = find_serial()
    return _serial


def adb(*args, check=False):
    global _connected
    if not _connected:
        subprocess.run([ADB, "connect", serial()], capture_output=True)
        _connected = True
    r = subprocess.run([ADB, "-s", serial(), *args], capture_output=True)
    if check and r.returncode != 0:
        sys.exit(f"adb {' '.join(args)} failed: {r.stderr.decode(errors='replace').strip()}")
    return r


def sh(cmd):
    """Run one shell command on the TV, return stdout+stderr as text."""
    r = adb("shell", cmd)
    return (r.stdout + r.stderr).decode("utf-8", errors="replace").replace("\r\n", "\n").strip()


def packages(flag=""):
    out = sh(f"pm list packages {flag}")
    return sorted(l.split(":", 1)[1].strip() for l in out.splitlines() if l.startswith("package:"))


def ensure_backup_dir():
    os.makedirs(BACKUP, exist_ok=True)
    return BACKUP
