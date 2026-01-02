package back.mips;

import middle.llvm.value.IRValue;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Note {
    private final IRValue content;

    public Note(IRValue content) {
        this.content = content;
    }

    @Override
    public String toString() {
        String raw = content.toString();
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        return Arrays.stream(raw.split("\n"))
                .map(line -> "# " + line)
                .collect(Collectors.joining("\n"));
    }
}
