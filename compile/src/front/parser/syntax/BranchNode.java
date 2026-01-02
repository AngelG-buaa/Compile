package front.parser.syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * 非终结符的子类：
 *  属性：形式定义中出现的所有不是符号的未定东西
 *      （是终结符的话用TokenNode，否则用具体的Node类）
 *  以自身为根节点的子树：形式定义中出现的所有不是符号的未定东西
 *
 *  设置了两种表示方法，普通的AST为遍历输出提供方便;
 *  在每个非终结符节点记录他的子节点的情况，方便转化成中间表达。
 */
public class BranchNode extends AstNode {
    // 使用 private 来封装子节点列表
    private final List<AstNode> children;

    public BranchNode(SynType type) {
        super(type);
        this.children = new ArrayList<>();
    }

    /**
     * 添加一个子节点到当前节点。
     * @param child 要添加的子节点。
     */
    public void appendChild(AstNode child) {
        if (child != null) {
            this.children.add(child);
        } else {
            System.out.println("Null child");
        }
    }

    public List<AstNode> getChildren() {
        return new ArrayList<>(children); // 返回一个副本以防止外部修改
    }

    /**
     * 用于调试：生成当前节点的字符串表示，不包含子节点。
     */
    public String toPrettyFormat() {
        return String.format("<%s>", super.getNodeType());
    }

    /**
     * 递归地、带缩进地打印整个子树，方便调试。
     * @param indent 缩进字符串
     */
    @Override
    public void prettyPrint(String indent) {
        System.out.println(indent + this.toPrettyFormat());
        for (AstNode child : children) {
            child.prettyPrint(indent + "\t"); // 子节点增加缩进
        }
    }

    /**
     * 答案输出
     */
    public String toString() {
        StringBuilder resultBuilder = new StringBuilder();
        SynType currentType = super.getNodeType();
        boolean shouldFormat = !isSpecialType(currentType);

        appendChildrenText(resultBuilder, currentType, shouldFormat);

        if (shouldFormat) {
            appendTypeTag(resultBuilder, currentType);
        }

        return resultBuilder.toString();
    }

    private boolean isSpecialType(SynType type) {
        return     type == SynType.BlockItem
                || type == SynType.Decl
                || type == SynType.BType;
    }

    private void appendChildrenText(StringBuilder builder, SynType currentType, boolean shouldFormat) {
        for (AstNode child : children) {
            builder.append(child.toString());
            if (shouldFormat) {
                builder.append('\n');
            }
        }
    }

    private void appendTypeTag(StringBuilder builder, SynType type) {
        builder.append('<')
                .append(type)
                .append('>');
    }
}