"""MF650 read-only legacy ADB SYNC: STAT, LIST and RECV only.

Uses direct ADB CNXN -> OPEN sync:. No shell, authentication bypass, SEND,
upload DATA, upload DONE, directory recursion, or device configuration.
Protocol: https://android.googlesource.com/platform/system/adb/+/fda46ba4632b59d6054b67c6c7e79898839f785b/SYNC.TXT
"""
import argparse
import hashlib
import json
import socket
import stat
import struct
import sys
from pathlib import Path

TARGET = ("192.168.100.1", 7628)
MAX_BYTES = 4 * 1024 * 1024
READ_REQUESTS = {"stat": b"STAT", "list": b"LIST", "recv": b"RECV"}


class SyncError(Exception):
    pass


class ReadOnlySync:
    def __init__(self):
        self.socket = socket.create_connection(TARGET, timeout=3)
        self.trace = []
        self.buffer = bytearray()
        self.local_id = 1
        self.remote_id = None
        self.peer_version = 0x01000000

    def _exact(self, length):
        data = bytearray()
        while len(data) < length:
            block = self.socket.recv(length - len(data))
            if not block:
                self.trace.append({"direction": "receive", "event": "TCP EOF", "partial_hex": data.hex()})
                raise SyncError("connection closed before expected response")
            data.extend(block)
        return bytes(data)

    def _send(self, command, arg0=0, arg1=0, payload=b""):
        if command not in (b"CNXN", b"OPEN", b"WRTE", b"OKAY"):
            raise SyncError("outbound transport command rejected")
        if command == b"OPEN" and payload != b"sync:\0":
            raise SyncError("only sync: may be opened")
        if command == b"WRTE" and payload[:4] not in READ_REQUESTS.values():
            raise SyncError("only read-only SYNC requests may be transmitted")
        value = int.from_bytes(command, "little")
        packet = struct.pack("<6I", value, arg0, arg1, len(payload), sum(payload), value ^ 0xffffffff) + payload
        self.trace.append({"direction": "send", "command": command.decode(), "arg0": arg0,
                           "arg1": arg1, "payload_hex": payload.hex(), "wire_hex": packet.hex()})
        self.socket.sendall(packet)

    def _packet(self):
        header = self._exact(24)
        command, arg0, arg1, length, checksum, magic = struct.unpack("<6I", header)
        if magic != command ^ 0xffffffff or length > 65536:
            raise SyncError("invalid ADB header or excessive response")
        payload = self._exact(length)
        tag = command.to_bytes(4, "little")
        skip_checksum = (arg0 if tag == b"CNXN" else self.peer_version) >= 0x01000001
        if not skip_checksum and checksum != sum(payload):
            raise SyncError("ADB payload checksum mismatch")
        self.trace.append({"direction": "receive", "command": tag.decode("ascii", errors="replace"),
                           "arg0": arg0, "arg1": arg1, "payload_hex": payload.hex(),
                           "wire_hex": (header + payload).hex()})
        return tag, arg0, arg1, payload

    def open(self):
        self._send(b"CNXN", 0x01000000, 4096, b"host::\0")
        tag, version, _, _ = self._packet()
        if tag != b"CNXN":
            raise SyncError(f"expected CNXN; received {tag!r}; no authentication attempted")
        self.peer_version = version
        self._send(b"OPEN", self.local_id, 0, b"sync:\0")
        tag, remote, local, _ = self._packet()
        if tag != b"OKAY" or local != self.local_id or not remote:
            raise SyncError(f"OPEN sync: rejected: {tag.decode(errors='replace')} arg0={remote} arg1={local}")
        self.remote_id = remote

    def _sync_exact(self, length):
        while len(self.buffer) < length:
            tag, remote, local, payload = self._packet()
            if tag == b"CLSE":
                raise SyncError("SYNC stream closed before complete response")
            if tag not in (b"OKAY", b"WRTE") or remote != self.remote_id or local != self.local_id:
                raise SyncError("unexpected transport stream response")
            if tag == b"WRTE":
                self.buffer.extend(payload)
                self._send(b"OKAY", self.local_id, self.remote_id)
        value = bytes(self.buffer[:length])
        del self.buffer[:length]
        return value

    def request(self, operation, path):
        if operation not in READ_REQUESTS:
            raise SyncError("unsupported operation; reads only")
        encoded = path.encode("utf-8")
        if not path.startswith("/") or b"\0" in encoded or len(encoded) > 1024:
            raise SyncError("an absolute path without NUL, at most 1024 bytes, is required")
        self._send(b"WRTE", self.local_id, self.remote_id,
                   READ_REQUESTS[operation] + struct.pack("<I", len(encoded)) + encoded)

    def _tag(self):
        tag = self._sync_exact(4)
        if tag == b"FAIL":
            length = struct.unpack("<I", self._sync_exact(4))[0]
            if length > 4096:
                raise SyncError("excessive SYNC error length")
            raise SyncError(self._sync_exact(length).decode(errors="replace"))
        return tag

    def read_stat(self):
        if self._tag() != b"STAT":
            raise SyncError("expected STAT response")
        mode, size, mtime = struct.unpack("<3I", self._sync_exact(12))
        return {"mode": oct(mode), "size": size, "mtime": mtime}

    def read_list(self):
        entries = []
        while True:
            tag = self._tag()
            if tag == b"DONE":
                self._sync_exact(16)  # Legacy LIST DONE has the DENT-sized tail.
                return entries
            if tag != b"DENT" or len(entries) >= 4096:
                raise SyncError("unexpected or excessive directory response")
            mode, size, mtime, length = struct.unpack("<4I", self._sync_exact(16))
            if length > 1024:
                raise SyncError("excessive directory name length")
            name = self._sync_exact(length).decode("utf-8", errors="replace")
            entries.append({"name": name, "mode": oct(mode), "size": size, "mtime": mtime,
                            "directory": stat.S_ISDIR(mode)})

    def read_file(self):
        data = bytearray()
        while True:
            tag = self._tag()
            length = struct.unpack("<I", self._sync_exact(4))[0]
            if tag == b"DONE":
                return bytes(data)
            if tag != b"DATA" or length > 65536 or len(data) + length > MAX_BYTES:
                raise SyncError("unexpected or excessive file response")
            data.extend(self._sync_exact(length))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=READ_REQUESTS)
    parser.add_argument("path")
    parser.add_argument("output", nargs="?", help="local output file; required for recv")
    parser.add_argument("--trace", type=Path, help="local raw trace; keep private")
    args = parser.parse_args()
    if args.operation == "recv" and not args.output:
        parser.error("recv requires a local output file")
    client = None
    try:
        client = ReadOnlySync()
        client.open()
        client.request(args.operation, args.path)
        if args.operation == "stat":
            result = client.read_stat()
        elif args.operation == "list":
            result = client.read_list()
        else:
            data = client.read_file()
            output = Path(args.output)
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(data)  # Local PC only, after a complete RECV/DONE response.
            result = {"bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
        print(json.dumps({"operation": args.operation, "path": args.path, "result": result}, ensure_ascii=False, indent=2))
        return 0
    except (OSError, SyncError) as exc:
        print(json.dumps({"operation": args.operation, "path": args.path, "error": str(exc)}, ensure_ascii=False), file=sys.stderr)
        return 1
    finally:
        if client:
            client.socket.close()
            if args.trace:
                args.trace.parent.mkdir(parents=True, exist_ok=True)
                args.trace.write_text(json.dumps(client.trace, indent=2), encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
