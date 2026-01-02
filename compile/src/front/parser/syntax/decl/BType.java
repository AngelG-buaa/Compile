package front.parser.syntax.decl;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;

/**
 * 基本类型 BType → 'int'
 */
public class BType extends BranchNode {
    // 具体来说，这个基本符号到底是什么呀？为了保持扩展性，不仅仅支持int
    private TokenNode content;

    public BType() {
        super(SynType.BType);
    }

    public void appendChild(AstNode node) {
        super.appendChild(node);
        content = (TokenNode) node;
    }
    
    /**
     * 获取基本类型的类型信息
     * @return 类型的TokenNode，通常是"int"
     */
    public TokenNode getType() {
        return content;
    }
    
    /**
     * 获取类型名称字符串
     * @return 类型名称，如"int"
     */
    public String getTypeName() {
        return content != null ? content.getContent() : null;
    }
}
