package back.mips.data;

/**
 * .str: .asciiz "This is a string\n"
 */
public class AsciiData extends Data {
    private final String content;

    public AsciiData(String name, String content) {
        super(name);
        this.content = content;
    }

    @Override
    public String toString() {
        return identifier + " : .asciiz \"" + content + "\"";
    }
}
