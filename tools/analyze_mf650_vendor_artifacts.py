"""Offline ZIP inventory/extraction and literal search; never loads target code."""
import csv
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil
import stat
import struct
import zipfile
import zlib

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'analysis/vendor-artifacts'
PACKAGES = {
    'web后台备份工具.zip': 'backup-tool',
    '阿乐卡MF650高级后台.zip': 'advanced-backend',
    '高级后台断网修复包-1.zip': 'network-fix',
}
KEYWORDS = ['8081', '/api/device/info', '/api/device/status', '/api/system/info',
            '/api/system/check-update', '/api/system/apply-update', '/api/at-debug',
            'memory_usage', 'system_status', 'v5.2.4', '5.2.4', 'mf650.html', 'system.html',
            'at_debug.html', 'cpu_usage', 'battery', 'temperature',
            'mem_usage', '/proc/meminfo', 'MemTotal', 'MemFree', 'MemAvailable',
            'Buffers', 'Cached', 'SReclaimable', 'sysinfo', 'version',
            '192.168.100.1', '7628', '7689', '5555', 'tar', 'adb', 'reboot']

def write_csv(path, rows, columns):
    with path.open('w', encoding='utf-8', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)

def classify(data, path):
    if data.startswith(b'\x7fELF'):
        order = '<' if data[5] == 1 else '>'
        machine = struct.unpack_from(order+'H', data, 18)[0]
        return f'ELF{32 if data[4] == 1 else 64} machine={machine} endian={data[5]}', True, 'BINARY'
    if data.startswith(b'MZ'):
        return 'PE/MZ', True, 'BINARY'
    for magic, name in ((b'PK\x03\x04', 'ZIP'), (b'\x89PNG', 'PNG'), (b'GIF8', 'GIF'), (b'\xff\xd8\xff', 'JPEG'), (b'\x1f\x8b', 'GZIP')):
        if data.startswith(magic):
            return name, False, 'BINARY'
    executable = path.suffix.lower() in ('.bat', '.cmd', '.ps1', '.sh', '.py') or data.startswith(b'#!')
    if b'\0' not in data and not any(value < 9 or 13 < value < 32 for value in data):
        for encoding in ('utf-8-sig', 'gb18030'):
            try:
                data.decode(encoding)
                return 'TEXT/'+encoding, executable, 'TEXT'
            except UnicodeDecodeError:
                pass
    return 'DATA', executable, 'BINARY'

def safe_path(destination, name):
    parts = PurePosixPath(name.replace('\\', '/')).parts
    if not parts or name.startswith(('/', '\\')) or any(part in ('.', '..') or ':' in part for part in parts):
        raise ValueError('unsafe archive member path')
    if any(part.rstrip(' .') != part for part in parts):
        raise ValueError('unsafe Windows archive member path')
    reserved = {'CON', 'PRN', 'AUX', 'NUL'} | {f'{name}{index}' for name in ('COM', 'LPT') for index in range(1,10)}
    if any(part.split('.')[0].upper() in reserved for part in parts):
        raise ValueError('Windows device name in archive member')
    target = destination.joinpath(*parts).resolve()
    if not target.is_relative_to(destination.resolve()):
        raise ValueError('archive path escaped destination')
    return target

