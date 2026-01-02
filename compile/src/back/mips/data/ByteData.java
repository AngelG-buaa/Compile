package back.mips.data;

import java.util.ArrayList;
import java.util.StringJoiner;

public class ByteData extends Data {
    private final ArrayList<Integer> values;

    public ByteData(String name, ArrayList<Integer> values) {
        super(name);
        this.values = (values != null) ? values : new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(identifier).append(": .byte ");
        
        StringJoiner joiner = new StringJoiner(", ");
        for (Integer val : values) {
            joiner.add(String.valueOf(val));
        }
        builder.append(joiner);
        builder.append("\n");
        return builder.toString();
    }
}
