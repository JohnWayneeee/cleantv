#!/usr/bin/env python3
"""Undo this project's changes on the TV.

  restore.py                  # re-enable all packages, restore settings, give Home back to the stock launcher
  restore.py com.foo.bar ...  # re-enable just these packages

Reads backup/disabled-by-us.txt, settings-changes.tsv, appops-changes.tsv and stock-home.txt.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tvlib import BACKUP, sh  # noqa: E402


def read(name):
    path = os.path.join(BACKUP, name)
    return open(path, encoding="utf-8").read() if os.path.exists(path) else ""


def main(args):
    pkgs = args or read("disabled-by-us.txt").split()
    for p in pkgs:
        print(f"{p}: {sh(f'pm enable {p}')}")
    if args:
        return
    for line in read("settings-changes.tsv").splitlines():
        ns, key, old = (line.split("\t") + ["null"])[:3]
        cmd = f"settings delete {ns} {key}" if old == "null" else f"settings put {ns} {key} {old}"
        sh(cmd)
        print(f"{ns} {key} -> {old}")
    for line in read("appops-changes.tsv").splitlines():
        pkg, op, old = line.split("\t")
        sh(f"cmd appops set {pkg} {op} {old}")
        print(f"{pkg} {op} -> {old}")
    stock = read("stock-home.txt").strip()
    if stock:
        print(f"home -> {stock}: {sh(f'cmd package set-home-activity {stock}')}")
    print("done. Remove the launcher with: adb uninstall com.cleantv.launcher")


if __name__ == "__main__":
    main(sys.argv[1:])
