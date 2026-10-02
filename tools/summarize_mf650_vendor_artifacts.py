"""Offline derived metadata. Target bytes are read, never imported/executed."""
import csv,collections,hashlib,json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'fota-analysis/binaries/python-deps'))
from elftools.elf.elffile import ELFFile
BASE=ROOT/'analysis/vendor-artifacts'
REPORTS=BASE/'reports'
def load_csv(name):
    return list(csv.DictReader((REPORTS/name).open(encoding='utf-8')))
def json_out(name,data):
    (REPORTS/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def csv_out(name,data,fields):
    with (REPORTS/name).open('w',encoding='utf-8',newline='') as stream:
        writer=csv.DictWriter(stream,fieldnames=fields);writer.writeheader();writer.writerows(data)
files=load_csv('VENDOR_PACKAGE_MANIFEST.csv')
counts=collections.Counter(row['archive'] for row in files)
comparison=[]
index=json.loads((ROOT/'analysis/web8081/reports/source-index.json').read_text(encoding='utf-8'))
for source in index['sources']:
    if source['file'] not in ('html-mf650.html','html-system.html','html-at_debug.html'):continue
    body=(ROOT/'analysis/web8081/html'/source['file']).read_bytes()
    assert hashlib.sha256(body).hexdigest()==source['sha256']
    basename=source['file'].removeprefix('html-')
    for archive in counts:
        candidates=[row for row in files if row['archive']==archive and Path(row['local_path']).name.casefold()==basename.casefold()]
        if not candidates:candidates=[None]
        for candidate in candidates:
            comparison.append({'path':basename,'current_hash':source['sha256'],
                'package_hash':candidate['sha256'] if candidate else '',
                'same':bool(candidate and candidate['sha256']==source['sha256']),
                'archive':archive,'package_path':candidate['relative_path'] if candidate else '',
                'status':'FOUND' if candidate else 'NOT_PRESENT'})
csv_out('CURRENT_VS_PACKAGE_WEB.csv',comparison,list(comparison[0]))
all_current={source['sha256'] for source in index['sources'] if source.get('bytes')}
all_hash_matches=[row for row in files if row['sha256'] in all_current]

elf_records=[]
for row in files:
    if not row['file_type'].startswith('ELF'):continue
    path=ROOT/row['local_path'];packed=path.name in ('forwards','webservers')
    parsed=BASE/'extracted'/(path.name+'.load-segments.elf') if packed else path
    with parsed.open('rb') as stream:
        elf=ELFFile(stream);needed=[];interpreter=[];build=[]
        for segment in elf.iter_segments():
            if segment.header.p_type=='PT_INTERP':interpreter.append(segment.get_interp_name())
            if segment.header.p_type=='PT_NOTE':
                for note in segment.iter_notes():
                    if note['n_type']=='NT_GNU_BUILD_ID':build.append(note['n_desc'])
            if segment.header.p_type=='PT_DYNAMIC':
                for tag in segment.iter_tags():
                    if tag.entry.d_tag=='DT_NEEDED':needed.append(tag.needed)
        elf_records.append({'archive':row['archive'],'path':row['relative_path'],'size':int(row['size']),
            'sha256':row['sha256'],'machine':elf.header.e_machine,'bits':elf.elfclass,
            'little_endian':elf.little_endian,'elf_type':elf.header.e_type,
            'interpreter':interpreter,'dt_needed':needed,'build_id':build,
            'parsed_from_recovered_load_segments':packed,'parsed_image_sha256':hashlib.sha256(parsed.read_bytes()).hexdigest()})
json_out('ELF_METADATA.json',elf_records)

words=['/api/device/info','/api/device/status','/api/system/info','/api/system/check-update',
       '/api/system/apply-update','/api/at-debug','memory_usage','system_status','v5.2.4','5.2.4',
       'mf650.html','system.html','at_debug.html','cpu_usage','battery','temperature',
       '/proc/meminfo','MemAvailable','MemTotal','MemFree','Buffers','Cached','SReclaimable',
       'sysinfo','mem_usage','8081','6391','8152']
derived_hits=[]
for name in ('webservers','forwards'):
    path=BASE/'extracted'/(name+'.load-segments.elf');data=path.read_bytes()
    for word in words:
        for encoding in ('utf-8','utf-16le','utf-16be'):
            needle=word.encode(encoding);start=0
            while (offset:=data.lower().find(needle.lower(),start))>=0:
                derived_hits.append({'file':name,'derived_sha256':hashlib.sha256(data).hexdigest(),
                    'keyword':word,'encoding':encoding,'file_offset':hex(offset)})
                start=offset+len(needle)
csv_out('RECOVERED_KEYWORD_OFFSETS.csv',derived_hits,['file','derived_sha256','keyword','encoding','file_offset'])

def evidence(path,offset,text,meaning):
    data=(ROOT/path).read_bytes();needle=text.encode('utf-8')
    assert data[offset:offset+len(needle)]==needle
    return {'file':path,'sha256':hashlib.sha256(data).hexdigest(),'file_offset':hex(offset),'evidence':text,'meaning':meaning}
evidence_rows=[
evidence('analysis/vendor-artifacts/extracted/webservers.load-segments.elf',0x1e6d8,'http://0.0.0.0:6391','URL passed by reference main to listen path'),
evidence('analysis/vendor-artifacts/extracted/webservers.load-segments.elf',0x1ecc0,'mem_usage','legacy field; not current memory_usage'),
evidence('analysis/vendor-artifacts/extracted/webservers.load-segments.elf',0x1f5ec,'/api/sysinfo','legacy route literal'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x14094,'http://0.0.0.0:8152','default URL passed from main to mg_http_listen'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x14120,'/api/at','Mongoose dispatch route to AT command executor'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x141d8,'/api/lockr','reads lock configuration'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x14220,'/api/lockw','writes lock configuration and issues AT'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x140b0,'atcmd %s','execute_atcmd formats command before popen at VA 0x15a0'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x141b8,'/home/root/.sn','fn opens this path with mode w at VA 0x17f2'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x141e4,'/home/root/.lock','fn reads and writes this lock configuration'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x14234,'AT+SFUN=5','fn calls execute_atcmd at VA 0x194c'),
evidence('analysis/vendor-artifacts/network-fix/高级后台断网修复包1/main',0x14268,'AT+SFUN=4','fn calls execute_atcmd at VA 0x19b6'),
]
csv_out('STATIC_BYTE_EVIDENCE.csv',evidence_rows,list(evidence_rows[0]))
raw_hits=list(csv.DictReader((ROOT/'test-results/vendor-artifacts/ALL_KEYWORD_OFFSETS.csv').open(encoding='utf-8')))
summary=[{'archive':archive,'keyword':word,'raw_hits':sum(row['archive']==archive and row['keyword']==word for row in raw_hits)} for archive in counts for word in words]
csv_out('KEYWORD_COUNTS.csv',summary,['archive','keyword','raw_hits'])
json_out('VENDOR_ANALYSIS_RESULT.json',{
    'date':'2026-10-02','baseline_commit':'1248459c96e8d69716e39a0d283e72a94eb6a073',
    'scope':'Offline user-supplied archives and previously captured local pages only',
    'files_including_nested':len(files),'per_archive_files':dict(counts),'cross_package_identical_hashes':len(load_csv('PACKAGE_OVERLAP.csv')),
    'current_web_hash_matches_any_package_file':len(all_hash_matches),'comparison_pages':['mf650.html','system.html','at_debug.html'],
    'advanced_backend':'REFERENCE_OTHER_IMPLEMENTATION','version_classification':'UNRELATED',
    'current_device_provenance_relationship':'UNKNOWN',
    'package_backend_version':'UNKNOWN','package_install_banner':'20250830','network_fix_ui_label':'v3.0',
    'backup_wrapper_remote_risk':'R0_READ_ONLY_PULL','backup_tool_overall_safety':'UNKNOWN_BUNDLED_ADB_UNVERIFIED',
    'backup_current_backend_export':'UNKNOWN','backup_scope':['/www'],'requires_device_temp_tar':False,
    'original_backup_executed':False,'readonly_reimplementation':'DRY_RUN_ONLY_EXECUTION_DISABLED',
    'network_fix':'Installs main HTTP/AT/lock service on 8152 and replaces /www; R2/R4 capabilities',
    'packed_elf_load_segments_recovered':True,'packed_pe_installer_fully_recovered':False,
    'current_backend_found':False,'current_device_info_handler':'NOT_FOUND','current_memory_handler':'NOT_FOUND',
    'current_memory_formula':'UNKNOWN','current_memavailable':'UNKNOWN','ram_patch_status':'NOT_READY',
    'device_requests':0,'device_commands':0,'device_modifications':False,'any_attachment_executed':False,
    'adb_patch_installed':False,'9008_used':False,'closed_binaries_committed':False
})
print('PASS: page hashes compared, ELF metadata/decoded keyword offsets/byte evidence/results generated')
