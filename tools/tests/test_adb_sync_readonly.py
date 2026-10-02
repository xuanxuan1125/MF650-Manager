"""Offline wire fixtures: read results, fragmented packets and write rejection."""
import struct
import unittest
from unittest.mock import patch
from tools import adb_sync_readonly as sync


def packet(tag, arg0=0, arg1=0, payload=b""):
    command = int.from_bytes(tag, "little")
    return struct.pack("<6I", command, arg0, arg1, len(payload), sum(payload), command ^ 0xffffffff) + payload


class SocketFixture:
    def __init__(self, response):
        self.response = bytearray(response)
        self.sent = []

    def recv(self, length):
        # Exercise packet and SYNC response fragmentation across reads.
        length = min(length, 3)
        value = bytes(self.response[:length])
        del self.response[:length]
        return value

    def sendall(self, data):
        self.sent.append(data)


class SyncTests(unittest.TestCase):
    def client(self, response, accepted=True):
        initial = packet(b"CNXN", 0x01000000, 4096, b"device::\0")
        initial += packet(b"OKAY", 23, 1) if accepted else packet(b"CLSE", 0, 1)
        fake = SocketFixture(initial + response)
        with patch.object(sync.socket, "create_connection", return_value=fake):
            client = sync.ReadOnlySync()
        return client, fake

    def response(self, value):
        # ACK the request, then split a single SYNC result over ADB WRTE frames.
        return (packet(b"OKAY", 23, 1) + packet(b"WRTE", 23, 1, value[:7]) +
                packet(b"WRTE", 23, 1, value[7:]))

    def assert_read_wire(self, fake, request):
        opened = [p[24:] for p in fake.sent if p[:4] == b"OPEN"]
        requests = [p[24:] for p in fake.sent if p[:4] == b"WRTE"]
        self.assertEqual(opened, [b"sync:\0"])
        self.assertEqual(len(requests), 1)
        self.assertEqual(requests[0][:4], request)
        self.assertFalse(any(p[:4] in (b"SEND", b"DATA", b"DONE") for p in requests))

    def test_stat_zero_size_virtual_file(self):
        client, fake = self.client(self.response(b"STAT" + struct.pack("<3I", 0o100444, 0, 123)))
        client.open()
        client.request("stat", "/proc/meminfo")
        self.assertEqual(client.read_stat(), {"mode": "0o100444", "size": 0, "mtime": 123})
        self.assert_read_wire(fake, b"STAT")

    def test_directory_list(self):
        value = b"DENT" + struct.pack("<4I", 0o100644, 42, 123, 10) + b"mf650.html" + b"DONE" + bytes(16)
        client, fake = self.client(self.response(value))
        client.open()
        client.request("list", "/www")
        self.assertEqual(client.read_list(), [{"name": "mf650.html", "mode": "0o100644", "size": 42, "mtime": 123, "directory": False}])
        self.assertEqual(client.buffer, b"")
        self.assert_read_wire(fake, b"LIST")

    def test_recv_and_acknowledge(self):
        contents = b"MemTotal: 227900 kB\n"
        value = b"DATA" + struct.pack("<I", len(contents)) + contents + b"DONE" + bytes(4)
        client, fake = self.client(self.response(value))
        client.open()
        client.request("recv", "/proc/meminfo")
        self.assertEqual(client.read_file(), contents)
        self.assertEqual(sum(p[:4] == b"OKAY" for p in fake.sent), 2)
        self.assert_read_wire(fake, b"RECV")

    def test_closed_open_sends_no_sync_request(self):
        client, fake = self.client(b"", accepted=False)
        with self.assertRaisesRegex(sync.SyncError, "OPEN sync: rejected: CLSE"):
            client.open()
        self.assertEqual([p[:4] for p in fake.sent], [b"CNXN", b"OPEN"])

    def test_reject_write_and_other_services_before_transmission(self):
        client, fake = self.client(b"")
        for tag in (b"SEND", b"DATA", b"DONE"):
            with self.assertRaises(sync.SyncError):
                client._send(b"WRTE", 1, 23, tag + bytes(4))
        for service in (b"shell:\0", b"root:\0", b"remount:\0"):
            with self.assertRaises(sync.SyncError):
                client._send(b"OPEN", 1, 0, service)
        self.assertEqual(fake.sent, [])

    def test_fail_and_oversized_data_do_not_return_file(self):
        for value in (b"FAIL" + struct.pack("<I", 6) + b"denied", b"DATA" + struct.pack("<I", 65537)):
            with self.subTest(value=value[:4]):
                client, _ = self.client(self.response(value))
                client.open()
                client.request("recv", "/proc/meminfo")
                with self.assertRaises(sync.SyncError):
                    client.read_file()


if __name__ == "__main__":
    unittest.main()
