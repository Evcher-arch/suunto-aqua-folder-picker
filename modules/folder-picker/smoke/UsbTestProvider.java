package local.suunto.music;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONObject;

/** Deterministic SAF provider, packaged only in the emulator test APK. */
public final class UsbTestProvider extends DocumentsProvider {
    static boolean disconnected;
    @Override public boolean onCreate() { return true; }
    @Override public Cursor queryRoots(String[] projection) { return new MatrixCursor(new String[]{"root_id"}); }
    static MatrixCursor cursor() { return new MatrixCursor(new String[]{"document_id", "_display_name", "mime_type"}); }
    static void dir(MatrixCursor c, String id, String name) { c.addRow(new Object[]{id, name, DocumentsContract.Document.MIME_TYPE_DIR}); }
    static void file(MatrixCursor c, String id, String name) { c.addRow(new Object[]{id, name, "audio/mpeg"}); }
    @Override public Cursor queryDocument(String id, String[] projection) {
        MatrixCursor c = new MatrixCursor(projection);
        c.addRow(new Object[]{id.equals("music") ? "MUSIC" : "Aqua", DocumentsContract.Document.MIME_TYPE_DIR});
        return c;
    }
    @Override public Cursor queryChildDocuments(String id, String[] projection, String order) throws FileNotFoundException {
        if (disconnected) throw new FileNotFoundException("USB disconnected");
        MatrixCursor c = cursor();
        if (id.equals("root")) { dir(c, "music", "MUSIC"); dir(c, "system", "SYSTEM"); }
        if (id.equals("music")) { dir(c, "year", "2025"); dir(c, "other", "2026"); }
        if (id.equals("year")) { file(c, "a", "A.mp3"); file(c, "d1", "dup.mp3"); dir(c, "live", "Live"); }
        if (id.equals("other")) file(c, "d2", "dup.mp3");
        if (id.equals("live")) file(c, "b", "Cafe\u0301.mp3");
        if (id.equals("system")) file(c, "s", "system.wav");
        return c;
    }
    @Override public ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal) throws FileNotFoundException {
        throw new FileNotFoundException("No audio content may be opened by indexer");
    }
    static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); }
    public static void run(Context context) throws Exception {
        JSONArray catalog = new JSONArray();
        String[] names = {"a.mp3", "Caf\u00e9.mp3", "dup.mp3", "system.wav", "missing.mp3"};
        for (int i = 0; i < names.length; i++) catalog.put(new JSONObject().put("index", "" + i).put("key", "k" + i).put("path", names[i]));
        File base = context.getExternalFilesDir(null);
        File catalogFile = new File(base, "aqua-folder-catalog.json"), mapFile = new File(base, "aqua-folder-map.json");
        Files.write(catalogFile.toPath(), catalog.toString().getBytes(StandardCharsets.UTF_8));
        Uri tree = DocumentsContract.buildTreeDocumentUri("local.suunto.folders.test.usb", "root");
        UsbFolderUpdate.update(context, tree);
        byte[] saved = Files.readAllBytes(mapFile.toPath());
        JSONArray entries = new JSONObject(new String(saved, StandardCharsets.UTF_8)).getJSONArray("entries");
        check(entries.getJSONObject(0).getString("folderPath").equals("MUSIC/2025/A.mp3"), "Case matching");
        check(entries.getJSONObject(1).getString("folderPath").startsWith("MUSIC/2025/Live/"), "Nested Unicode");
        check(entries.getJSONObject(2).isNull("folderPath"), "Duplicate USB filename");
        check(entries.getJSONObject(3).getString("folderPath").equals("SYSTEM/system.wav"), "System root");
        check(entries.getJSONObject(4).isNull("folderPath"), "Missing file");
        disconnected = true;
        try { UsbFolderUpdate.update(context, tree); throw new AssertionError("Disconnect accepted"); }
        catch (java.io.IOException expected) { }
        finally { disconnected = false; }
        check(java.util.Arrays.equals(saved, Files.readAllBytes(mapFile.toPath())), "Disconnect changed index");
        try { UsbFolderUpdate.update(context, DocumentsContract.buildTreeDocumentUri("local.suunto.folders.test.usb", "wrong")); throw new AssertionError("Wrong root accepted"); }
        catch (java.io.IOException expected) { }
        check(java.util.Arrays.equals(saved, Files.readAllBytes(mapFile.toPath())), "Wrong root changed index");
        JSONArray duplicates = new JSONArray().put(catalog.getJSONObject(0)).put(new JSONObject().put("index", "9").put("key", "9").put("path", "a.mp3"));
        JSONObject duplicateMap = UsbFolderIndex.build(duplicates, java.util.Collections.singletonList("MUSIC/2025/a.mp3"));
        check(duplicateMap.getJSONArray("entries").getJSONObject(0).isNull("folderPath"), "Duplicate catalog filename");
        UsbFolderUpdate.update(context, DocumentsContract.buildTreeDocumentUri("local.suunto.folders.test.usb", "music"));
        check(new JSONObject(new String(Files.readAllBytes(mapFile.toPath()), StandardCharsets.UTF_8)).getJSONArray("entries")
            .getJSONObject(0).getString("folderPath").equals("MUSIC/2025/A.mp3"), "MUSIC selection");
        android.util.Log.i("AquaSmoke", "USB_SAF_TEST_PASS: nested, Unicode, duplicates, missing, disconnect rollback, wrong root, MUSIC root");
        // Leave no mapping that could alter the separate folder-selection fixture.
        Files.deleteIfExists(mapFile.toPath());
    }
}
