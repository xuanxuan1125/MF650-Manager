"""Unpack a reference ZIP locally, preserving ZIP metadata in a hashed manifest.

Firmware programs are never executed. ZIP symlink entries are saved as target
text, never followed or created as host symlinks. Nothing connects to a device.
"""
import argparse
import csv
import hashlib
import json
from pathlib import Path, PurePosixPath
import stat
import zipfile


def unpack(archive, destination):
    root = destination.resolve()
    extracted = root / "extracted"
    for name in ("extracted", "adb", "usb", "web", "binaries", "diff", "reports", "hashes"):
        (root / name).mkdir(parents=True, exist_ok=True)
    rows = []
    with zipfile.ZipFile(archive) as source:
        seen = set()
        names = set()
        local_names = {}
        for entry in source.infolist():
            path = PurePosixPath(entry.filename)
            if path.is_absolute() or ".." in path.parts or "\\" in entry.filename or ":" in entry.filename:
                raise ValueError(f"unsafe ZIP path: {entry.filename}")
            key = entry.filename.rstrip("/").casefold()
            if entry.filename in names or (key in seen and entry.is_dir()):
                raise ValueError(f"duplicate ZIP path or colliding directory: {entry.filename}")
            local_names[entry.filename] = entry.filename
            if key in seen:
                # NTFS normally folds case; keep both Linux library variants.
                suffix = hashlib.sha256(entry.filename.encode()).hexdigest()[:8]
                local_names[entry.filename] += ".__zipcase__" + suffix
            seen.add(key)
            names.add(entry.filename)
        for entry in source.infolist():
            local_name = local_names[entry.filename]
            output = extracted.joinpath(*PurePosixPath(local_name).parts)
            if not output.resolve().is_relative_to(extracted.resolve()):
                raise ValueError("extraction target escapes destination")
            mode = entry.external_attr >> 16
            kind = "directory" if entry.is_dir() else "symlink-text" if stat.S_ISLNK(mode) else "file"
            digest = ""
            target = ""
            if entry.is_dir():
                output.mkdir(parents=True, exist_ok=True)
            else:
                content = source.read(entry)  # ZIP CRC is checked while reading.
                output.parent.mkdir(parents=True, exist_ok=True)
                output.write_bytes(content)
                digest = hashlib.sha256(content).hexdigest()
                if kind == "symlink-text":
                    target = content.decode("utf-8", errors="replace")
            rows.append({"path": entry.filename, "local_path": local_name, "type": kind, "size": entry.file_size,
                         "compressed_size": entry.compress_size, "zip_mode": oct(mode),
                         "zip_timestamp": "%04d-%02d-%02d %02d:%02d:%02d" % entry.date_time,
                         "sha256": digest, "symlink_target": target})
    with (root / "hashes/FILE_MANIFEST.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)
    with archive.open("rb") as stream:
        archive_hash = hashlib.file_digest(stream, "sha256").hexdigest()
    sums = [f"{archive_hash}  {archive.name}"]
    sums += [f"{r['sha256']}  extracted/{r['local_path']}" for r in rows if r["sha256"]]
    (root / "hashes/SHA256SUMS.txt").write_text("\n".join(sums) + "\n", encoding="utf-8")
    summary = {"archive": archive.name, "archive_bytes": archive.stat().st_size,
               "archive_sha256": archive_hash, "entries": len(rows),
               "uncompressed_bytes": sum(r["size"] for r in rows),
               "case_aliases": sum(r["path"] != r["local_path"] for r in rows),
               "types": {k: sum(r["type"] == k for r in rows) for k in ("directory", "file", "symlink-text")}}
    (root / "hashes/ARCHIVE_SUMMARY.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    unpack(args.archive, args.destination)
