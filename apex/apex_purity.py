#!/usr/bin/env python3
"""Fail if Apex modified or deleted a stock file that is not on the touched-stock-files allowlist.

usage: apex/apex_purity.py [base_commit]   (default: the `stock` vendor branch)
New files are allowed; every touched stock file must also appear in PATCHES.md.
"""
import subprocess, sys
from pathlib import Path

def _default_base():
    for ref in ("origin/stock", "stock"):
        if subprocess.run(["git", "rev-parse", "--verify", "-q", ref], capture_output=True).returncode == 0:
            return ref
    sys.exit("No stock reference branch found (expected origin/stock or stock).")
BASE = sys.argv[1] if len(sys.argv) > 1 else _default_base()
root = Path(__file__).resolve().parent.parent
allowed = {l.strip() for l in (root / "apex/touched-stock-files.txt").read_text().splitlines()
           if l.strip() and not l.startswith("#")}
ledger = (root / "PATCHES.md").read_text()
out = subprocess.check_output(["git", "-C", str(root), "diff", "--name-status", "--no-renames", BASE, "HEAD"], text=True)
bad, touched = [], set()
for line in out.splitlines():
    status, path = line.split("\t", 1)
    if status == "A":
        continue
    touched.add(path)
    if path not in allowed:
        bad.append(f"{status} {path}")
missing = sorted(p for p in allowed if p not in ledger)
unused = sorted(allowed - touched)
if bad or missing:
    print("Stock purity check failed.")
    for b in bad: print("  not allowlisted:", b)
    for m in missing: print("  not explained in PATCHES.md:", m)
    sys.exit(1)
print(f"Stock purity OK: {len(touched)} stock files touched, all allowlisted and documented.")
if unused: print("  allowlisted but unchanged:", ", ".join(unused))
