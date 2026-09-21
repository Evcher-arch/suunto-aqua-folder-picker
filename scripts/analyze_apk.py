from __future__ import annotations

import argparse
import collections
import hashlib
import json
import struct
import zipfile
from pathlib import Path


KEYWORDS = [
    "suunto",
    "uptodown",
    "http://",
    "https://",
    "api",
    "okhttp",
    "retrofit",
    "certificate",
    "pinning",
    "trust",
    "root",
    "superuser",
    "install",
    "delete",
    "package",
    "account",
    "biometric",
    "firebase",
    "ad_id",
]


def read_uleb128(data: bytes, offset: int) -> tuple[int, int]:
    result = 0
    shift = 0
    while True:
        value = data[offset]
        offset += 1
        result |= (value & 0x7F) << shift
        if value & 0x80 == 0:
            return result, offset
        shift += 7


def read_dex_strings(data: bytes) -> list[str]:
    if len(data) < 112 or data[:8] not in (b"dex\n035\0", b"dex\n037\0", b"dex\n038\0", b"dex\n039\0"):
        return []
    string_ids_size = struct.unpack_from("<I", data, 0x38)[0]
    string_ids_off = struct.unpack_from("<I", data, 0x3C)[0]
    strings: list[str] = []
    for i in range(string_ids_size):
        (string_data_off,) = struct.unpack_from("<I", data, string_ids_off + i * 4)
        _, value_off = read_uleb128(data, string_data_off)
        end = data.find(b"\x00", value_off)
        if end == -1:
            continue
        raw = data[value_off:end]
        try:
            strings.append(raw.decode("utf-8", errors="replace"))
        except Exception:
            pass
    return strings


def main() -> int:
    parser = argparse.ArgumentParser(description="Create a compact APK analysis summary.")
    parser.add_argument("apk", type=Path)
    parser.add_argument("--out", type=Path, default=Path("suunto-modernization/evidence/apk-summary.json"))
    args = parser.parse_args()

    apk_bytes = args.apk.read_bytes()
    summary: dict[str, object] = {
        "apk": str(args.apk),
        "size_bytes": len(apk_bytes),
        "sha256": hashlib.sha256(apk_bytes).hexdigest(),
    }

    with zipfile.ZipFile(args.apk) as zf:
        names = zf.namelist()
        by_extension = collections.Counter(
            name.rsplit(".", 1)[-1].lower() if "." in name else "<none>"
            for name in names
        )
        dex_names = [name for name in names if name.endswith(".dex")]
        lib_names = [name for name in names if name.startswith("lib/") and name.endswith(".so")]
        summary.update(
            {
                "entry_count": len(names),
                "top_extensions": by_extension.most_common(30),
                "dex_files": dex_names,
                "native_libraries": lib_names,
                "meta_inf_sample": [name for name in names if name.startswith("META-INF/")][:50],
            }
        )

        keyword_hits: dict[str, list[str]] = {keyword: [] for keyword in KEYWORDS}
        dex_string_counts: dict[str, int] = {}
        for dex_name in dex_names:
            strings = read_dex_strings(zf.read(dex_name))
            dex_string_counts[dex_name] = len(strings)
            for value in strings:
                lower = value.lower()
                for keyword in KEYWORDS:
                    if keyword in lower and len(keyword_hits[keyword]) < 80:
                        keyword_hits[keyword].append(value)

        summary["dex_string_counts"] = dex_string_counts
        summary["keyword_hits"] = {k: v for k, v in keyword_hits.items() if v}

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(args.out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
