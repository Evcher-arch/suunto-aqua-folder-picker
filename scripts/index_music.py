"""Index music-related DEX strings without executing APK contents."""
import json
import sys
import zipfile
from pathlib import Path
from analyze_apk import read_dex_strings

apk = Path(sys.argv[1])
output = Path(sys.argv[2])
needles = ('offline', 'playlist', 'music', 'relative_path', 'folder', 'storage/impl')
result = {}
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if name.endswith('.dex'):
            values = read_dex_strings(archive.read(name))
            result[name] = [v for v in values if any(n in v.lower() for n in needles)]
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print(output)
