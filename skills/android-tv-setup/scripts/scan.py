#!/usr/bin/env python3
"""Find TVs on the local /24: ADB (5555) and Cast/Android TV Remote ports (8008, 8009, 6466).

  scan.py                 # subnet of the default route
  scan.py 192.168.0.0     # another /24

"refused" on 5555 means a live host with ADB off. No answer at all from anything, including the
router, usually means a VPN captures the LAN (see references/connect.md).
"""
import concurrent.futures as cf
import errno
import socket
import sys

PORTS = (5555, 8008, 8009, 6466)
REFUSED = {errno.ECONNREFUSED, 10061}


def local_prefixes():
    """Private /24s of this machine, home-style 192.168.* first. A VPN may own the default route."""
    ips = {ai[4][0] for ai in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET)}
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("192.0.2.1", 9))  # no packet is sent; adds the default-route interface
        ips.add(s.getsockname()[0])
    finally:
        s.close()
    private = [ip for ip in ips if ip.startswith(("192.168.", "10.")) or
               (ip.startswith("172.") and 16 <= int(ip.split(".")[1]) <= 31)]
    order = {"192": 0, "10": 1, "172": 2}
    return sorted({ip.rsplit(".", 1)[0] for ip in private}, key=lambda p: (order[p.split(".")[0]], p))


def probe(arg):
    host, port = arg
    s = socket.socket()
    s.settimeout(1.2)
    try:
        r = s.connect_ex((host, port))
    finally:
        s.close()
    return host, port, "open" if r == 0 else "refused" if r in REFUSED else None


def scan(prefix):
    found = {}
    with cf.ThreadPoolExecutor(256) as ex:
        for host, port, state in ex.map(probe, [(f"{prefix}.{i}", p) for i in range(1, 255) for p in PORTS]):
            if state:
                found.setdefault(host, []).append(f"{port}:{state}")
    if not found:
        print(f"{prefix}.0/24: no ADB/Cast ports open. If even the router does not answer ping, "
              "a VPN captures this subnet (references/connect.md)")
    for host in sorted(found, key=lambda h: int(h.rsplit(".", 1)[1])):
        tags = " ".join(sorted(found[host]))
        hint = "  <- ADB open" if "5555:open" in tags else "  <- TV? (cast/remote ports)" if "8009" in tags or "6466" in tags else ""
        print(f"{host:16} {tags}{hint}")


def main(a):
    for prefix in [x.rsplit(".", 1)[0] for x in a] or local_prefixes():
        scan(prefix)


if __name__ == "__main__":
    main(sys.argv[1:])
