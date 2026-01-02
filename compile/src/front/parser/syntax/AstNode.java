package front.parser.syntax;

/**
 * 抽象语法树所有节点的基类。
 */
public abstract class AstNode {
    // 将类型设为受保护的，只能在子类中访问
    protected final SynType nodeType;

    public AstNode(SynType type) {
        this.nodeType = type;
    }

    public SynType getNodeType() {
        return nodeType;
    }

    /**
     * 为了方便调试，所有子类都应该提供一个有意义的字符串表示。
     */
    @Override
    public abstract String toString();

    /**
     * 提供一个通用的遍历接口，子类可以重写以实现特定逻辑。
     * @param indent 用于格式化输出的缩进
     */
    public void prettyPrint(String indent) {
        System.out.println(indent + this.toString());
    }
}