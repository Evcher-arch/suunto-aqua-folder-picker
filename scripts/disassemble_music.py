"""Keep selected class disassembly from the Android SDK's verified DEX reader."""
import argparse
import subprocess
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('apk', type=Path)
parser.add_argument('output', type=Path)
parser.add_argument('--dex', default='classes3.dex')
parser.add_argument('--match', default='com/suunto/headset/')
parser.add_argument('--dexdump', default=r'C:\Users\ARCANA\AppData\Local\Android\Sdk\build-tools\37.0.0\dexdump.exe')
args = parser.parse_args()
args.output.parent.mkdir(parents=True, exist_ok=True)
dex = args.output.with_suffix('.dex')
with zipfile.ZipFile(args.apk) as archive:
    dex.write_bytes(archive.read(args.dex))
with args.output.open('w', encoding='utf-8') as output:
    process = subprocess.Popen([args.dexdump, '-d', str(dex)], stdout=subprocess.PIPE,
                               text=True, encoding='utf-8', errors='replace')
    selected = False
    for line in process.stdout:
        if line.startswith('Class #'):
            selected = False
        if 'Class descriptor' in line:
            selected = args.match in line
        if selected:
            output.write(line)
    if process.wait() != 0:
        raise RuntimeError('dexdump failed')
print(args.output)
