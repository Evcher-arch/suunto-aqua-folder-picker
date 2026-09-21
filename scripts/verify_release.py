"""Audit unchanged APK payloads and create the installable split archive."""
import hashlib
import json
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
release = root / 'release' / 'suunto-6.13.8-folders-beta1'
original = root / 'evidence' / 'suunto-6.13.8'

def signature(name):
    return name == 'stamp-cert-sha256' or (name.upper().startswith('META-INF/') and name.upper().endswith(('.RSA', '.DSA', '.EC', '.SF', 'MANIFEST.MF')))

results = []
for apk in sorted(release.glob('*.apk')):
    with zipfile.ZipFile(original / apk.name) as before, zipfile.ZipFile(apk) as after:
        changes = {'classes3.dex', 'classes13.dex'} if apk.name == 'com.stt.android.suunto.apk' else set()
        expected = {n for n in before.namelist() if not signature(n)} | changes
        actual = {n for n in after.namelist() if not signature(n)}
        assert expected == actual, (apk.name, expected ^ actual)
        checked = 0
        for name in expected - changes:
            assert before.read(name) == after.read(name), (apk.name, name)
            checked += 1
        results.append({'apk': apk.name, 'unchanged_entries': checked, 'modified_entries': sorted(changes),
                        'sha256': hashlib.sha256(apk.read_bytes()).hexdigest()})
assert len(results) == 7
archive = release.parent / 'suunto-6.13.8-folders-beta1.xapk'
with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED) as output:
    for apk in sorted(release.glob('*.apk')): output.write(apk, apk.name)
report = {'payload_audit': 'PASS', 'apks': results, 'archive': archive.name,
          'archive_sha256': hashlib.sha256(archive.read_bytes()).hexdigest()}
(release / 'payload-audit.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report, indent=2))
