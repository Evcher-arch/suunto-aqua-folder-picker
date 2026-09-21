package local.suunto.music;

import java.util.Arrays;
import java.util.Collections;

public final class FolderSelectionTest {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }

    public static void main(String[] args) {
        FolderSelection<String> state = new FolderSelection<>();
        FolderSelection.Track<String> ten = new FolderSelection.Track<>("0a", "a", "/MUSIC/A/10.mp3", "ten");
        FolderSelection.Track<String> two = new FolderSelection.Track<>("02", "b", "/MUSIC/A/2.mp3", "two");
        FolderSelection.Track<String> other = new FolderSelection.Track<>("03", "c", "/MUSIC/B/2.mp3", "other");
        FolderSelection.Track<String> nested = new FolderSelection.Track<>("04", "d", "MUSIC\\A\\Live\\1.mp3", "nested");
        state.addPage(Arrays.asList(ten, two), false);
        try { state.selectFolder("MUSIC/A", false); throw new AssertionError(); }
        catch (IllegalStateException expected) { }
        state.select(two, true);
        state.addPage(Arrays.asList(other, nested), true);
        check(state.folders("MUSIC").equals(Arrays.asList("MUSIC/A", "MUSIC/B")));
        check(state.songs("MUSIC/A", false).size() == 2);
        check(state.songs("MUSIC/A", true).size() == 3);
        check(state.songs("MUSIC/A", false).get(0) == two);
        state.selectFolder("MUSIC/A", false);
        state.selectFolder("MUSIC/B", false);
        check(state.selection().size() == 3);
        check(state.selection().get(0) == two);
        check(state.selection().get(2) == other);
        state.move(2, 0);
        check(state.selection().get(0).value.equals("other"));
        check(state.selection().get(1).originalPath.equals("/MUSIC/A/2.mp3"));
        state.select(two, false);
        state.select(two, true);
        check(state.selection().get(2) == two);
        check(FolderSelection.naturalCompare("Track2", "Track10") < 0);
        check(FolderSelection.naturalCompare("99999999999999999999", "100000000000000000000") < 0);
        check(FolderSelection.naturalCompare("A", "a") != 0);
        try { new FolderSelection.Track<>("x", "y", "../a.mp3", "bad"); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        state.reset();
        check(state.selection().isEmpty() && !state.isComplete());
        state.addPage(Collections.emptyList(), true);
        check(state.folders("").isEmpty());
        state.reset();
        state.addPage(Arrays.asList(new FolderSelection.Track<>("1", "a", "2.mp3", "bare"),
            new FolderSelection.Track<>("2", "b", "10.mp3", "bare2")), true);
        check(state.folders("").isEmpty());
        check(state.songs("", false).size() == 2);
        System.out.println("PASS: folders, pagination, stable identity, selection order, sorting, reset");
    }
}
