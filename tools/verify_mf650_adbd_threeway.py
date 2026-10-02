"""Cross-check fixed adbd metadata against host GNU captures; no target execution."""
import csv
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'analysis/adbd-threeway'
sys.path.insert(0,str(ROOT/'tools'))
import analyze_mf650_adbd_threeway as analysis

def gnu_strings(path):
    result=set()
    for line in path.read_text(encoding='utf-8').splitlines():
        match=re.match(r'\s*([0-9a-f]+) (.*)',line)
        if match:result.add((int(match[1],16),match[2]))
    return result
def gnu_instructions(path):
    result={}
    for line in path.read_text(encoding='utf-8').splitlines():
        match=re.match(r'\s*([0-9a-f]+):\s+([0-9a-f ]+)\t([^\t]+)\t(.*)',line)
        if match:result[int(match[1],16)]=(match[2].strip(),match[3].strip(),match[4].strip())
    return result

def main():
    reports=BASE/'reports'
    variants=list(csv.DictReader((reports/'ADBD_VARIANTS.csv').open(encoding='utf-8')))
    assert len(variants)==3
    data={row['variant']:(BASE/row['variant']/'adbd').read_bytes() for row in variants}
    for row in variants:
        name=row['variant'];directory=BASE/'disasm'/name
        assert len(data[name])==30380 and hashlib.sha256(data[name]).hexdigest()==row['sha256']==analysis.EXPECTED[name]
        assert hashlib.md5(data[name]).hexdigest()==row['md5']
        assert (directory/'sha256sum.txt').read_text(encoding='utf-8').split()[0]==row['sha256']
        assert (directory/'md5sum.txt').read_text(encoding='utf-8').split()[0]==row['md5']
        notes=(directory/'readelf-notes.txt').read_text(encoding='utf-8')
        assert 'Build ID: '+row['build_id'] in notes
        header=(directory/'readelf-header.txt').read_text(encoding='utf-8')
        assert '0x2261' in header and 'ARM' in header
        needed=re.findall(r'Shared library: \[([^]]+)\]',(directory/'readelf-dynamic.txt').read_text(encoding='utf-8'))
        assert needed==row['dt_needed'].split('|')
        assert row['interpreter'] in (directory/'readelf-program.txt').read_text(encoding='utf-8')
        symbols=(directory/'readelf-symbols.txt').read_text(encoding='utf-8')
        assert re.search(r'00003561\s+1460\s+FUNC.*_Z13service_to_fdPKc',symbols)
    for label in ('header','program','dynamic','symbols','sections'):
        captures=[(BASE/'disasm'/name/('readelf-'+label+'.txt')).read_bytes() for name in data]
        assert captures[0]==captures[1]==captures[2]
    instructions={name:gnu_instructions(BASE/'disasm'/name/'thumb-objdump.txt') for name in data}
    assert len(instructions['original'])>4000
    assert instructions['original']==instructions['our-patch']
    differing=[address for address in instructions['original'] if instructions['original'][address]!=instructions['vendor'][address]]
    assert differing==[0x3580]
    assert instructions['original'][0x3580][:2]==('d103','bne.n')
    assert instructions['vendor'][0x3580][:2]==('2001','movs')
    assert '358a' in instructions['original'][0x3580][2]
    assert instructions['vendor'][0x3580][2]=='r0, #1'
    for name,table in instructions.items():
        assert table[0x258c][1]=='movw' and '7628' in table[0x258c][2]
        assert table[0x2590][1]=='blx' and 'local_init' in table[0x2590][2]
        assert 'getuid' in table[0x299e][2] and 'property_get_bool' in table[0x24ba][2]
        assert 'property_set' in table[0x2758][2] and 'property_set' in table[0x27ae][2]
    string_diff=json.loads((reports/'ADBD_STRING_DIFF.json').read_text(encoding='utf-8'))
    strings={name:gnu_strings(BASE/'disasm'/name/'strings.txt') for name in data}
    for pair in string_diff['pairs']:
        left,right=pair['before'],pair['after']
        assert {(int(r['offset'],16),r['string']) for r in pair['removed']}==strings[left]-strings[right]
        assert {(int(r['offset'],16),r['string']) for r in pair['added']}==strings[right]-strings[left]
    # GNU cmp uses 1-based positions and octal bytes, independently of Python grouping.
    wsl_root='/mnt/'+ROOT.drive[0].lower()+ROOT.as_posix()[2:]
    diff_rows=list(csv.DictReader((reports/'ADBD_BINARY_DIFF.csv').open(encoding='utf-8')))
    for left,right in [('original','vendor'),('original','our-patch'),('vendor','our-patch')]:
        result=subprocess.run(['wsl.exe','--exec','cmp','-l',wsl_root+'/analysis/adbd-threeway/'+left+'/adbd',
                               wsl_root+'/analysis/adbd-threeway/'+right+'/adbd'],capture_output=True)
        assert result.returncode==1 and not result.stderr
        (BASE/'diff'/(left+'-vs-'+right+'.cmp.txt')).write_bytes(result.stdout)
        actual=[(int(parts[0])-1,int(parts[1],8),int(parts[2],8)) for line in result.stdout.splitlines() if (parts:=line.split())]
        expected=[]
        for row in diff_rows:
            if row['before']==left and row['after']==right:
                start=int(row['offset_start'],16);a=bytes.fromhex(row['before_hex']);b=bytes.fromhex(row['after_hex'])
                expected.extend((start+i,x,y) for i,(x,y) in enumerate(zip(a,b)))
        assert actual==expected
    # A diff round trip proves all changed bytes are accounted for; nothing installed.
    restored=bytearray(data['original'])
    for row in diff_rows:
        if row['before']=='original' and row['after']=='vendor':
            start=int(row['offset_start'],16);restored[start:start+int(row['length'])]=bytes.fromhex(row['after_hex'])
    assert bytes(restored)==data['vendor']
    # Wrong inputs must be rejected before a working evidence copy is written.
    try: analysis.save_input('original',b'wrong')
    except ValueError: pass
    else:raise AssertionError('invalid input accepted')
    changed=bytearray(data['original']);changed[0x3580]^=1
    try: analysis.save_input('original',bytes(changed))
    except ValueError: pass
    else:raise AssertionError('same-length changed hash accepted')
    check={'status':'PASS','variants':3,'python_diff_vs_gnu_cmp':'PASS','gnu_headers_dynamic_symbols_sections':'IDENTICAL',
           'build_ids_vs_gnu_readelf':'PASS','string_diffs_vs_gnu_strings':'PASS',
           'capstone_vs_gnu_changed_instruction':'PASS','only_vendor_text_change':'0x3580',
           'tcp_root_auth_usb_call_checks':'PASS','in_memory_diff_round_trip':'PASS',
           'wrong_size_and_same_length_wrong_hash_rejected':'PASS','hashes_vs_gnu_sha256sum_md5sum':'PASS',
           'target_adbd_executed':False,'device_requests':0,'device_modifications':False,'new_enable_zip_generated':False}
    (reports/'ADBD_OFFLINE_VERIFICATION.json').write_text(json.dumps(check,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(check))

if __name__=='__main__':main()
