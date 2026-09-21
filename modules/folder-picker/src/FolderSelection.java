package local.suunto.music;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/** Pure selection state. The adapter must retain the original SongOffsetKey and SongDetail. */
public final class FolderSelection<T> {
    public static final class Track<T> {
        public final String index;
        public final String key;
        public final String originalPath;
        public final T value;
        private final String path;

        public Track(String index, String key, String path, T value) {
            this.index = Objects.requireNonNull(index);
            this.key = Objects.requireNonNull(key);
            if (index.isEmpty() || key.isEmpty()) throw new IllegalArgumentException("Missing device identity");
            this.originalPath = Objects.requireNonNull(path);
            this.path = normalize(path);
            if (this.path.isEmpty() || path.endsWith("/") || path.endsWith("\\")) {
                throw new IllegalArgumentException("Missing filename");
            }
            this.value = Objects.requireNonNull(value);
        }

        private String id() { return index.length() + ":" + index + key; }
        public String name() { return path.substring(path.lastIndexOf('/') + 1); }
        public String folder() {
            int slash = path.lastIndexOf('/');
            return slash < 0 ? "" : path.substring(0, slash);
        }
    }

    private final Map<String, Track<T>> tracks = new LinkedHashMap<>();
    private final Map<String, Track<T>> selected = new LinkedHashMap<>();
    private boolean complete;

    /** Append pages from one device catalog snapshot; never mix snapshots. */
    public void addPage(Collection<Track<T>> page, boolean lastPage) {
        if (complete) throw new IllegalStateException("Catalog already complete");
        for (Track<T> track : page) Objects.requireNonNull(track);
        for (Track<T> track : page) {
            tracks.put(track.id(), track);
            if (selected.containsKey(track.id())) selected.put(track.id(), track);
        }
        complete = lastPage;
    }

    public boolean isComplete() { return complete; }

    public List<String> folders(String parent) {
        String base = normalize(parent);
        String prefix = base.isEmpty() ? "" : base + "/";
        TreeSet<String> result = new TreeSet<>(FolderSelection::naturalCompare);
        for (Track<T> track : tracks.values()) {
            if (!track.path.startsWith(prefix)) continue;
            String relative = track.path.substring(prefix.length());
            int slash = relative.indexOf('/');
            if (slash >= 0) result.add(prefix + relative.substring(0, slash));
        }
        return new ArrayList<>(result);
    }

    public List<Track<T>> songs(String folder, boolean recursive) {
        String base = normalize(folder);
        String prefix = base.isEmpty() ? "" : base + "/";
        List<Track<T>> result = new ArrayList<>();
        for (Track<T> track : tracks.values()) {
            if (track.folder().equals(base) || (recursive && track.path.startsWith(prefix))) result.add(track);
        }
        result.sort(Comparator.<Track<T>, String>comparing(t -> t.path, FolderSelection::naturalCompare)
            .thenComparing(t -> t.index).thenComparing(t -> t.key));
        return result;
    }

    public void select(Track<T> track, boolean enabled) {
        Track<T> current = tracks.get(track.id());
        if (current == null) throw new IllegalArgumentException("Track is outside this catalog");
        if (enabled) selected.putIfAbsent(current.id(), current);
        else selected.remove(current.id());
    }

    public void selectFolder(String folder, boolean recursive) {
        if (!complete) throw new IllegalStateException("Load all catalog pages before selecting a folder");
        for (Track<T> track : songs(folder, recursive)) select(track, true);
    }

    public List<Track<T>> selection() {
        return Collections.unmodifiableList(new ArrayList<>(selected.values()));
    }

    public void move(int from, int to) {
        List<Track<T>> order = new ArrayList<>(selected.values());
        if (from < 0 || to < 0 || from >= order.size() || to >= order.size()) throw new IndexOutOfBoundsException();
        order.add(to, order.remove(from));
        selected.clear();
        for (Track<T> track : order) selected.put(track.id(), track);
    }

    /** Invalidate both catalog and selection when the device or its song index changes. */
    public void reset() { tracks.clear(); selected.clear(); complete = false; }

    private static String normalize(String path) {
        List<String> parts = new ArrayList<>();
        for (String part : Objects.requireNonNull(path).replace('\\', '/').split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) throw new IllegalArgumentException("Parent traversal in device path");
            parts.add(part);
        }
        return String.join("/", parts);
    }

    // Compare numeric runs without converting them to bounded integers.
    static int naturalCompare(String left, String right) {
        int i = 0, j = 0;
        while (i < left.length() && j < right.length()) {
            char a = left.charAt(i), b = right.charAt(j);
            if (a >= '0' && a <= '9' && b >= '0' && b <= '9') {
                int endA = i, endB = j;
                while (endA < left.length() && left.charAt(endA) >= '0' && left.charAt(endA) <= '9') endA++;
                while (endB < right.length() && right.charAt(endB) >= '0' && right.charAt(endB) <= '9') endB++;
                int startA = i, startB = j;
                while (startA < endA - 1 && left.charAt(startA) == '0') startA++;
                while (startB < endB - 1 && right.charAt(startB) == '0') startB++;
                int cmp = Integer.compare(endA - startA, endB - startB);
                if (cmp == 0) cmp = left.substring(startA, endA).compareTo(right.substring(startB, endB));
                if (cmp != 0) return cmp;
                i = endA; j = endB;
            } else {
                int cmp = Character.compare(Character.toLowerCase(a), Character.toLowerCase(b));
                if (cmp != 0) return cmp;
                i++; j++;
            }
        }
        int cmp = Integer.compare(left.length() - i, right.length() - j);
        return cmp != 0 ? cmp : left.compareTo(right);
    }
}
