import hashlib
import json
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
apk = root / 'release' / 'Suunto-6.13.8-Folders-Beta1-universal.apk'
parts = root / 'release' / 'suunto-6.13.8-folders-beta1'
manifest = (root / 'release' / 'universal-manifest.txt').read_text(encoding='utf-8-sig')
for attribute in ('isSplitRequired', 'requiredSplitTypes', 'com.android.vending.splits.required'):
    assert attribute not in manifest, attribute
with zipfile.ZipFile(apk) as merged:
    assert len(merged.namelist()) == len(set(merged.namelist())), 'Duplicate ZIP entries'
    assert merged.testzip() is None
    dex_count = 0
    lib_count = 0
    for part in parts.glob('*.apk'):
        with zipfile.ZipFile(part) as source:
            for name in source.namelist():
                if name.endswith('.dex') or name.startswith('lib/') and name.endswith('.so'):
                    assert source.read(name) == merged.read(name), name
                    if name.endswith('.dex'): dex_count += 1
                    else: lib_count += 1
    assert dex_count == 13 and lib_count == 30
report = {'status': 'PASS', 'dex_count': dex_count, 'native_library_count': lib_count,
          'bytes': apk.stat().st_size, 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest()}
(root / 'release' / 'universal-payload-audit.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report))
