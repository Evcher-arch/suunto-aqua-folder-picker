"""Read Aqua USB filenames and correlate only unique matches with its app catalog."""
import argparse
import collections
import json
import unicodedata
from pathlib import Path

def canonical(name):
    return unicodedata.normalize('NFC', name).casefold()

def make_index(catalog, root):
    files = collections.defaultdict(list)
    root = root.resolve(strict=True)
    for directory in ('MUSIC', 'SYSTEM'):
        target = root / directory
        if not target.is_dir():
            continue
        for file in target.rglob('*'):
            if file.is_file() and file.resolve().is_relative_to(root):
                files[canonical(file.name)].append(file.relative_to(root).as_posix())
    catalog_names = collections.Counter(canonical(row['path'].replace('\\', '/').split('/')[-1]) for row in catalog)
    entries = []
    for row in catalog:
        name = canonical(row['path'].replace('\\', '/').split('/')[-1])
        matches = files[name]
        mapped = matches[0] if len(matches) == 1 and catalog_names[name] == 1 else None
        entries.append(dict(index=row['index'], key=row['key'], rawPath=row['path'], folderPath=mapped,
                            status='matched' if mapped else 'ambiguous' if matches else 'missing'))
    return {'version': 1, 'source': 'Aqua USB directory snapshot', 'entries': entries}

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('catalog', type=Path)
    parser.add_argument('usb_root', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    result = make_index(json.loads(args.catalog.read_text(encoding='utf-8-sig')), args.usb_root)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(dict(collections.Counter(row['status'] for row in result['entries']))))
