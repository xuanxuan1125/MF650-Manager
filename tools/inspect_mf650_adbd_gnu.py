"""Invoke existing host inspection tools; target adbd is always a data argument."""
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'analysis/adbd-threeway'
WSL_ROOT='/mnt/'+ROOT.drive[0].lower()+ROOT.as_posix()[2:]
BIN=WSL_ROOT+'/fota-analysis/binaries/arm-binutils/usr'
LD=BIN+'/lib/x86_64-linux-gnu'
def capture(command,destination):
    result=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True)
    destination.write_bytes(result.stdout)
    if result.stderr: print('host inspection stderr:',result.stderr.decode('utf-8',errors='replace')[:200])
for variant in ('original','vendor','our-patch'):
    directory=BASE/'disasm'/variant;directory.mkdir(exist_ok=True)
    target=WSL_ROOT+'/analysis/adbd-threeway/'+variant+'/adbd'
    capture(['wsl.exe','--exec','file',target],directory/'file.txt')
    capture(['wsl.exe','--exec','sha256sum',target],directory/'sha256sum.txt')
    capture(['wsl.exe','--exec','md5sum',target],directory/'md5sum.txt')
    prefix=['wsl.exe','--exec','env','LC_ALL=C','LD_LIBRARY_PATH='+LD]
    for flag,label in [('-hW','header'),('-lW','program'),('-dW','dynamic'),('-sW','symbols'),('-SW','sections'),('-nW','notes')]:
        capture(prefix+[BIN+'/bin/arm-linux-gnueabihf-readelf',flag,target],directory/('readelf-'+label+'.txt'))
    capture(prefix+[BIN+'/bin/arm-linux-gnueabihf-objdump','-d','-M','force-thumb','-j','.text',target],directory/'thumb-objdump.txt')
    capture(prefix+[BIN+'/bin/arm-linux-gnueabihf-strings','-a','-n','4','-t','x',target],directory/'strings.txt')
    print('Captured GNU readelf/objdump/strings and host file:',variant)
