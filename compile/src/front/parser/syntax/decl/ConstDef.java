package front.parser.syntax.decl;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.exp.ConstExp;

/**
 * ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
 */
public class ConstDef extends BranchNode {
    // Ident是终结符
    private TokenNode identifier;
    private ConstExp innerExp;
    private ConstInitVal initValue;

    public ConstDef() {
        super(SynType.ConstDef);
        this.identifier = null;
        this.innerExp = null;
        this.initValue = null;
    }

    public void appendChild(AstNode child) {
        super.appendChild(child);

        if (child.getNodeType() == SynType.ConstExp) {
            this.innerExp = (ConstExp) child;
        } else if (child.getNodeType() == SynType.IDENFR) {
            this.identifier = (TokenNode) child;
        } else if (child.getNodeType() == SynType.ConstInitVal) {
            this.initValue = (ConstInitVal) child;
        }
    }

    /**
     * 获取常量定义的标识符节点
     * @return 标识符的TokenNode，如果不存在则返回null
     */
    public TokenNode getIdentifier() {
        return identifier;
    }

    /**
     * 获取数组维度的常量表达式
     * @return 常量表达式节点，如果不是数组则返回null
     */
    public ConstExp getConstExp() {
        return innerExp;
    }

    /**
     * 获取常量初值
     * @return 常量初值节点，如果不存在则返回null
     */
    public ConstInitVal getInitValue() {
        return initValue;
    }

    /**
     * 判断是否为数组常量
     * @return 如果是数组常量返回true，否则返回false
     */
    public boolean isArray() {
        return innerExp != null;
    }

}
