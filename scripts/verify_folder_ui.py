"""Emulator-only smoke test of the real folder UI with deterministic callback fixtures."""
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build' / 'smoke'
ADB = r'C:\Users\ARCANA\AppData\Local\Android\Sdk\platform-tools\adb.exe'

def adb(*args):
    return subprocess.check_output([ADB, '-s', 'emulator-5554', *args], encoding='utf-8', errors='replace')

def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/folders-ui.xml')
    return list(ET.fromstring(adb('shell', 'cat', '/sdcard/folders-ui.xml')).iter('node'))

def tap(label):
    matches = [n for n in nodes() if n.get('text', '').casefold() == label.casefold()]
    if len(matches) != 1:
        raise AssertionError((label, [n.get('text') for n in nodes() if n.get('text')]))
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', matches[0].get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(0.25)

def screenshot(name):
    adb('shell', 'screencap', '-p', '/sdcard/folders.png')
    adb('pull', '/sdcard/folders.png', str(OUT / name))

def restart():
    adb('shell', 'am', 'force-stop', 'local.suunto.folders.test')
    adb('shell', 'am', 'start', '-n', 'local.suunto.folders.test/com.suunto.headset.ui.OfflineMusicManageActivity')
    time.sleep(0.5)
    tap('Папки наушников')
    time.sleep(2)
    tap('▸ MUSIC')

restart()
tap('▸ 2026')
labels = [n.get('text') for n in nodes()]
assert labels.index('2.mp3') < labels.index('10.mp3')
tap('2.mp3')
tap('10.mp3')
screenshot('folder-selection.png')
tap('↑')
tap('▸ Другие')
tap('2.mp3')
tap('Выбранные')
tap('/MUSIC/Другие/2.mp3')
tap('Выше')
screenshot('selected-order.png')
tap('Добавить')
text = '\n'.join(n.get('text', '') for n in nodes())
expected = 'SAVED\n/MUSIC/2026/2.mp3\n/MUSIC/Другие/2.mp3\n/MUSIC/2026/10.mp3\n'
assert expected in text, text
screenshot('saved-order.png')

restart()
tap('▸ 2026')
tap('Выбрать папку')
tap('Включая вложенные папки')
tap('Добавить')
text = '\n'.join(n.get('text', '') for n in nodes())
assert '/MUSIC/2026/Live/1.mp3' in text and '/MUSIC/Другие' not in text, text

result = {'status': 'PASS', 'scope': 'Real Android folder UI with mock device catalog and save callback',
          'checks': ['two-page loading', 'natural sorting', 'cross-folder selection',
                     'duplicate basenames', 'explicit reordering', 'save callback order', 'recursive selection'],
          'not_tested': ['Suunto runtime integration', 'Bluetooth device catalog', 'Aqua persistence']}
(OUT / 'ui-test-result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(result, ensure_ascii=False))
