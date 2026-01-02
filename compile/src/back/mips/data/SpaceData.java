package back.mips.data;

/**
 * buffer: .space 64  # 分配64字节空间，未初始化
 */
public class SpaceData extends Data {
    private final int size;

    public SpaceData(String name, int size) {
        super(name);
        this.size = size;
    }

    public int getByteNum() {
        return size;
    }

    @Override
    public String toString() {
        return identifier + " : .space " + size;
    }
}
