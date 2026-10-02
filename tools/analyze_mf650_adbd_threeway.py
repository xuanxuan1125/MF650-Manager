"""Fixed-sample offline adbd comparison. Never executes/loads target programs.

Requires locally available pyelftools and Capstone; no dependency downloads.
Raw input copies and complete strings/disassembly stay in ignored directories.
"""
import csv
import hashlib
import json
from pathlib import Path
import re
import shutil
import struct
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'analysis/adbd-threeway'
sys.path.insert(0, str(ROOT/'fota-analysis/binaries/python-deps'))
from elftools.elf.elffile import ELFFile
from capstone import Cs, CS_ARCH_ARM, CS_MODE_THUMB

EXPECTED = {
    'original': '323b52edac198c990a0a1a9d06280d4ca02c58790844219d44c243b6915f1185',
    'vendor': 'e545cde0feedeb1deb2aa97ffe062b54caf522d4996e0db404b4fb0e7552f1c8',
    'our-patch': 'efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae',
}
KEYWORDS = ['9057','9059','90DB','7628','5555','shell:','sync:','root:',
            'unroot:','tcpip:','usb:','remount:','service.adb.tcp.port',
            'persist.adb.tcp.port','ro.secure','ro.adb.secure','ro.debuggable',
            'serialno','androidboot.serialno','md5sum','boot_hsusb_comp']

def sha(data): return hashlib.sha256(data).hexdigest()
def csv_out(name, rows):
    with (BASE/'reports'/name).open('w',encoding='utf-8',newline='') as stream:
        writer = csv.DictWriter(stream,fieldnames=list(rows[0]))
        writer.writeheader(); writer.writerows(rows)
