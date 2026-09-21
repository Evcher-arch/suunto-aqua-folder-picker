package com.suunto.headset.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import local.suunto.music.AquaFolders;
import local.suunto.music.UsbFolderMap;

/** Emulator-only callback contract fixture; never packaged into Suunto. */
public final class OfflineMusicManageActivity extends Activity {
    public static final class Flow {
        Object value; Flow(Object value) { this.value = value; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
    }
    public static final class Key {
        final String index; Key(String index) { this.index = index; }
        public String getIndex() { return index; }
        public String getKey() { return "key" + index; }
    }
    public static final class Song {
        final String path; final Key key;
        Song(String path, String id) { this.path = path; this.key = new Key(id); }
        public String getPath() { return path; }
        public Key getSongOffsetKey() { return key; }
    }
    public static final class Item {
        final Song song; final boolean selected;
        Item(Song song, boolean selected) { this.song = song; this.selected = selected; }
        public Song getSongDetail() { return song; }
        public boolean getSelected() { return selected; }
    }
    public static final class Model {
        private final Flow _selectableSongsState = new Flow(new ArrayList<Item>());
        private boolean allSelectableSongsLoaded;
        int page;
        public Flow getDeviceDisconnected() { return new Flow(false); }
        public Flow getSelectableSongsState() { return _selectableSongsState; }
        @SuppressWarnings("unchecked") public void loadMoreSelectableSongs() {
            List<Item> values = new ArrayList<>((List<Item>) _selectableSongsState.value);
            if (page == 0) { values.add(new Item(new Song("/MUSIC/2026/10.mp3", "10"), false)); values.add(new Item(new Song("/MUSIC/2026/2.mp3", "2"), false)); }
            if (page == 1) { values.add(new Item(new Song("/MUSIC/Другие/2.mp3", "3"), false)); values.add(new Item(new Song("/MUSIC/2026/Live/1.mp3", "4"), false)); }
            page++; allSelectableSongsLoaded = page >= 2; _selectableSongsState.value = values;
        }
    }
    final Model model = new Model();
    public Model getMusicManagerViewModel() { return model; }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); TextView result = new TextView(this); result.setTextSize(20); setContentView(result);
        try {
            local.suunto.music.UsbTestProvider.run(this);
            String json = "{\"version\":1,\"entries\":[{\"index\":\"1\",\"key\":\"k\",\"rawPath\":\"a.mp3\",\"folderPath\":\"MUSIC/2026/a.mp3\"}]}";
            UsbFolderMap map = new UsbFolderMap(json, java.util.Collections.singletonList(new String[]{"1", "k", "a.mp3"}));
            if (!"MUSIC/2026/a.mp3".equals(map.get("1", "k"))) throw new AssertionError("Mapping");
            UsbFolderMap changed = new UsbFolderMap(json, java.util.Collections.singletonList(new String[]{"1", "changed", "a.mp3"}));
            if (changed.size() != 0) throw new AssertionError("Stale identity reused");
            UsbFolderMap extended = new UsbFolderMap(json, java.util.Arrays.asList(new String[]{"1", "k", "a.mp3"}, new String[]{"2", "new", "b.mp3"}));
            if (extended.size() != 1) throw new AssertionError("Existing folder lost after catalog extension");
            android.util.Log.i("AquaSmoke", "USB_MAP_TEST_PASS");
        } catch (Exception error) { throw new RuntimeException(error); }
        if (getIntent().getBooleanExtra("usbMenu", false)) {
            result.setText("Suunto Aqua\nBluetooth: disconnected");
            local.suunto.music.UsbHeadsetMenu.install(this, result);
            return;
        }
        AquaFolders.attach(this); AquaFolders.begin(model);
        AquaFolders.bind(null, new Object() {
            @SuppressWarnings("unchecked") public Object invoke(Object key, Object desired) {
                List<Item> updated = new ArrayList<>();
                for (Item item : (List<Item>) model._selectableSongsState.value) updated.add(new Item(item.song,
                    item.song.key == key ? (Boolean) desired : item.selected));
                model._selectableSongsState.value = updated; return null;
            }
        }, null, new Object() {
            @SuppressWarnings("unchecked") public Object invoke() {
                StringBuilder saved = new StringBuilder("SAVED\n");
                for (Item item : (List<Item>) model._selectableSongsState.value) if (item.selected) saved.append(item.song.path).append('\n');
                result.setText(saved.toString()); android.util.Log.i("AquaSmoke", saved.toString()); return null;
            }
        }, null);
    }
    @Override protected void onDestroy() { AquaFolders.detach(); super.onDestroy(); }
}
