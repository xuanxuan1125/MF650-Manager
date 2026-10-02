"""Inventory this FOTA's USB scripts as text; never executes compositions.

This is a format-specific static inventory, not a general shell interpreter.
Functions are a union across USB configurations and conditional DIAG variants.
"""
import argparse
import csv
import hashlib
from pathlib import Path
import re


def inventory(directory, output):
    rows = []
    for path in sorted(directory.iterdir()):
        if not re.fullmatch(r"[0-9A-F]{4}", path.name):
            continue
        content = path.read_bytes()
        text = content.decode("utf-8")
        description = re.search(r"(?m)^# DESCRIPTION:\s*(.*)$", text)
        configfs = re.search(r"(?ms)^run_configfs\(\) \{\n(.*?)^\}", text)
        body = configfs[1] if configfs else ""
        references = sorted(set(re.findall(r"functions/([^\s;]+)", body)))
        branch = re.search(r"(?ms)^\s*\*[^)\n]*sdxprairie[^)\n]*\)\s*\n(.*?)^\s*;;", text)
        support = "UNKNOWN"
        note = "No explicit sdxprairie branch; fallback requires manual review"
        functions = []
        if branch and "run_configfs" in branch[1]:
            support = "YES"
            functions = references
            # 9025 passes the transport explicitly in this target's branch.
            if 'xport="gsi"' in branch[1]:
                functions = [f.replace("$2", "gsi") for f in functions]
            note = "Explicit sdxprairie configfs branch; union of configurations/conditions"
        elif path.name == "901D":
            support = "YES"
            functions = references
            note = "Generic branch: configfs if legacy enable node absent; both branches provide DIAG+ADB"
        else:
            fallback = re.search(r"(?ms)^\s*\*\s*\)\s*\n(.*?)^\s*;;", text)
            if fallback and re.search(r"not supported|not be supported|supported only", fallback[1], re.I):
                support = "NO"
                note = "Target falls through to unsupported message"
        known = support == "YES"
        rows.append({"PID": path.name, "Description": description[1].strip() if description else "",
                     "sdxprairie_support": support, "configfs_function_references": " | ".join(references),
                     "sdxprairie_functions": " | ".join(functions) if known else "N/A" if support == "NO" else "UNKNOWN",
                     "ADB": "YES" if known and "ffs.adb" in functions else "NO" if known else "N/A" if support == "NO" else "UNKNOWN",
                     "RNDIS": "YES" if known and any("rndis" in f for f in functions) else "NO" if known else "N/A" if support == "NO" else "UNKNOWN",
                     "ECM": "YES" if known and any("ecm" in f for f in functions) else "NO" if known else "N/A" if support == "NO" else "UNKNOWN",
                     "DIAG": "YES" if known and any("diag" in f for f in functions) else "NO" if known else "N/A" if support == "NO" else "UNKNOWN",
                     "adbd_kill_in_configfs": "YES" if "pkill adbd" in body else "NO",
                     "Note": note, "script_sha256": hashlib.sha256(content).hexdigest()})
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)
    print(f"{len(rows)} PID scripts inventoried")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    inventory(args.directory, args.output)
