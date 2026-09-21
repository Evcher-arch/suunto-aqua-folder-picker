import java.util.ArrayList;
import java.util.List;
public final class mie {
    public static List<Object> j(Iterable<?> items) {
        List<Object> result = new ArrayList<>(); for (Object item : items) result.add(item); return result;
    }
}