def extract(archive_path, destination, top_archive, prefix='', depth=0):
    if depth > 3:
        raise ValueError('unexpected archive nesting')
    with zipfile.ZipFile(archive_path) as archive:
        if sum(item.file_size for item in archive.infolist()) > 256*1024*1024:
            raise ValueError('unexpectedly large expanded archive')
        seen = set()
        for item in archive.infolist():
            target = safe_path(destination, item.filename)
            key = str(target).casefold()
            if key in seen:
                raise ValueError('duplicate/case-colliding archive member')
            seen.add(key)
            if item.flag_bits & 1 or stat.S_ISLNK(item.external_attr >> 16):
                raise ValueError('encrypted or symlink member needs manual review')
            if item.is_dir():
                target.mkdir(parents=True, exist_ok=True)
                continue
            data = archive.read(item)  # ZIP CRC is checked here.
            if len(data) != item.file_size or zlib.crc32(data) & 0xffffffff != item.CRC:
                raise ValueError('member length/CRC mismatch')
            target.parent.mkdir(parents=True, exist_ok=True)
            if target.exists():
                if target.read_bytes() != data:
                    raise ValueError('would overwrite existing evidence')
            else:
                target.write_bytes(data)
            kind, executable, text_binary = classify(data, target)
            record = {'archive': top_archive, 'relative_path': prefix+item.filename,
                      'size': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
                      'file_type': kind, 'extension': target.suffix.lower(),
                      'executable': executable, 'text_binary': text_binary,
                      'zip_crc32': f'{item.CRC:08x}', 'zip_mtime': str(item.date_time),
                      'local_path': target.relative_to(ROOT).as_posix()}
            files.append(record)
            for word in KEYWORDS:
                for encoding in ('utf-8', 'utf-16le', 'utf-16be'):
                    needle, lower = word.encode(encoding).lower(), data.lower()
                    start = 0
                    while (offset := lower.find(needle, start)) >= 0:
                        hits.append({'archive': top_archive, 'relative_path': record['relative_path'],
                                     'sha256': record['sha256'], 'keyword': word, 'encoding': encoding,
                                     'offset': hex(offset)})
                        start = offset + len(needle)
            if zipfile.is_zipfile(target):
                inner_destination = BASE / 'extracted' / PACKAGES[top_archive] / target.stem
                extract(target, inner_destination, top_archive, prefix+item.filename+'!', depth+1)

def main():
    for name in ('original', 'extracted', 'backup-tool', 'advanced-backend', 'network-fix', 'reports', 'hashes'):
        (BASE / name).mkdir(parents=True, exist_ok=True)
    artifacts = []
    for name, category in PACKAGES.items():
        source = ROOT / 'vendor-artifacts' / name
        data = source.read_bytes()
        original = BASE / 'original' / name
        if original.exists():
            if original.read_bytes() != data:
                raise ValueError('original ZIP differs from the supplied evidence')
        else:
            shutil.copy2(source, original)
        with zipfile.ZipFile(original) as archive:
            members = archive.infolist()
            if archive.testzip() is not None:
                raise ValueError('invalid ZIP CRC')
        artifacts.append({'filename': name, 'size': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
                          'md5': hashlib.md5(data).hexdigest(), 'archive_crc32': f'{zlib.crc32(data)&0xffffffff:08x}',
                          'zip_member_crc': 'PASS', 'members': len(members),
                          'file_mtime_utc': datetime.fromtimestamp(source.stat().st_mtime, timezone.utc).isoformat()})
        extract(original, BASE / category, name)
    write_csv(BASE / 'reports/VENDOR_ARTIFACTS.csv', artifacts, list(artifacts[0]))
    write_csv(BASE / 'reports/VENDOR_PACKAGE_MANIFEST.csv', files, list(files[0]))
    private = ROOT / 'test-results/vendor-artifacts'
    private.mkdir(parents=True, exist_ok=True)
    columns = ['archive','relative_path','sha256','keyword','encoding','offset']
    write_csv(private / 'ALL_KEYWORD_OFFSETS.csv', hits, columns)
    core = set(KEYWORDS) - {'version', 'tar', 'adb', 'reboot', 'Buffers', 'Cached'}
    write_csv(BASE / 'reports/KEYWORD_OFFSETS.csv', [row for row in hits if row['keyword'] in core], columns)
    groups = {}
    for record in files:
        groups.setdefault(record['sha256'], []).append(record)
    overlaps = [{'sha256': sha, 'archives': '|'.join(sorted({row['archive'] for row in group})),
                 'paths': '|'.join(row['relative_path'] for row in group), 'size': group[0]['size']}
                for sha, group in groups.items() if len({row['archive'] for row in group}) > 1]
    write_csv(BASE / 'reports/PACKAGE_OVERLAP.csv', overlaps, ['sha256','archives','paths','size'])
    (BASE / 'hashes/SHA256SUMS.txt').write_text(''.join(row['sha256']+'  '+row['filename']+'\n' for row in artifacts), encoding='utf-8')
    print(json.dumps({'archives': len(artifacts), 'files_including_nested': len(files), 'literal_hits': len(hits), 'cross_package_identical_hashes': len(overlaps)}))

files, hits = [], []
if __name__ == '__main__':
    main()
