import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import brut.androlib.smali.SmaliBuilder;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

public class AssembleDex {
    public static void main(String[] args) throws Exception {
        DexBuilder dex = new DexBuilder(new Opcodes(32));
        SmaliBuilder smali = new SmaliBuilder(32);
        try (var paths = Files.walk(Path.of(args[0]))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".smali")).sorted().toList()) {
                if (!smali.buildFile(path.toFile(), dex)) throw new IllegalStateException(path.toString());
            }
        }
        FileDataStore output = new FileDataStore(new File(args[1]));
        try { dex.writeTo(output); } finally { output.raf.close(); }
    }
}
