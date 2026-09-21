"""Replace specified DEX entries, preserving binary resources and all unrelated payloads."""
import argparse
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('source', type=Path)
parser.add_argument('output', type=Path)
parser.add_argument('--dex', action='append', default=[], help='entry=local-path')
args = parser.parse_args()
replacements = {name: Path(path) for name, path in (spec.split('=', 1) for spec in args.dex)}
args.output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(args.source) as source, zipfile.ZipFile(args.output, 'w') as output:
    for info in source.infolist():
        upper = info.filename.upper()
        if info.filename in replacements or info.filename == 'stamp-cert-sha256':
            continue
        if upper.startswith('META-INF/') and upper.endswith(('.RSA', '.DSA', '.EC', '.SF', 'MANIFEST.MF')):
            continue
        output.writestr(info, source.read(info))
    for name, path in replacements.items():
        output.writestr(name, path.read_bytes(), compress_type=zipfile.ZIP_DEFLATED)
