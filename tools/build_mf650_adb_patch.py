"""Build the pinned MF650 vendor adbd patch offline; never contacts a device.

Requires capstone 5.0.9 and pyelftools 0.33. Output ZIPs are recovery candidates,
not evidence that the current device accepts or can recover from an update.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import struct
import zipfile

from capstone import Cs, CS_ARCH_ARM, CS_MODE_THUMB
from capstone.arm import ARM_OP_IMM, ARM_OP_MEM, ARM_OP_REG, ARM_REG_PC
from elftools.elf.elffile import ELFFile


FOTA_SHA256 = "ef46382fb3b1126c42780e141b60c0a638f7b68d3125659bfaa1c42a14ae8e8e"
ORIGINAL_SHA256 = "323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185"
PATCHED_SHA256 = "efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae"
ADBD_PATH = "system/sbin/adbd"
OFFSET = 0x5E18
SIZE = 30380
MEMBER_HASHES = {
    ADBD_PATH: ORIGINAL_SHA256,
    "META-INF/com/google/android/update-binary": "852c8ba869bc4bcbb1afb690e44f7e7af780015207380f8bb0504e084e731dd1",
    "META-INF/com/google/android/updater-script": "9509e81c3a4c704b236abc61d3fb92a683aadab134843adfe015fb47dc76eb9c",
    "file_contexts": "cf7e2d5b98b86ca5af4483f253c8b8f3f9a3cef46b777e04770a74cedfb9fb28",
    "system/usr/lib/libshared.so": "2dd01f8b1bd2256807bd83ac96ecd5900e92f0e7ba55fc7197be28c28683d9da",
    "system/bin/busybox.nosuid": "bf85d416c2109464cd533596d493054bd200fc8c2938ca97f3a92140dd1c85ab",
}
FINGERPRINTS = {
    (0x34D0, 0x3560): "3b2cf2657326f53b96249fb1ff5b3300689a5b2e5414b107b6d9e326d0befbe6",
    (0x357A, 0x358A): "748a69533948767ef8369b643665db704b5249db756fdfa97b2ea6e61ee812d4",
    (0x258C, 0x2594): "6795deb7283ffb31ef078b55eb9d6ab73b4e1cc289b3fd622d381afca480e3f5",
}
FLAG_REFS = [0x2C8C, 0x2E9C, 0x3586, 0x3648, 0x38F6, 0x390E]
SEED_SHA256 = "eb4c413cf6861cac0ae801fd53fa965c3b18b4052da6dc984e650abf239d087c"
UPDATE_DIR = "META-INF/com/google/android/"
HELPER = "/tmp/busybox.mf650-adb-patch"
TARGET = "/system/sbin/adbd"


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def require(condition, message):
    if not condition:
        raise ValueError(message)


def elf(data):
    result = ELFFile(io.BytesIO(data))
    require(result.elfclass == 32 and result.little_endian
            and result.header.e_machine == "EM_ARM"
            and result.header.e_type == "ET_DYN", "unexpected ELF identity")
    return result


def file_offset(image, address, size):
    matches = [s['p_offset'] + address - s['p_vaddr']
               for s in image.iter_segments() if s['p_type'] == 'PT_LOAD'
               and s['p_vaddr'] <= address
               and address + size <= s['p_vaddr'] + s['p_filesz']]
    require(len(matches) == 1, "VA is not uniquely file-backed")
    return matches[0]


def read_va(data, image, address, size):
    offset = file_offset(image, address, size)
    return data[offset:offset + size]


def pc_references(data, image):
    """Conservative Thumb literal/add-PC candidates in this hash-pinned ELF.

    Includes linear literal-pool decoding, so unexpected candidates abort rather
    than being silently discarded. Manual CFG/GNU review is recorded in docs.
    This is not a general computed-pointer/data-flow analyzer.
    """
    decoder = Cs(CS_ARCH_ARM, CS_MODE_THUMB)
    decoder.detail = True
    decoder.skipdata = True
    section = image.get_section_by_name('.text')
    literals, refs, calls = {}, {}, []
    for ins in decoder.disasm(section.data(), section['sh_addr']):
        if not ins.id:
            continue
        ops = ins.operands
        if ins.mnemonic.startswith('ldr') and len(ops) == 2 and ops[1].type == ARM_OP_MEM and ops[1].mem.base == ARM_REG_PC:
            slot = ((ins.address + 4) & ~3) + ops[1].mem.disp
            try:
                literals[ops[0].reg] = struct.unpack('<I', read_va(data, image, slot, 4))[0]
            except ValueError:
                literals.pop(ops[0].reg, None)
        if ins.mnemonic.startswith('add') and ops and ops[0].type == ARM_OP_REG and any(o.type == ARM_OP_REG and o.reg == ARM_REG_PC for o in ops[1:]):
            if ops[0].reg in literals:
                refs[ins.address] = (literals[ops[0].reg] + ins.address + 4) & 0xFFFFFFFF
        if ins.mnemonic in ('bl', 'blx') and ops[0].type == ARM_OP_IMM and ops[0].imm == 0x34D0:
            calls.append(ins.address)
    return refs, calls


def check_gate(data, original):
    require(len(data) == SIZE, "adbd size mismatch")
    require(sha256(data) == (ORIGINAL_SHA256 if original else PATCHED_SHA256), "adbd SHA256 mismatch")
    image = elf(data)
    require(image.header.e_entry == 0x2261 and image.header.e_flags == 0x5000400, "ELF entry/ABI mismatch")
    require(file_offset(image, 0x5E18, 5) == OFFSET, "literal offset mismatch")
    require(data.count(b'90DB') == int(original) and data.count(b'9057') == int(not original)
            and data.count(b'9059') == 1, "literal occurrence count mismatch")
    require(data[OFFSET:OFFSET + 5] == (b'90DB\0' if original else b'9057\0'), "literal bytes mismatch")
    for (start, end), digest in FINGERPRINTS.items():
        require(sha256(read_va(data, image, start, end - start)) == digest, f"instruction fingerprint mismatch at {start:#x}")
    refs, calls = pc_references(data, image)
    require([a for a, v in refs.items() if v == 0x5E18] == [0x3516], "gate literal XREF mismatch")
    require([a for a, v in refs.items() if v == 0x8044] == FLAG_REFS, "flag XREF mismatch")
    require(calls == [0x357A], "gate caller mismatch")
    require(struct.pack('<I', 0x5E18) not in data, "unexpected absolute literal pointer")

    # Resolve both compare operands from the actual LDR literal pools/add-PC.
    first = struct.unpack('<I', read_va(data, image, 0x3558, 4))[0] + 0x3516 + 4
    second = struct.unpack('<I', read_va(data, image, 0x355C, 4))[0] + 0x3522 + 4
    require((first, second) == (0x5E18, 0x5E20), "compare operand mismatch")
    decoder = Cs(CS_ARCH_ARM, CS_MODE_THUMB)
    decoder.detail = True
    instructions = {i.address: i for i in decoder.disasm(read_va(data, image, 0x34D0, 0x74), 0x34D0)}
    require(instructions[0x351C].mnemonic == 'cbz' and instructions[0x351C].operands[-1].imm == 0x353A,
            "first compare allow branch mismatch")
    require(instructions[0x353A].mnemonic == 'movs' and instructions[0x353A].operands[1].imm == 1
            and instructions[0x353C].operands[0].imm == 0x352E
            and instructions[0x3528].mnemonic == 'clz' and instructions[0x352C].mnemonic == 'lsrs',
            "allow return/second compare boolean conversion mismatch")
    labels = []
    dyn = image.get_section_by_name('.dynsym')
    for index, rel in enumerate(image.get_section_by_name('.rel.plt').iter_relocations()):
        labels.append((image.get_section_by_name('.plt')['sh_addr'] + 20 + 12 * index,
                       dyn.get_symbol(rel['r_info_sym']).name))
    require(dict(labels)[0x1EF8] == 'strcasecmp', "compare import mismatch")
    bss = image.get_section_by_name('.bss')
    require(bss['sh_addr'] <= 0x8044 < bss['sh_addr'] + bss['sh_size'], "flag not in BSS")
    require(refs[0x3586] == 0x8044 and data[0x3588:0x358A] == bytes.fromhex('0860'), "allow flag STR mismatch")
    literals = [read_va(data, image, a, 5).split(b'\0')[0].decode('ascii').lower() for a in (first, second)]
    traces = {}
    for token in ('9057', '9059', '90DB', '90570', 'FFFF', ''):
        if token.lower() == literals[0]:
            traces[token] = {'allow': 1, 'path': '3518 -> 351c(taken) -> 353a(r0=1) -> 352e -> 3588(flag=1)'}
        else:
            allow = int(token.lower() == literals[1])
            traces[token] = {'allow': allow, 'path': '3518 -> 351c(not taken) -> 3524 -> 3528(clz) -> 352c(lsr5) -> 352e -> ' + ('3588(flag=1)' if allow else '358a(no flag write)')}
    require([traces[x]['allow'] for x in ('9057', '9059', '90DB', '90570', 'FFFF', '')]
            == ([0, 1, 1, 0, 0, 0] if original else [1, 1, 0, 0, 0, 0]), "static gate truth table mismatch")
    return image, traces


def make_patch(original):
    before, _ = check_gate(original, True)
    patched = original[:OFFSET] + b'9057' + original[OFFSET + 4:]
    after, traces = check_gate(patched, False)
    differences = [i for i, (a, b) in enumerate(zip(original, patched)) if a != b]
    require(differences == [0x5E1A, 0x5E1B], "unexpected binary difference")
    require(original[:52] == patched[:52]
            and [s.header for s in before.iter_segments()] == [s.header for s in after.iter_segments()], "ELF headers changed")
    for section in before.iter_sections():
        if section.name != '.rodata':
            require(section.data() == after.get_section_by_name(section.name).data(), f"section {section.name} changed")
    return patched, traces


def checksum_seed(shared):
    image = elf(shared)
    offset = file_offset(image, 0x15000, 1983)
    seed = shared[offset:offset + 1983]
    require(seed[-1:] == b'\0' and sha256(seed[:-1]) == SEED_SHA256, "FOTA checksum seed mismatch")
    return seed[:-1]


def verify_trailer(package, seed):
    require(package[-1:] == b'\n' and package[-33:-1] == hashlib.md5(package[:-33] + seed).hexdigest().encode(),
            "vendor FOTA trailer mismatch")


def installer(before, after, helper):
    """Use original Edify updater, with staged FOTA BusyBox for strict SHA256.

    All utilities come from the pinned FOTA. Missing loader/library/applet or
    unknown current metadata/hash fails before the adbd write. No activation.
    """
    sha1 = lambda b: hashlib.sha1(b).hexdigest()
    mode_check = f'[ "$({HELPER} stat -c %u:%g:%a {TARGET})" = "0:0:755" ] && [ ! -L {TARGET} ]'
    q = json.dumps
    return f'''ui_print("MF650 adbd gate patch: recovery candidate; no activation");
assert(!is_mounted("/system"));
assert(mount("ubifs", "UBI", "system", "/system", ""));
assert(sha1_check(read_file("{TARGET}"), "{sha1(before)}"));
assert(sha1_check(package_extract_file("system/sbin/adbd"), "{sha1(after)}"));
assert(package_extract_file("patch-tools/busybox.nosuid", "{HELPER}"));
assert(sha1_check(read_file("{HELPER}"), "{sha1(helper)}"));
set_perm(0, 0, 0700, "{HELPER}");
assert(package_extract_file("checks/before.sha256", "/tmp/mf650-adbd-before.sha256"));
assert(package_extract_file("checks/after.sha256", "/tmp/mf650-adbd-after.sha256"));
assert(package_extract_file("checks/backup.sha256", "/tmp/mf650-adbd-backup.sha256"));
assert(run_program("{HELPER}", "sha256sum", "-c", "/tmp/mf650-adbd-before.sha256") == "0");
assert(run_program("{HELPER}", "sh", "-c", {q(mode_check)}) == "0");
assert(run_program("{HELPER}", "cp", "-p", "{TARGET}", "/tmp/mf650-adbd-before") == "0");
assert(run_program("{HELPER}", "sha256sum", "-c", "/tmp/mf650-adbd-backup.sha256") == "0");
assert(package_extract_file("system/sbin/adbd", "{TARGET}"));
set_perm(0, 0, 0755, "{TARGET}");
assert(run_program("{HELPER}", "sha256sum", "-c", "/tmp/mf650-adbd-after.sha256") == "0");
assert(run_program("{HELPER}", "sync") == "0");
assert(unmount("/system"));
ui_print("adbd file verified; activation requires separate user approval");
'''.encode()


def make_package(before, after, members, seed):
    entries = {
        UPDATE_DIR + 'update-binary': members[UPDATE_DIR + 'update-binary'],
        UPDATE_DIR + 'updater-script': installer(before, after, members['system/bin/busybox.nosuid']),
        'META-INF/com/android/metadata': members['META-INF/com/android/metadata'],
        'file_contexts': members['file_contexts'],
        'patch-tools/busybox.nosuid': members['system/bin/busybox.nosuid'],
        'checks/before.sha256': f'{sha256(before)}  {TARGET}\n'.encode(),
        'checks/after.sha256': f'{sha256(after)}  {TARGET}\n'.encode(),
        'checks/backup.sha256': f'{sha256(before)}  /tmp/mf650-adbd-before\n'.encode(),
        ADBD_PATH: after,
    }
    output = io.BytesIO()
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as package:
        for name, contents in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 2, 0, 0, 0))
            info.create_system = 3
            mode = 0o755 if name in (ADBD_PATH, UPDATE_DIR + 'update-binary', 'patch-tools/busybox.nosuid') else 0o644
            info.external_attr = (0o100000 | mode) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            package.writestr(info, contents, compresslevel=9)
    raw = output.getvalue()
    raw += hashlib.md5(raw + seed).hexdigest().encode() + b'\n'
    verify_trailer(raw, seed)
    with zipfile.ZipFile(io.BytesIO(raw)) as package:
        require(package.testzip() is None, "package CRC failure")
        require(set(package.namelist()) == set(entries), "unexpected package entry")
        for name, content in entries.items():
            require(package.read(name) == content, f"package round-trip failure: {name}")
        require(sha256(package.read(ADBD_PATH)) == sha256(after), "package payload hash failure")
    return raw


def build(fota, output):
    raw = fota.read_bytes()
    require(sha256(raw) == FOTA_SHA256, "FOTA SHA256 mismatch; no version substitution")
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        names = archive.namelist()
        members = {}
        for name in [*MEMBER_HASHES, 'META-INF/com/android/metadata']:
            require(names.count(name) == 1, f"missing/duplicate input member: {name}")
            members[name] = archive.read(name)
        for name, digest in MEMBER_HASHES.items():
            require(sha256(members[name]) == digest, f"input member SHA256 mismatch: {name}")
    seed = checksum_seed(members['system/usr/lib/libshared.so'])
    verify_trailer(raw, seed)
    original = members[ADBD_PATH]
    patched, traces = make_patch(original)
    # Build and verify rollback first. Payload restoration is verified offline;
    # the updater and firmware executables are never run on the PC or device.
    rollback = make_package(patched, original, members, seed)
    enable = make_package(original, patched, members, seed)
    require(not output.exists() or not any(output.iterdir()), "output directory is not empty; choose a new output path")
    output.mkdir(parents=True, exist_ok=True)
    artifacts = {'MF650_ADB_Rollback_v0.1.zip': rollback,
                 'MF650_ADB_Enable_9057_v0.1.zip': enable, 'patched-adbd': patched}
    for name, content in artifacts.items():
        (output / name).write_bytes(content)
    (output / 'original-adbd.sha256').write_text(f'{ORIGINAL_SHA256}  system/sbin/adbd\n', encoding='utf-8')
    (output / 'patched-adbd.sha256').write_text(f'{PATCHED_SHA256}  patched-adbd\n', encoding='utf-8')
    diff = 'OFFSET 0x5E18 (24088); span 4 bytes; actual changed bytes 2\nBEFORE: 39 30 44 42; ASCII: 90DB\nAFTER:  39 30 35 37; ASCII: 9057\nChanged offsets: 0x5E1A: 44 -> 35; 0x5E1B: 42 -> 37\nAll other bytes identical; ELF size 30380 unchanged.\n'
    (output / 'binary-diff.txt').write_text(diff, encoding='utf-8')
    report = {'status': 'PATCH_READY_OFFLINE', 'installability': 'UNVERIFIED', 'installed': False,
              'strategy': 'Literal', 'fota_sha256': FOTA_SHA256, 'original_sha256': ORIGINAL_SHA256,
              'patched_sha256': PATCHED_SHA256, 'file_offset': hex(OFFSET), 'literal_va': '0x5e18',
              'patch_span_bytes': 4, 'changed_bytes': 2, 'before_hex': '39304442', 'after_hex': '39303537',
              'literal_xrefs': ['0x3516'], 'gate_callers': ['0x357a'], 'flag_va': '0x8044',
              'flag_reads': ['0x2c8e', '0x2e9e', '0x364a', '0x3910'], 'flag_writes': ['0x3588', '0x38f8'],
              'gate_static_traces': traces, 'tcp_default': 7628, 'usb_composition': 'UNCHANGED',
              'runtime_boot_comp': 'UNKNOWN', 'edl_recovery': 'NOT READY',
              'elf_size': SIZE, 'elf_headers_dynamic_symbols_text_unchanged': True,
              'dt_needed': [t.needed for t in elf(patched).get_section_by_name('.dynamic').iter_tags() if t.entry.d_tag == 'DT_NEEDED'],
              'artifacts': {n: {'bytes': len(v), 'sha256': sha256(v)} for n, v in artifacts.items()},
              'limitations': ['Current recovery acceptance, helper execution and online firmware hash are unverified.',
                              'USB descriptor 9057 does not prove boot_hsusb_comp equals 9057.',
                              'Guarded rollback rejects partially corrupted or unknown binaries; it is not EDL recovery.']}
    (output / 'adb-gate.patch.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    body = f'''# MF650 adbd patch local build

Status: PATCH_READY_OFFLINE; INSTALLABILITY UNVERIFIED; installed NO; device modification NO.

Original SHA256: `{ORIGINAL_SHA256}`
Patched SHA256: `{PATCHED_SHA256}`

{diff}
9057 gate PASS (static); 9059 gate PASS (static); 90DB automatic allow removed.
USB composition unchanged; default TCP 7628 unchanged. Runtime boot file/UID unknown.

Recovery candidates reuse the original update-binary, file_contexts, metadata and
vendor checksum format. Only /system/sbin/adbd is written persistently; temporary
FOTA BusyBox provides mandatory SHA256 checks and a verified /tmp backup. Existing
root:root/0755 metadata is checked; in-place extraction preserves the inode label.
Rollback requires the exact patched hash and restores the original hash offline.
No format, raw image write, activation or reboot command is in either script.

Current recovery compatibility and signature policy remain unverified. A normal
FOTA recovery workflow may reboot. EDL recovery is NOT READY. Do not install until
the separate compatibility review and explicit user installation authorization.

'''
    for name, details in report['artifacts'].items():
        body += f'- {name}: {details["bytes"]} bytes; SHA256 `{details["sha256"]}`\n'
    (output / 'BUILD_REPORT.md').write_text(body, encoding='utf-8')
    print(json.dumps(report, indent=2))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('fota', type=Path)
    parser.add_argument('--output', type=Path, default=Path('patch-output'))
    args = parser.parse_args()
    try:
        build(args.fota, args.output)
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        parser.exit(1, f'ABORT: {error}\n')
