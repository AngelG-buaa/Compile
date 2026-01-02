package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;


/**
 * RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
 */
public class RelExp extends BranchNode {
    private AddExp addExp;
    private TokenNode operator;
    private RelExp relExp;

    public RelExp() {
        super(SynType.RelExp);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.AddExp) {
            this.addExp = (AddExp) child;
        } else if (child.getNodeType() == SynType.RelExp) {
            this.relExp = (RelExp) child;
        } else if (child.getNodeType() == SynType.GEQ
                || child.getNodeType() == SynType.LEQ
                || child.getNodeType() == SynType.GRE
                || child.getNodeType() == SynType.LSS) {
            this.operator = (TokenNode) child;
        }
        super.appendChild(child);
    }

    /**
     * 获取AddExp节点
     * @return AddExp节点，如果不存在则返回null
     */
    public AddExp getAddExp() {
        return addExp;
    }

    /**
     * 获取操作符
     * @return 操作符TokenNode（<、>、<=或>=），如果不存在则返回null
     */
    public TokenNode getOperator() {
        return operator;
    }

    /**
     * 获取RelExp节点（用于递归结构）
     * @return RelExp节点，如果不存在则返回null
     */
    public RelExp getRelExp() {
        return relExp;
    }
}
