"""Apply the RuStore permission profile to the decoded v1.0.0 unique APK."""
import argparse
import re
from pathlib import Path
import xml.etree.ElementTree as ET

REMOVED = {
    'QUERY_ALL_PACKAGES', 'READ_CALL_LOG', 'READ_CONTACTS',
    'ACCESS_BACKGROUND_LOCATION', 'ACCESS_FINE_LOCATION',
    'ACCESS_COARSE_LOCATION', 'READ_PHONE_STATE', 'MANAGE_ONGOING_CALLS',
    'BIND_NOTIFICATION_LISTENER_SERVICE',
}
ANDROID = 'http://schemas.android.com/apk/res/android'
NAME = '{%s}name' % ANDROID


def method(text, signature, transform):
    pattern = r'(?m)^\.method ' + re.escape(signature) + r'\n.*?^\.end method'
    result, count = re.subn(pattern, lambda m: transform(m.group()), text, flags=re.S)
    if count != 1:
        raise ValueError('Expected one method: ' + signature)
    return result


def apply(root):
    manifest = root / 'AndroidManifest.xml'
    ET.register_namespace('android', ANDROID)
    tree = ET.parse(manifest)
    xml = tree.getroot()
    if xml.get('package') != 'com.evcherarch.suuntoaquamusicfolders':
        raise ValueError('Expected the unique-package v1.0.0 build')
    found = set()
    for node in list(xml):
        permission = node.get(NAME, '').removeprefix('android.permission.')
        if node.tag.startswith('uses-permission') and permission in REMOVED:
            found.add(permission)
            xml.remove(node)
    if found != REMOVED:
        raise ValueError('Unexpected baseline permissions: ' + repr(found))
    app = xml.find('application')
    # Remove the listener component, never expose it by stripping its bind guard.
    listeners = [n for n in app.findall('service') if n.get(NAME) ==
                 'com.suunto.connectivity.notifications.AncsService']
    if len(listeners) != 1:
        raise ValueError('Expected one ANCS notification listener')
    app.remove(listeners[0])

    patches = {}
    path = root / 'smali_classes8/com/stt/android/bluetooth/BleScanner.smali'
    def scanner(body):
        # Android 12L+ with neverForLocation needs Nearby Devices, not GPS.
        body, n = re.subn(r'(?s)(    \.locals 2\n).*?    :cond_0\n',
                          r'\1\n', body, count=1)
        if n != 1:
            raise ValueError('Missing legacy location permission block')
        body, n = re.subn(r'(?s)    :cond_1\n.*?    :cond_2\n',
                          '    :cond_1\n', body, count=1)
        if n != 1:
            raise ValueError('Missing legacy GPS provider block')
        if 'ACCESS_' in body or 'LocationManager' in body:
            raise ValueError('Unexpected location dependency')
        return body
    patches[path] = method(path.read_text(encoding='utf-8'),
        'public c(Lcom/stt/android/bluetooth/BluetoothScanListener;)V', scanner)

    # Only BLE-specific gates change. General location checks still return denied.
    path = root / 'smali_classes3/com/suunto/soa/ble/permission/BlePermission.smali'
    text = path.read_text(encoding='utf-8')
    for name in ('isLocationAllowed', 'isLocationEnabled'):
        signature = 'public final ' + name + '()Z'
        text = method(text, signature, lambda body:
            body.split('\n', 1)[0] + '\n    .locals 1\n'
            '    # No location prerequisite for BLE on supported API 32+.\n'
            '    const/4 v0, 0x1\n    return v0\n.end method')
    patches[path] = text

    path = root / 'smali_classes7/com/suunto/connectivity/util/LocationHelper.smali'
    text = path.read_text(encoding='utf-8')
    for name in ('isLocationPermissionGranted', 'isLocationProviderEnabled'):
        text = method(text, 'public ' + name + '()Z', lambda body:
            body.split('\n', 1)[0] + '\n    .locals 1\n'
            '    # This helper is used by the BLE scanner only.\n'
            '    const/4 v0, 0x1\n    return v0\n.end method')
    patches[path] = text

    config = root / 'apktool.yml'
    yml = config.read_text(encoding='utf-8')
    if 'minSdkVersion: 32' not in yml:
        raise ValueError('This patch requires minSdkVersion 32')
    yml, count = re.subn(r'(?m)^  versionCode: \d+$', '  versionCode: 6013009', yml)
    if count != 1:
        raise ValueError('Missing versionCode')
    yml, count = re.subn(r'(?m)^  versionName: .+$', '  versionName: 1.0.1-rustore', yml)
    if count != 1:
        raise ValueError('Missing versionName')
    # Validate all inputs before changing the decoded build.
    tree.write(manifest, encoding='utf-8', xml_declaration=True)
    config.write_text(yml, encoding='utf-8')
    for path, text in patches.items():
        path.write_text(text, encoding='utf-8')
    print('Removed all 9 requested permissions and ANCS service; patched 3 BLE classes.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('decoded', type=Path)
    apply(parser.parse_args().decoded)
