#!/usr/bin/env python3

import os

with open("mem.hex") as f:
    for (i,l) in enumerate(f.readlines()):
        print(f"mem({i}.U) := \"h{l.strip()}\".U")

