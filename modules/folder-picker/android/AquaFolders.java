package local.suunto.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Version-specific bridge for Suunto 6.13.8. Uses the original selection/save callbacks. */
public final class AquaFolders {
    private static WeakReference<Activity> host = new WeakReference<>(null);
    private static Object model;
    private static boolean pending;
    private static int generation;
    private static Object toggle, confirm;
    private static AlertDialog chooser;
    private static Browser browser;

    public static void attach(Activity activity) {
        if (!activity.getClass().getName().equals("com.suunto.headset.ui.OfflineMusicManageActivity")) return;
        detach();
        host = new WeakReference<>(activity);
        try { model = call(activity, "getMusicManagerViewModel"); }
        catch (Exception e) { fail(activity, e); }
    }

    public static void detach() {
        generation++;
        pending = false;
        if (chooser != null) chooser.dismiss();
        if (browser != null) browser.dialog.dismiss();
        chooser = null; browser = null; model = null; toggle = null; confirm = null;
        host.clear();
    }

    public static void begin(Object viewModel) {
        if (viewModel != model) return;
        generation++;
        if (browser != null) browser.dialog.dismiss();
        pending = true;
    }

    public static void bind(Object items, Object onToggle, Object onCancel, Object onConfirm, Object onMore) {
        Activity activity = host.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || model == null) return;
        toggle = onToggle;
        confirm = onConfirm;
        if (!pending) return;
        pending = false;
        final int token = generation;
        new Handler(Looper.getMainLooper()).post(() -> {
            if (token != generation || activity.isFinishing() || activity.isDestroyed()) return;
            chooser = new AlertDialog.Builder(activity).setTitle("Добавить музыку")
                .setItems(new String[]{"Все треки", "Папки наушников"}, (dialog, which) -> {
                    if (which == 1) {
                        try { browser = new Browser(activity, token); browser.start(); }
                        catch (Exception e) { fail(activity, e); }
                    }
                }).setNegativeButton("Назад", null).create();
            chooser.show();
        });
    }

    private static Object call(Object target, String name, Object... args) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == args.length) {
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Field field(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object flow(String getter) throws Exception { return call(call(model, getter), "getValue"); }
    private static void fail(Activity activity, Exception error) {
        android.util.Log.e("AquaFolders", "Folder picker integration error", error);
        if (activity != null && !activity.isFinishing()) Toast.makeText(activity,
            "Не удалось открыть папки. Доступен обычный список треков.", Toast.LENGTH_LONG).show();
    }

    private static final class Browser {
        final Activity activity;
        final int token;
        final Handler handler = new Handler(Looper.getMainLooper());
        final AlertDialog dialog;
        final TextView path;
        final TextView status;
        final ListView list;
        final Button up, all, selected, retry;
        final FolderSelection<Object> state = new FolderSelection<>();
        final Map<String, Object> initial = new HashMap<>();
        final List<Object> rows = new ArrayList<>();
        String folder = "";
        boolean selectedView;
        boolean ready;
        boolean closed;
        long lastProgress = SystemClock.elapsedRealtime();
        int lastSize = -1;
        List<?> catalog = Collections.emptyList();
        String indexStatus = "";

        Browser(Activity activity, int token) {
            this.activity = activity; this.token = token;
            int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(pad, pad / 2, pad, 0);
            path = new TextView(activity); path.setTextSize(17); root.addView(path);
            status = new TextView(activity); root.addView(status);
            LinearLayout commands = new LinearLayout(activity);
            root.addView(commands);
            up = button(commands, "↑", "Родительская папка");
            all = button(commands, "Выбрать папку", "Выбрать треки текущей папки");
            selected = button(commands, "Выделить все", "Выбрать все файлы в текущей папке");
            list = new ListView(activity);
            list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            root.addView(list, new LinearLayout.LayoutParams(-1,
                (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.42)));
            retry = new Button(activity); retry.setText("Повторить загрузку");
            root.addView(retry); retry.setVisibility(View.GONE);
            dialog = new AlertDialog.Builder(activity).setTitle("Папки наушников").setView(root)
                .setPositiveButton("Добавить", null).setNegativeButton("Отмена", null)
                .setNeutralButton("Все треки", null).create();
            dialog.setOnDismissListener(d -> { closed = true; handler.removeCallbacksAndMessages(null); });
            up.setOnClickListener(v -> {
                if (selectedView) selectedView = false;
                else { int slash = folder.lastIndexOf('/'); folder = slash < 0 ? "" : folder.substring(0, slash); }
                render();
            });
            all.setOnClickListener(v -> {
                if (!ready) return;
                new AlertDialog.Builder(activity).setItems(new String[]{"Только эта папка", "Включая вложенные папки"},
                    (d, which) -> { state.selectFolder(folder, which == 1); render(); }).show();
            });
            selected.setOnClickListener(v -> {
                if (!ready || selectedView) return;
                List<FolderSelection.Track<Object>> current = state.songs(folder, false);
                boolean allSelected = !current.isEmpty();
                for (FolderSelection.Track<Object> track : current) {
                    if (!state.selection().contains(track)) { allSelected = false; break; }
                }
                for (FolderSelection.Track<Object> track : current) state.select(track, !allSelected);
                render();
            });
            retry.setOnClickListener(v -> { lastProgress = SystemClock.elapsedRealtime(); retry.setVisibility(View.GONE); poll(); });
            list.setOnItemClickListener((parent, view, position, id) -> {
                Object row = rows.get(position);
                if (row instanceof String) { folder = (String) row; render(); }
                else {
                    @SuppressWarnings("unchecked") FolderSelection.Track<Object> track = (FolderSelection.Track<Object>) row;
                    if (selectedView) {
                        new AlertDialog.Builder(activity).setTitle(track.name()).setItems(
                            new String[]{"Выше", "Ниже", "Убрать"}, (d, action) -> {
                                int at = state.selection().indexOf(track);
                                if (action == 0 && at > 0) state.move(at, at - 1);
                                if (action == 1 && at < state.selection().size() - 1) state.move(at, at + 1);
                                if (action == 2) state.select(track, false);
                                render();
                            }).show();
                    } else { state.select(track, !state.selection().contains(track)); render(); }
                }
            });
        }

        Button button(LinearLayout parent, String label, String description) {
            Button b = new Button(activity); b.setText(label); b.setTextSize(12); b.setAllCaps(false);
            b.setContentDescription(description); b.setTooltipText(description);
            parent.addView(b, new LinearLayout.LayoutParams(0,
                (int) (56 * activity.getResources().getDisplayMetrics().density), 1)); return b;
        }

        void start() {
            dialog.show();
            dialog.getWindow().setLayout(-1, -2);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> commit(true));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                if (ready) commit(false); else dialog.dismiss();
            });
            render(); poll();
        }

        void poll() {
            if (closed) return;
            if (token != generation || activity.isFinishing() || activity.isDestroyed() || model == null) { dialog.dismiss(); return; }
            try {
                if (Boolean.TRUE.equals(flow("getDeviceDisconnected"))) {
                    ready = false; render(); status.setText("Наушники отключены. Подключите их и откройте выбор заново."); return;
                }
                if (ready) { handler.postDelayed(this::poll, 700); return; }
                catalog = (List<?>) flow("getSelectableSongsState");
                if (catalog.size() != lastSize) { lastSize = catalog.size(); lastProgress = SystemClock.elapsedRealtime(); }
                boolean complete = field(model, "allSelectableSongsLoaded").getBoolean(model);
                status.setText("Загрузка каталога: " + catalog.size());
                if (complete) { populate(); ready = true; render(); handler.postDelayed(this::poll, 700); return; }
                if (SystemClock.elapsedRealtime() - lastProgress > 30000) {
                    status.setText("Каталог загружен не полностью. Повторите загрузку или откройте все треки.");
                    retry.setVisibility(View.VISIBLE); return;
                }
                call(model, "loadMoreSelectableSongs");
                handler.postDelayed(this::poll, 700);
            } catch (Exception e) {
                status.setText("Ошибка чтения каталога. Доступен обычный список треков.");
                retry.setVisibility(View.VISIBLE); android.util.Log.e("AquaFolders", "Catalog", e);
            }
        }

        void populate() throws Exception {
            List<FolderSelection.Track<Object>> tracks = new ArrayList<>();
            org.json.JSONArray diagnostic = new org.json.JSONArray();
            List<String[]> identities = new ArrayList<>();
            for (Object item : catalog) {
                Object song = call(item, "getSongDetail");
                Object key = call(song, "getSongOffsetKey");
                identities.add(new String[]{(String) call(key, "getIndex"), (String) call(key, "getKey"), (String) call(song, "getPath")});
            }
            UsbFolderMap folderMap = null;
            java.io.File external = activity.getExternalFilesDir(null);
            if (external != null) {
                java.io.File mapFile = new java.io.File(external, "aqua-folder-map.json");
                if (mapFile.isFile()) {
                    try {
                        folderMap = new UsbFolderMap(new String(java.nio.file.Files.readAllBytes(mapFile.toPath()), java.nio.charset.StandardCharsets.UTF_8), identities);
                        indexStatus = "Папки определены для " + folderMap.size() + " из " + catalog.size() + " треков. "
                            + (folderMap.size() < catalog.size() ? "Остальные показаны без папок; обновите USB-индекс. " : "");
                    } catch (Exception invalid) { indexStatus = "USB-индекс устарел или повреждён. "; }
                }
            }
            for (Object item : catalog) {
                Object song = call(item, "getSongDetail");
                Object key = call(song, "getSongOffsetKey");
                org.json.JSONObject entry = new org.json.JSONObject();
                entry.put("path", call(song, "getPath"));
                entry.put("index", call(key, "getIndex"));
                entry.put("key", call(key, "getKey"));
                diagnostic.put(entry);
                String index = (String) call(key, "getIndex"), songKey = (String) call(key, "getKey");
                String displayPath = (String) call(song, "getPath");
                if (folderMap != null) {
                    String mapped = folderMap.get(index, songKey);
                    if (mapped != null) displayPath = mapped;
                }
                FolderSelection.Track<Object> track = new FolderSelection.Track<>(index, songKey, displayPath, item);
                tracks.add(track);
                initial.put(track.index + ":" + track.key, item);
            }
            state.reset(); state.addPage(tracks, true);
            for (FolderSelection.Track<Object> track : tracks) if (Boolean.TRUE.equals(call(track.value, "getSelected"))) state.select(track, true);
            java.io.File directory = activity.getExternalFilesDir(null);
            if (directory != null) {
                try (java.io.FileOutputStream output = new java.io.FileOutputStream(new java.io.File(directory, "aqua-folder-catalog.json"))) {
                    output.write(diagnostic.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (java.io.IOException error) { android.util.Log.w("AquaFolders", "Diagnostic export unavailable", error); }
            }
            android.util.Log.i("AquaFolders", "Catalog size=" + tracks.size() + "; root folders=" + state.folders(""));
        }

        void render() {
            rows.clear(); List<String> labels = new ArrayList<>();
            final int firstVisible = list.getFirstVisiblePosition();
            final View firstChild = list.getChildAt(0);
            final int firstTop = firstChild == null ? 0 : firstChild.getTop();
            path.setText(selectedView ? "Порядок треков" : (folder.isEmpty() ? "/" : "/" + folder));
            if (ready) {
                if (!selectedView) for (String child : state.folders(folder)) {
                    rows.add(child); labels.add("▸ " + child.substring(child.lastIndexOf('/') + 1));
                }
                List<FolderSelection.Track<Object>> songs = selectedView ? state.selection() : state.songs(folder, false);
                for (FolderSelection.Track<Object> song : songs) { rows.add(song); labels.add(selectedView ? song.originalPath : song.name()); }
                status.setText(indexStatus + (state.folders("").isEmpty()
                    ? "В полученном каталоге нет путей папок. Выбрано: " + state.selection().size()
                    : "Выбрано: " + state.selection().size() + (rows.isEmpty() ? " · Нет треков" : "")));
            } else status.setText("Загрузка каталога…");
            list.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_multiple_choice, labels) {
                @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                    CheckedTextView row = (CheckedTextView) super.getView(position, null, parent);
                    row.setMaxLines(3);
                    row.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
                    if (rows.get(position) instanceof String) row.setCheckMarkDrawable(null);
                    return row;
                }
            });
            list.post(() -> {
                if (firstVisible >= 0 && firstVisible < list.getCount()) list.setSelectionFromTop(firstVisible, firstTop);
            });
            for (int i = 0; i < rows.size(); i++) list.setItemChecked(i, state.selection().contains(rows.get(i)));
            all.setEnabled(ready && !selectedView && !state.folders("").isEmpty());
            selected.setEnabled(ready);
            up.setEnabled(ready && (selectedView || !folder.isEmpty()));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ready && !state.selection().isEmpty());
        }

        void commit(boolean save) {
            if (!ready) return;
            try {
                if (Boolean.TRUE.equals(flow("getDeviceDisconnected"))) throw new IllegalStateException("Disconnected");
                // Apply removals before additions; additions follow the user's chosen order.
                for (FolderSelection.Track<Object> track : state.songs("", true)) {
                    if (Boolean.TRUE.equals(call(track.value, "getSelected")) && !state.selection().contains(track)) change(track, false);
                }
                for (FolderSelection.Track<Object> track : state.selection()) {
                    if (!Boolean.TRUE.equals(call(track.value, "getSelected"))) change(track, true);
                }
                // CREATE reads the state list directly, so retain the explicit folder-selection order.
                Object mutable = field(model, "_selectableSongsState").get(model);
                List<?> current = (List<?>) call(mutable, "getValue");
                List<Object> ordered = new ArrayList<>();
                for (FolderSelection.Track<Object> track : state.selection()) {
                    for (Object item : current) {
                        Object k = call(call(item, "getSongDetail"), "getSongOffsetKey");
                        if (track.index.equals(call(k, "getIndex")) && track.key.equals(call(k, "getKey"))) { ordered.add(item); break; }
                    }
                }
                for (Object item : current) if (!ordered.contains(item)) ordered.add(item);
                Method immutable = Class.forName("mie").getMethod("j", Iterable.class);
                call(mutable, "setValue", immutable.invoke(null, ordered));
                if (save) call(confirm, "invoke");
                dialog.dismiss();
            } catch (Exception e) { fail(activity, e); }
        }

        void change(FolderSelection.Track<Object> track, boolean desired) throws Exception {
            call(toggle, "invoke", call(call(track.value, "getSongDetail"), "getSongOffsetKey"), desired);
        }
    }
}
