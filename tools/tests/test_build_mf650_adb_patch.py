"""Offline regression checks; optional reference ZIP is never executed."""
import contextlib
import io
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import build_mf650_adb_patch as builder


class PatchTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fota = Path(os.environ.get('MF650_FOTA', Path(__file__).resolve().parents[3] / 'MF650_2.3_Fota.zip'))
        if not cls.fota.is_file():
            raise unittest.SkipTest('provide the original ZIP via MF650_FOTA')
        with zipfile.ZipFile(cls.fota) as z:
            cls.members = {n: z.read(n) for n in [*builder.MEMBER_HASHES, 'META-INF/com/android/metadata']}
        cls.original = cls.members[builder.ADBD_PATH]
        cls.patched, cls.traces = builder.make_patch(cls.original)
        cls.seed = builder.checksum_seed(cls.members['system/usr/lib/libshared.so'])

    def test_exact_diff_and_allow_paths(self):
        self.assertEqual(len(self.patched), 30380)
        self.assertEqual([i for i, (a, b) in enumerate(zip(self.original, self.patched)) if a != b], [0x5E1A, 0x5E1B])
        self.assertEqual(builder.sha256(self.patched), builder.PATCHED_SHA256)
        self.assertEqual([self.traces[x]['allow'] for x in ('9057', '9059', '90DB', '90570', 'FFFF', '')], [1, 1, 0, 0, 0, 0])
        self.assertIn('353a(r0=1)', self.traces['9057']['path'])
        self.assertIn('3528(clz)', self.traces['9059']['path'])
        self.assertEqual(self.original[:0x5E18], self.patched[:0x5E18])
        self.assertEqual(self.original[0x5E1C:], self.patched[0x5E1C:])

    def test_changed_adbd_rejected(self):
        for offset in (0, 0x351C, 0x5E18, len(self.original) - 1):
            with self.subTest(offset=offset):
                changed = bytearray(self.original)
                changed[offset] ^= 1
                with self.assertRaisesRegex(ValueError, 'SHA256 mismatch'):
                    builder.make_patch(bytes(changed))

    def test_instruction_fingerprint_independent_of_top_hash(self):
        changed = bytearray(self.original)
        changed[0x351C] ^= 1
        with patch.object(builder, 'ORIGINAL_SHA256', builder.sha256(changed)):
            with self.assertRaisesRegex(ValueError, 'instruction fingerprint'):
                builder.check_gate(bytes(changed), True)

    def test_duplicate_literal_independent_of_top_hash(self):
        changed = bytearray(self.original)
        changed[0x55A0:0x55A4] = b'90DB'
        with patch.object(builder, 'ORIGINAL_SHA256', builder.sha256(changed)):
            with self.assertRaisesRegex(ValueError, 'occurrence count'):
                builder.check_gate(bytes(changed), True)

    def test_wrong_offset_rejected(self):
        with patch.object(builder, 'OFFSET', builder.OFFSET + 1):
            with self.assertRaisesRegex(ValueError, 'offset mismatch'):
                builder.check_gate(self.original, True)

    def test_shared_literal_or_changed_caller_rejected(self):
        refs, calls = builder.pc_references(self.original, builder.elf(self.original))
        for candidate_refs, candidate_calls, message in (
                ({**refs, 0x2222: 0x5E18}, calls, 'literal XREF'),
                (refs, [0x357A, 0x2222], 'caller mismatch')):
            with self.subTest(message=message):
                with patch.object(builder, 'pc_references', return_value=(candidate_refs, candidate_calls)):
                    with self.assertRaisesRegex(ValueError, message):
                        builder.check_gate(self.original, True)

    def test_wrong_fota_aborts_without_output(self):
        with tempfile.TemporaryDirectory(prefix='mf650-patch-test-') as folder:
            root = Path(folder)
            bad = root / 'wrong.zip'
            bad.write_bytes(b'wrong version')
            with self.assertRaisesRegex(ValueError, 'FOTA SHA256 mismatch'):
                builder.build(bad, root / 'output')
            self.assertFalse((root / 'output').exists())

    def test_packages_protect_version_and_restore_original(self):
        for before, after in ((self.patched, self.original), (self.original, self.patched)):
            with self.subTest(after=builder.sha256(after)):
                raw = builder.make_package(before, after, self.members, self.seed)
                self.assertEqual(raw, builder.make_package(before, after, self.members, self.seed))
                builder.verify_trailer(raw, self.seed)
                with zipfile.ZipFile(io.BytesIO(raw)) as z:
                    self.assertEqual(len(z.namelist()), 9)
                    self.assertIsNone(z.testzip())
                    self.assertEqual(z.read(builder.ADBD_PATH), after)
                    self.assertEqual(z.getinfo(builder.ADBD_PATH).external_attr >> 16, 0o100755)
                    self.assertEqual(z.read(builder.UPDATE_DIR + 'update-binary'), self.members[builder.UPDATE_DIR + 'update-binary'])
                    self.assertEqual(z.read('checks/before.sha256'), f'{builder.sha256(before)}  /system/sbin/adbd\n'.encode())
                    self.assertEqual(z.read('checks/after.sha256'), f'{builder.sha256(after)}  /system/sbin/adbd\n'.encode())
                    script = z.read(builder.UPDATE_DIR + 'updater-script').decode()
                    mutation = script.index('package_extract_file("system/sbin/adbd", "/system/sbin/adbd")')
                    for guard in ('mf650-adbd-before.sha256") == "0"', 'mf650-adbd-backup.sha256") == "0"', 'stat -c %u:%g:%a'):
                        self.assertLess(script.index(guard), mutation)
                    self.assertGreater(script.index('mf650-adbd-after.sha256") == "0"'), mutation)
                    for forbidden in ('format(', 'write_raw_image(', 'reboot(', 'restart_adbd', 'systemrw', 'boot_hsusb_comp', 'usb_composition', '777', '5555'):
                        self.assertNotIn(forbidden, script)
                # Multi-call BusyBox must see a basename beginning with busybox.
                self.assertTrue(Path(builder.HELPER).name.startswith('busybox'))
                changed = bytearray(raw)
                changed[20] ^= 1
                with self.assertRaisesRegex(ValueError, 'trailer mismatch'):
                    builder.verify_trailer(bytes(changed), self.seed)

    def test_build_artifacts_and_refuse_overwrite(self):
        with tempfile.TemporaryDirectory(prefix='mf650-patch-test-') as folder:
            output = Path(folder) / 'output'
            with contextlib.redirect_stdout(io.StringIO()):
                report = builder.build(self.fota, output)
            self.assertEqual(report['status'], 'PATCH_READY_OFFLINE')
            self.assertEqual(report['installability'], 'UNVERIFIED')
            self.assertEqual((output / 'patched-adbd').read_bytes(), self.patched)
            self.assertTrue((output / 'binary-diff.txt').is_file())
            with zipfile.ZipFile(output / 'MF650_ADB_Rollback_v0.1.zip') as z:
                self.assertEqual(builder.sha256(z.read(builder.ADBD_PATH)), builder.ORIGINAL_SHA256)
            before = {p.name: p.read_bytes() for p in output.iterdir()}
            with self.assertRaisesRegex(ValueError, 'not empty'):
                builder.build(self.fota, output)
            self.assertEqual(before, {p.name: p.read_bytes() for p in output.iterdir()})


if __name__ == '__main__':
    unittest.main()
