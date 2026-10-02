"""Decode NRV2B byte buffers as data; no ELF loading/emulation/execution."""
from pathlib import Path
import hashlib
import struct

def nrv2b_8(source, expected):
    position, bits, remaining, last_offset = 0, 0, 0, 1
    output = bytearray()
    def byte():
        nonlocal position
        if position >= len(source): raise ValueError('truncated compressed data')
        value = source[position]; position += 1
        return value
    def bit():
        nonlocal bits, remaining, position
        if remaining == 0:
            bits = byte(); remaining = 8
        value = (bits >> 7) & 1
        bits = (bits << 1) & 0xff; remaining -= 1
        return value
    while True:
        while bit():
            output.append(byte())
            if len(output) > expected: raise ValueError('output too large')
        offset = 1
        while True:
            offset = offset*2 + bit()
            if offset > 0x1000002: raise ValueError('invalid offset code')
            if bit(): break
        if offset == 2:
            offset = last_offset
        else:
            offset = (offset-3)*256 + byte()
            if offset == 0xffffffff: break
            offset += 1; last_offset = offset
        length = bit()*2 + bit()
        if length == 0:
            length = 1
            while True:
                length = length*2 + bit()
                if length > expected: raise ValueError('invalid length')
                if bit(): break
            length += 2
        length += offset > 0xd00
        if offset < 1 or offset > len(output) or len(output)+length+1 > expected:
            raise ValueError('invalid backreference')
        for _ in range(length+1): output.append(output[-offset])
    if len(output) != expected or position != len(source):
        raise ValueError(('length/consumption mismatch',len(output),expected,position,len(source)))
    return bytes(output)

BASE = Path(__file__).resolve().parents[1] / 'analysis/vendor-artifacts/extracted'
EXPECTED = {
    'forwards': '299fe42affab306b35e06aec36b0185cbea7ba39986a87dfd061de78bb4b0111',
    'webservers': '27d12ed84c9a19cfc3361c30faf8acb3d3983bda2be617a7ce212f79926b5b2e',
}

def decode(name):
    path = BASE / 'advanced-backend/ALKMF650/bin' / name
    data = path.read_bytes()
    if hashlib.sha256(data).hexdigest() != EXPECTED[name]:
        raise ValueError('input differs from the audited packed file')
    offset, blocks, records = 0xac, [], []
    for index in range(3):
        u_len,c_len,method,filter_id,filter_offset,unused = struct.unpack_from('<IIBBBB',data,offset)
        if not 0 < u_len < 2*1024*1024 or not 0 < c_len <= len(data)-offset-12:
            raise ValueError('invalid compressed block lengths')
        source = data[offset+12:offset+12+c_len]
        if method != 3 or filter_id != 0:
            raise ValueError('unsupported compression/filter method')
        output = nrv2b_8(source,u_len)
        destination = BASE / (name + '.block'+str(len(blocks))+'.bin')
        destination.write_bytes(output)
        blocks.append(output)
        records.append({'block': index, 'source_header_offset': hex(offset), 'compressed_bytes': c_len,
                        'decoded_bytes': u_len, 'decoded_sha256': hashlib.sha256(output).hexdigest()})
        offset += 12+c_len
    (BASE/(name+'.decoded-stream.bin')).write_bytes(b''.join(blocks))
    header = blocks[0]
    if not header.startswith(b'\x7fELF'):
        raise ValueError('decoded header is not ELF')
    n = struct.unpack_from('<H', header, 44)[0]
    phdr = [struct.unpack_from('<IIIIIIII', header, 52+i*32) for i in range(n)]
    loads = [p for p in phdr if p[0] == 1]
    if len(loads) != 2 or loads[0][1] != 0 or len(header+blocks[1]) != loads[0][4] or len(blocks[2]) != loads[1][4]:
        raise ValueError('decoded bytes do not match the original load segment sizes')
    image = bytearray(loads[1][1]+len(blocks[2]))
    image[:loads[0][4]] = header+blocks[1]
    image[loads[1][1]:] = blocks[2]
    struct.pack_into('<I', image, 32, 0)
    struct.pack_into('<HHH', image, 46, 0, 0, 0)
    (BASE/(name+'.load-segments.elf')).write_bytes(image)
    return {'file': name, 'packed_sha256': EXPECTED[name], 'blocks': records,
            'restored_pt_load_count': len(loads), 'derived_image_bytes': len(image),
            'derived_image_sha256': hashlib.sha256(image).hexdigest(),
            'complete_original_file_restored': False,
            'section_table_header_cleared_in_derived_image': True, 'target_executed': False}

if __name__ == '__main__':
    import json
    result = [decode(name) for name in EXPECTED]
    (BASE.parent/'reports/PACKED_ELF_RECOVERY.json').write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print('PASS: six NRV2B blocks and four PT_LOAD segments decoded as data; supplied binaries untouched')
