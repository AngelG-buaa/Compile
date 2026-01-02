package front.parser.syntax;

/**
 * 代表语法树中的一个“叶子”节点（终结符）。
 * 它通常对应源代码中的一个词法单元（Token）。
 */
public class TokenNode extends AstNode {
    // 设为 private final 保证不可变性
    private final String content;
    private final int lineNumber;

    public TokenNode(SynType type, String content, int lineNumber) {
        super(type);
        this.content = content;
        this.lineNumber = lineNumber;
    }

    public String getContent() {
        return content;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public SynType getTokenType() {
        return super.getNodeType();
    }

    /**
     * 生成终结符节点的字符串表示。
     */
    @Override
    public String toString() {
        return String.format("%s %s",
                super.getNodeType(),
                this.content
                // this.lineNumber
        );
    }
}