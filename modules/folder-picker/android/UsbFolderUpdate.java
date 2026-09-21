package local.suunto.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** SAF result owner avoids changes to the original Activity's result dispatch. */
public final class UsbFolderUpdate extends Fragment {
    public static final String LABEL = "Обновить список папок по USB";
    private static final String TAG = "AquaUsbFolderUpdate";
    private static final int PICK_TREE = 7351;
    private boolean busy;
    private String result;
    private AlertDialog progress;

    public static void open(Activity activity) {
        UsbFolderUpdate fragment = (UsbFolderUpdate) activity.getFragmentManager().findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new UsbFolderUpdate();
            activity.getFragmentManager().beginTransaction().add(fragment, TAG).commit();
            activity.getFragmentManager().executePendingTransactions();
        }
        fragment.choose();
    }

    @Override public void onCreate(Bundle state) { super.onCreate(state); setRetainInstance(true); }

    private void choose() {
        if (busy) return;
        File root = getActivity().getExternalFilesDir(null);
        if (root == null || (!new File(root, "aqua-folder-catalog.json").isFile()
                && !new File(root, "aqua-folder-map.json").isFile())) {
            result = "Сначала откройте «Папки наушников» хотя бы один раз, чтобы приложение получило список треков.";
            showResult(); return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try { startActivityForResult(intent, PICK_TREE); }
        catch (Exception error) { result = "Не удалось открыть выбор USB-накопителя."; showResult(); }
    }

    @Override public void onActivityResult(int request, int code, Intent data) {
        if (request != PICK_TREE || code != Activity.RESULT_OK || data == null || data.getData() == null || busy) return;
        final Context context = getActivity().getApplicationContext();
        final Uri tree = data.getData();
        busy = true; showProgress();
        new Thread(() -> {
            String message;
            try { message = update(context, tree); }
            catch (Exception error) {
                android.util.Log.w(TAG, "USB index unchanged", error);
                message = "Список папок не изменён. Выберите папку MUSIC или корень накопителя Aqua и не отключайте USB во время чтения.";
            }
            final String completed = message;
            new Handler(Looper.getMainLooper()).post(() -> {
                busy = false; result = completed;
                if (progress != null) { progress.dismiss(); progress = null; }
                showResult();
            });
        }, "AquaUsbIndex").start();
    }

    @Override public void onResume() { super.onResume(); if (busy) showProgress(); else showResult(); }
    @Override public void onDetach() {
        if (progress != null) { progress.dismiss(); progress = null; }
        super.onDetach();
    }
    private void showProgress() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || progress != null) return;
        progress = new AlertDialog.Builder(activity).setTitle(LABEL).setMessage("Чтение папок USB-накопителя…")
            .setCancelable(false).show();
    }
    private void showResult() {
        Activity activity = getActivity();
        if (result == null || activity == null || activity.isFinishing() || !isResumed()) return;
        new AlertDialog.Builder(activity).setTitle(LABEL).setMessage(result).setPositiveButton("OK", null).show();
        result = null;
    }

    static String update(Context context, Uri tree) throws Exception {
        File directory = context.getExternalFilesDir(null);
        if (directory == null) throw new IOException("App storage unavailable");
        File catalogFile = new File(directory, "aqua-folder-catalog.json");
        JSONArray catalog = loadCatalog(directory);
        if (catalog.length() == 0) throw new IOException("Empty catalog");
        String catalogSnapshot = catalog.toString();
        Scan scan = new Scan(context, tree);
        String id = DocumentsContract.getTreeDocumentId(tree);
        Uri selected = DocumentsContract.buildDocumentUriUsingTree(tree, id);
        String display;
        try (Cursor cursor = context.getContentResolver().query(selected,
                new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst() || !DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(1)))
                throw new IOException("Not a directory");
            display = cursor.getString(0);
        }
        if ("MUSIC".equalsIgnoreCase(display)) scan.walk(id, "MUSIC", 0);
        else scan.root(id);
        JSONObject map = UsbFolderIndex.build(catalog, scan.paths);
        JSONArray entries = map.getJSONArray("entries");
        int matched = 0;
        for (int i = 0; i < entries.length(); i++) if ("matched".equals(entries.getJSONObject(i).getString("status"))) matched++;
        if (matched == 0) throw new IOException("No unique matches");
        List<String[]> identities = new ArrayList<>();
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject song = catalog.getJSONObject(i);
            identities.add(new String[]{song.getString("index"), song.getString("key"), song.getString("path")});
        }
        new UsbFolderMap(map.toString(), identities);
        if (!catalogSnapshot.equals(loadCatalog(directory).toString())) throw new IOException("Catalog changed during scan");
        AtomicFile target = new AtomicFile(new File(directory, "aqua-folder-map.json"));
        FileOutputStream output = null;
        try {
            output = target.startWrite();
            output.write(map.toString(2).getBytes(StandardCharsets.UTF_8));
            target.finishWrite(output);
        } catch (IOException error) { if (output != null) target.failWrite(output); throw error; }
        return "Список папок обновлён: " + matched + " из " + catalog.length() + " треков. "
            + (matched < catalog.length() ? "Несопоставленные треки останутся без папок. " : "")
            + "Отключите USB, подключите наушники по Bluetooth и заново откройте «Папки наушников».";
    }

    private static JSONArray loadCatalog(File directory) throws Exception {
        File catalogFile = new File(directory, "aqua-folder-catalog.json");
        if (catalogFile.isFile()) {
            return new JSONArray(new String(Files.readAllBytes(catalogFile.toPath()), StandardCharsets.UTF_8));
        }
        File mapFile = new File(directory, "aqua-folder-map.json");
        JSONObject map = new JSONObject(new String(Files.readAllBytes(mapFile.toPath()), StandardCharsets.UTF_8));
        JSONArray catalog = new JSONArray();
        JSONArray entries = map.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            catalog.put(new JSONObject().put("index", entry.getString("index"))
                .put("key", entry.getString("key")).put("path", entry.getString("rawPath")));
        }
        return catalog;
    }

    private static final class Scan {
        final Context context;
        final Uri tree;
        final List<String> paths = new ArrayList<>();
        final Set<String> visited = new HashSet<>();
        int nodes;
        Scan(Context context, Uri tree) { this.context = context; this.tree = tree; }
        Cursor children(String id) throws IOException {
            Cursor cursor = context.getContentResolver().query(DocumentsContract.buildChildDocumentsUriUsingTree(tree, id),
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
            if (cursor == null) throw new IOException("USB directory unavailable");
            Bundle extras = cursor.getExtras();
            if (extras != null && (extras.getBoolean(DocumentsContract.EXTRA_LOADING, false)
                    || extras.containsKey(DocumentsContract.EXTRA_ERROR))) {
                cursor.close(); throw new IOException("Incomplete provider listing");
            }
            return cursor;
        }
        void root(String id) throws Exception {
            boolean music = false;
            try (Cursor cursor = children(id)) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(1);
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))
                            && ("MUSIC".equalsIgnoreCase(name) || "SYSTEM".equalsIgnoreCase(name))) {
                        if ("MUSIC".equalsIgnoreCase(name)) music = true;
                        walk(cursor.getString(0), name.toUpperCase(java.util.Locale.ROOT), 0);
                    }
                }
            }
            if (!music) throw new IOException("MUSIC not found");
        }
        void walk(String id, String path, int depth) throws Exception {
            if (depth > 64 || !visited.add(id)) throw new IOException("Directory cycle or depth limit");
            try (Cursor cursor = children(id)) {
                while (cursor.moveToNext()) {
                    if (++nodes > 100000) throw new IOException("Directory limit");
                    String name = cursor.getString(1);
                    if (name == null || name.isEmpty() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\"))
                        throw new IOException("Invalid filename");
                    String child = path + "/" + name;
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))) walk(cursor.getString(0), child, depth + 1);
                    else paths.add(child);
                }
            }
        }
    }
}
