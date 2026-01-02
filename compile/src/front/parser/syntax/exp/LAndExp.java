package front.parser.syntax.exp;


import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

/**
 * LAndExp → EqExp | LAndExp '&&' EqExp
 */
public class LAndExp extends BranchNode {
    private EqExp eqExp;
    private LAndExp lAndExp;

    public LAndExp() {
        super(SynType.LAndExp);
        this.eqExp = null;
        this.lAndExp = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.LAndExp) {
            lAndExp = (LAndExp) child;
        } else if (child.getNodeType() == SynType.EqExp) {
            eqExp = (EqExp) child;
        }
        super.appendChild(child);
    }

    /**
     * 获取EqExp节点
     * @return EqExp节点，如果不存在则返回null
     */
    public EqExp getEqExp() {
        return eqExp;
    }

    /**
     * 获取LAndExp节点（用于递归结构）
     * @return LAndExp节点，如果不存在则返回null
     */
    public LAndExp getLAndExp() {
        return lAndExp;
    }
}
