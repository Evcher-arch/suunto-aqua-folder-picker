package local.suunto.music;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Correlates filenames conservatively; device identities are never rewritten. */
public final class UsbFolderIndex {
    private static String name(String path) {
        String normalized = path.replace('\\', '/');
        return Normalizer.normalize(normalized.substring(normalized.lastIndexOf('/') + 1),
            Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    public static JSONObject build(JSONArray catalog, List<String> paths) throws Exception {
        Map<String, List<String>> files = new HashMap<>();
        for (String path : paths) {
            if (!path.startsWith("MUSIC/") && !path.startsWith("SYSTEM/")) throw new IllegalArgumentException("Root");
            for (String part : path.split("/", -1)) {
                if (part.isEmpty() || part.equals(".") || part.equals("..") || part.contains("\\"))
                    throw new IllegalArgumentException("Path");
            }
            String key = name(path);
            if (!files.containsKey(key)) files.put(key, new ArrayList<>());
            files.get(key).add(path);
        }
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < catalog.length(); i++) {
            String key = name(catalog.getJSONObject(i).getString("path"));
            counts.put(key, counts.containsKey(key) ? counts.get(key) + 1 : 1);
        }
        JSONArray entries = new JSONArray();
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject song = catalog.getJSONObject(i);
            String raw = song.getString("path"), key = name(raw);
            List<String> matches = files.get(key);
            boolean matched = matches != null && matches.size() == 1 && counts.get(key) == 1;
            entries.put(new JSONObject().put("index", song.getString("index")).put("key", song.getString("key"))
                .put("rawPath", raw).put("folderPath", matched ? matches.get(0) : JSONObject.NULL)
                .put("status", matched ? "matched" : matches == null ? "missing" : "ambiguous"));
        }
        return new JSONObject().put("version", 1).put("source", "Android USB directory snapshot").put("entries", entries);
    }
}
