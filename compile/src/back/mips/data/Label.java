package back.mips.data;

public class Label extends Data {
    public Label(String name) {
        super(name);
    }

    @Override
    public String toString() {
        return identifier + ":\n";
    }
}
