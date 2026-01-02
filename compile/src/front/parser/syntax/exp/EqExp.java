package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;


/**
 * EqExp → RelExp | EqExp ('==' | '!=') RelExp
 */
public class EqExp extends BranchNode {
    private TokenNode operator;
    private EqExp eqExp;
    private RelExp relExp;

    public EqExp() {
        super(SynType.EqExp);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.EQL
            || child.getNodeType() == SynType.NEQ) {
            this.operator = (TokenNode) child;
        } else if (child.getNodeType() == SynType.EqExp) {
            this.eqExp = (EqExp) child;
        } else if (child.getNodeType() == SynType.RelExp) {
            this.relExp = (RelExp) child;
        }
        super.appendChild(child);
    }

    /**
     * 获取操作符
     * @return 操作符TokenNode（==或!=），如果不存在则返回null
     */
    public TokenNode getOperator() {
        return operator;
    }

    /**
     * 获取EqExp节点（用于递归结构）
     * @return EqExp节点，如果不存在则返回null
     */
    public EqExp getEqExp() {
        return eqExp;
    }

    /**
     * 获取RelExp节点
     * @return RelExp节点，如果不存在则返回null
     */
    public RelExp getRelExp() {
        return relExp;
    }
}
