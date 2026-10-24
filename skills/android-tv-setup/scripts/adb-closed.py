#!/usr/bin/env python3
"""Check that network ADB is closed on the TV.

  adb-closed.py [IP]      # default: the address in ./.tv

Only "connection refused" proves the port is closed. A timeout means the host is unreachable
(TV in standby, wrong IP, VPN/firewall), which proves nothing.
"""
import errno
import os
import socket
import sys

if len(sys.argv) > 1:
    host = sys.argv[1]
elif os.path.isfile(".tv"):
    host = open(".tv").read().split()[0].split(":")[0]
else:
    sys.exit(__doc__)

s = socket.socket()
s.settimeout(3)
r = s.connect_ex((host, 5555))
s.close()
if r == 0:
    print(f"OPEN: {host}:5555 accepts connections - switch off USB/network debugging")
    sys.exit(1)
if r in (errno.ECONNREFUSED, 10061):
    print(f"CLOSED: {host}:5555 refused the connection")
    sys.exit(0)
print(f"UNKNOWN: {host}:5555 unreachable (code {r}); wake the TV and check the IP")
sys.exit(2)
