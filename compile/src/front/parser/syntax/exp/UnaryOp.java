package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;


/**
 * UnaryOp → '+' | '−' | '!'
 */
public class UnaryOp extends BranchNode {
    private TokenNode operator;

    public UnaryOp() {
        super(SynType.UnaryOp);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.PLUS
        || child.getNodeType() == SynType.MINU
        || child.getNodeType() == SynType.NOT) {
            this.operator = (TokenNode) child;
        }
        super.appendChild(child);
    }

    /**
     * 获取操作符
     * @return 操作符TokenNode（+、-或!），如果不存在则返回null
     */
    public TokenNode getOperator() {
        return operator;
    }
}
