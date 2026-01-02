package back.mips.data;

import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * 需要有初始化，对应int
 * x: .word 5  # 分配 4 字节内存，初始化为 5
 * array: .word 1, 2, 3, 4  # 分配 16 字节内存，分别初始化为 1, 2, 3, 4
 */
public class WordData extends Data {
    private final ArrayList<Integer> values;

    public WordData(String name, ArrayList<Integer> values) {
        super(name);
        this.values = (values != null) ? values : new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(identifier).append(" : .word ");
        
        StringJoiner joiner = new StringJoiner(", ");
        for (Integer val : values) {
            joiner.add(String.valueOf(val));
        }
        sb.append(joiner);
        
        return sb.toString();
    }
}
