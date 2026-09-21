package local.suunto.music;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Use only entries whose device identity and raw filename still match. */
public final class UsbFolderMap {
    private final Map<String, String> folders = new HashMap<>();

    public UsbFolderMap(String json, List<String[]> catalog) throws Exception {
        JSONObject document = new JSONObject(json);
        if (document.getInt("version") != 1) throw new IllegalArgumentException("Index version");
        JSONArray entries = document.getJSONArray("entries");
        Map<String, String> candidates = new HashMap<>();
        Map<String, String> rawPaths = new HashMap<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            String id = id(entry.getString("index"), entry.getString("key"));
            if (rawPaths.put(id, entry.getString("rawPath")) != null) throw new IllegalArgumentException("Duplicate identity");
            if (!entry.isNull("folderPath")) {
                String path = entry.getString("folderPath");
                if ((!path.startsWith("MUSIC/") && !path.startsWith("SYSTEM/")) || path.contains("\\")) throw new IllegalArgumentException("Invalid folder path");
                for (String part : path.split("/", -1)) if (part.isEmpty() || part.equals("..") || part.equals(".")) throw new IllegalArgumentException("Invalid path segment");
                candidates.put(id, path);
            }
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String[] row : catalog) {
            String identity = id(row[0], row[1]);
            if (!seen.add(identity)) throw new IllegalArgumentException("Duplicate catalog identity");
            if (row[2].equals(rawPaths.get(identity)) && candidates.containsKey(identity)) folders.put(identity, candidates.get(identity));
        }
    }

    private static String id(String index, String key) { return index.length() + ":" + index + key; }
    public String get(String index, String key) { return folders.get(id(index, key)); }
    public int size() { return folders.size(); }
}