def json_out(name, data):
    (BASE/'reports'/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def save_input(variant, data, source=None):
    if len(data)!=30380 or sha(data)!=EXPECTED[variant]:
        raise ValueError('input identity differs from audited sample: '+variant)
    target = BASE/variant/'adbd'
    if target.exists():
        if target.read_bytes()!=data: raise ValueError('would overwrite evidence')
    elif source: shutil.copy2(source,target)
    else: target.write_bytes(data)
    return target

def metadata(variant,path):
    with path.open('rb') as stream:
        elf = ELFFile(stream)
        needed,interpreter,build_id = [],'', ''
        for segment in elf.iter_segments():
            if segment.header.p_type=='PT_INTERP': interpreter=segment.get_interp_name()
            if segment.header.p_type=='PT_NOTE':
                for note in segment.iter_notes():
                    if note['n_type']=='NT_GNU_BUILD_ID':build_id=note['n_desc']
            if segment.header.p_type=='PT_DYNAMIC':
                needed=[tag.needed for tag in segment.iter_tags() if tag.entry.d_tag=='DT_NEEDED']
        sections=[{'name':s.name,'offset':s.header.sh_offset,'size':s.header.sh_size,
                   'va':s.header.sh_addr,'type':s.header.sh_type,'flags':s.header.sh_flags}
                  for s in elf.iter_sections()]
        header=dict(elf.header);header['e_ident']=dict(header['e_ident'])
        layout={'elf_header':header,'program_headers':[dict(s.header) for s in elf.iter_segments()],
                'section_headers':[dict(s.header) for s in elf.iter_sections()],
                'sections':sections,'entry':elf.header.e_entry}
        symbol=elf.get_section_by_name('.dynsym').get_symbol_by_name('_Z13service_to_fdPKc')[0]
        body=path.read_bytes()
        return {'variant':variant,'size':len(body),'sha256':sha(body),'md5':hashlib.md5(body).hexdigest(),
                'build_id':build_id,'entry':hex(elf.header.e_entry),'machine':elf.header.e_machine,
                'interpreter':interpreter,'dt_needed':'|'.join(needed),
                'service_to_fd_symbol':hex(symbol.entry.st_value),'service_to_fd_bytes':symbol.entry.st_size},layout

def differences(before,after,sections):
    offsets=[i for i,(a,b) in enumerate(zip(before,after)) if a!=b]
    blocks=[]
    for offset in offsets:
        if not blocks or offset!=blocks[-1][1]:blocks.append([offset,offset+1])
        else:blocks[-1][1]+=1
    rows=[]
    for start,end in blocks:
        section=next(s for s in sections if s['type']!='SHT_NOBITS' and s['offset']<=start and end<=s['offset']+s['size'])
        category=section['name'] if section['name'] in ('.text','.rodata','.data','.dynamic') else 'OTHER'
        rows.append({'offset_start':hex(start),'offset_end':hex(end-1),'length':end-start,
                     'before_hex':before[start:end].hex(),'after_hex':after[start:end].hex(),
                     'elf_section':section['name'],'category':category,
                     'va':hex(section['va']+start-section['offset']) if section['flags']&2 else 'N/A'})
    return rows,len(offsets)

def strings(body):
    return [(match.start(),match.group().decode('ascii')) for match in re.finditer(rb'[\x20-\x7e]{4,}',body)]

def main():
    for name in ('original','vendor','our-patch','diff','disasm','reports'):
        (BASE/name).mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(ROOT.parent/'MF650_2.3_Fota.zip') as archive:
        original=archive.read('system/sbin/adbd')
    sources={'vendor':ROOT/'analysis/vendor-artifacts/extracted/advanced-backend/ALKMF650/sbin/adbd',
             'our-patch':ROOT/'patch-output/patched-adbd'}
    data={'original':original,**{name:path.read_bytes() for name,path in sources.items()}}
    paths={name:save_input(name,body,sources.get(name)) for name,body in data.items()}
    records,layouts=[],{}
    for name,path in paths.items():
        record,layouts[name]=metadata(name,path);records.append(record)
    assert all(layout==layouts['original'] for layout in layouts.values())
    csv_out('ADBD_VARIANTS.csv',records)
    json_out('ELF_LAYOUT_COMPARISON.json',{'all_headers_and_layouts_identical':True,'original_layout':layouts['original']})
    diff_rows,pairs=[],[]
    for left,right in [('original','vendor'),('original','our-patch'),('vendor','our-patch')]:
        rows,count=differences(data[left],data[right],layouts[left]['sections'])
        diff_rows.extend({'before':left,'after':right,**row} for row in rows)
        pairs.append({'before':left,'after':right,'changed_bytes':count,'blocks':len(rows)})
    assert [(p['changed_bytes'],p['blocks']) for p in pairs]==[(37,5),(2,1),(39,6)]
    csv_out('ADBD_BINARY_DIFF.csv',diff_rows)
    json_out('ADBD_DIFF_SUMMARY.json',pairs)
    text={name:strings(body) for name,body in data.items()}
    for name,items in text.items():
        (BASE/'diff'/(name+'.strings.txt')).write_text(''.join(f'{off:08x} {value}\n' for off,value in items),encoding='utf-8')
    string_pairs=[]
    for left,right in [('original','vendor'),('original','our-patch'),('vendor','our-patch')]:
        a,b=set(text[left]),set(text[right])
        removed=[{'offset':hex(off),'string':value} for off,value in sorted(a-b)]
        added=[{'offset':hex(off),'string':value} for off,value in sorted(b-a)]
        changes=[{'offset':old['offset'],'before':old['string'],'after':new['string']}
                 for old in removed for new in added if old['offset']==new['offset']]
        string_pairs.append({'before':left,'after':right,'removed':removed,'added':added,'same_offset_changes':changes})
    keyword_rows=[]
    for name,items in text.items():
        for keyword in KEYWORDS:
            hits=[{'offset':hex(off+value.index(keyword)),'string_offset':hex(off)} for off,value in items if keyword in value]
            keyword_rows.append({'variant':name,'keyword':keyword,'hits':hits})
    json_out('ADBD_STRING_DIFF.json',{'method':'ASCII printable runs >=4; GNU strings cross-check required',
                                   'pairs':string_pairs,'keywords':keyword_rows,
                                   'numeric_port_is_instruction_not_string':True})
    instruction_rows=[];decoder=Cs(CS_ARCH_ARM,CS_MODE_THUMB)
    for name,body in data.items():
        for start,end,label in [(0x357a,0x358a,'permission_setter'),(0x258c,0x2594,'tcp_default'),
                                (0x2c8a,0x2c94,'thread_gate'),(0x2e9a,0x2ea6,'shell_gate')]:
            for ins in decoder.disasm(body[start:end],start):
                instruction_rows.append({'variant':name,'range':label,'va':hex(ins.address),
                                         'file_offset':hex(ins.address),'thumb_hex':ins.bytes.hex(),
                                         'mnemonic':ins.mnemonic,'operands':ins.op_str})
    assert data['original'][0x3580:0x3582]==bytes.fromhex('03d1')
    assert data['vendor'][0x3580:0x3582]==bytes.fromhex('0120')
    assert data['our-patch'][0x3580:0x3582]==bytes.fromhex('03d1')
    csv_out('KEY_THUMB_INSTRUCTIONS.csv',instruction_rows)
    semantics={0x1c0:('GNU build-id changed; not proof of rebuild','UNRELATED'),
               0x3580:('BNE 0x358a replaced by MOVS r0,#1; always falls through to flag write','REQUIRED_FOR_ADB'),
               0x5754:('part of displayed revision string changed','UNRELATED'),
               0x5759:('remaining changed bytes in same displayed revision string','UNRELATED'),
               0x7094:('.gnu_debuglink CRC32 changed; matching debug file unavailable','UNRELATED')}
    patch_map=[]
    for row in diff_rows:
        if row['before']=='original' and row['after']=='vendor':
            meaning,classification=semantics[int(row['offset_start'],16)]
            patch_map.append({'offset':row['offset_start'],'offset_end':row['offset_end'],'length':row['length'],
                              'original':row['before_hex'],'vendor':row['after_hex'],'section':row['elf_section'],
                              'semantic':meaning,'classification':classification,
                              'risk':'RISKY_UNCONDITIONAL_GATE' if classification=='REQUIRED_FOR_ADB' else 'METADATA_ONLY'})
    json_out('VENDOR_ADBD_PATCH_MAP.json',patch_map)
    region_rows=[]
    for label,start,end in [('main_and_pools',0x2480,0x274c),('usb_handler',0x274c,0x277c),
                            ('tcpip_handler',0x277c,0x2800),('root_handler',0x298c,0x2a34),
                            ('unroot_handler',0x2a34,0x2a84),('serial_auxiliary',0x2a84,0x2b88),
                            ('create_service_thread',0x2c50,0x2da4),('shell_handler_region',0x2e70,0x3274),
                            ('boot_token_gate_and_pools',0x34d0,0x3560),('serial_authorization_region',0x3864,0x3990)]:
        bodies=[data[name][start:end] for name in data]
        assert bodies[0]==bodies[1]==bodies[2]
        region_rows.append({'region':label,'va_start':hex(start),'va_end_exclusive':hex(end),
                            'bytes':end-start,'all_three_identical':True,'sha256':sha(bodies[0])})
    csv_out('UNCHANGED_CODE_REGIONS.csv',region_rows)
    assert struct.unpack_from('<I',data['original'],0x3a58)[0]+0x3586+4==0x8044
    assert struct.unpack_from('<I',data['original'],0x2d7c)[0]+0x2c8c+4==0x8044
    assert struct.unpack_from('<I',data['original'],0x3244)[0]+0x2e9c+4==0x8044
    assert struct.unpack_from('<I',data['original'],0x3a84)[0]+0x3648+4==0x8044
    for name,body in data.items():
        assert struct.unpack_from('<I',body,0x6df8)[0]==0x3d85
        ins=list(decoder.disasm(body[0x258c:0x2594],0x258c))
        assert ins[0].mnemonic=='movw' and ins[0].op_str=='r0, #0x1dcc'
        assert ins[1].mnemonic=='blx' and ins[1].op_str=='#0x1c64'
    install=(ROOT/'analysis/vendor-artifacts/extracted/advanced-backend/ALKMF650/install.sh').read_text(encoding='utf-8')
    skip_md5=re.search(r'if \[ "\$adb_MD5" = "([0-9a-f]{32})" \]; then',install).group(1)
    md5s={record['variant']:record['md5'] for record in records}
    assert skip_md5==md5s['vendor'] and skip_md5!=md5s['original']
    json_out('ADBD_CONTROL_FLOW_RESULT.json',{
        'scope':'Fixed local files; static instructions only; no target execution or authorization token generation',
        'service_to_fd':{'symbol':'_Z13service_to_fdPKc','thumb_symbol':'0x3561','instruction_start':'0x3560','size':1460},
        'permission_flag':{'va':'0x8044','initial_value':0,'lifetime':'process-global; no normal clear instruction added'},
        'changed_instruction':{'va':'0x3580','file_offset':'0x3580','original_hex':'03d1','original':'bne 0x358a',
                               'vendor_hex':'0120','vendor':'movs r0,#1','effect':'Always fall through to STR at 0x3588 after boot-check returns'},
        'automatic_gate':{'original':'strcasecmp(token,90DB)==0 OR strcasecmp(token,9059)==0',
                          'vendor':'TRUE when service_to_fd reaches 0x3580 normally; boot helper still called',
                          'our-patch':'strcasecmp(token,9057)==0 OR strcasecmp(token,9059)==0'},
        'automatic_gate_truth_table':[{'token':token,'original':token.upper() in ('90DB','9059'),
                                       'vendor':True,'our-patch':token.upper() in ('9057','9059')}
                                      for token in ('90DB','9057','9059','FFFF','90570','')],
        'vendor_compares_9057_directly':False,'vendor_effectively_allows_9057':True,
        'vendor_effectively_allows_9059':True,'vendor_effectively_allows_90DB':True,
        'thread_gate':{'code':'UNCHANGED','flag_read':'0x2c8e','zero_branch':'0x2c92 -> 0x2d46',
                       'effect':'Check retained; normal service prelude now provides flag=1'},
        'shell_gate':{'code':'UNCHANGED','flag_read':'0x2e9e','zero_branch':'0x2ea2 -> 0x3126'},
        'sync_dispatch':{'compare':'0x3730/0x3732','handler_pointer':'0x382c..0x3832',
                         'got_va':'0x7df8','got_file_offset':'0x6df8','callback_thumb':'0x3d85','callback_instruction_start':'0x3d84',
                         'thread_creation_call':'0x3836',
                         'gated_thread_function':'0x2c50','effect':'Reference sync handler made reachable; external library/runtime still required'},
        'serial_authorization':{'code':'UNCHANGED','effective_classification':'BYPASSED',
                                'selector':'0x364a/0x3650','zero_branch_target':'0x3864',
                                'success_write':'0x38f8','why':'Normal service prelude wrote 1 before shell selector; branch to authorization not taken'},
        'tcp_listener':{'default_port':7628,'movw':'0x258c','local_init_call':'0x2590','local_init_plt':'0x1c64','changed':False},
        'tcpip_handler':{'va':'0x277c','property_set_call':'0x27ae','property':'service.adb.tcp.port','changed':False,
                         'runtime_libcutils_in_vendor_package':'NOT_PRESENT','property_set_behavior_online':'UNKNOWN'},
        'usb_handler':{'va':'0x274c','property_set_call':'0x2758','value':'0','changed':False,
                       'new_composition_or_at_calls':False},
        'root_handler_changed':False,'standard_rsa_auth_code_changed':False,
        'auth_main':{'property':'ro.adb.secure','bool_default':False,'call':'0x24ba','auth_required_store':'0x24c4','unchanged':True},
        'root_main':{'new_uid_drop_or_force_root_code':False,'current_uid':'UNKNOWN','root_handler_reachability_expanded':True},
        'new_dangerous_call_sites':False,'existing_write_capabilities_reachability':'EXPANDED_BY_GATE',
        'install_md5':{'line':48,'comparison':'==','equal_action':'SKIP_REPLACEMENT',
                       'not_equal_action':'cp -f /tmp/sbin/adbd /sbin/ at line 52','skip_md5':skip_md5,
                       'matches_fota':False,'matches_vendor':True,'VENDOR_PATCH_TARGET_MATCH':'NO',
                       'would_replace_fota':'YES','targets_only_this_fota':'NO'},
        'patch_type':'MIXED_BRANCH_AND_BUILD_METADATA','rebuild_proven':False,
        'vendor_vs_our_strategy':'C_MORE_PERMISSIVE','VENDOR_CONFIRMS_9057_GATE_PATCH':False,
        'supports_our_two_byte_strategy':'PARTIAL; validates gate location, not literal equivalence',
        'recommended_design':'Keep existing scoped 90DB->9057 literal design; do not copy unconditional vendor setter',
        'current_online_variant':'UNKNOWN','deployable':False,'device_modifications':False,'new_zip_generated':False})
    print(json.dumps({'variants':3,'sizes':[len(body) for body in data.values()],
                      'pair_differences':pairs,'target_executed':False},ensure_ascii=False))

if __name__=='__main__':main()
