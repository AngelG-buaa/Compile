package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;


/**
 * LOrExp → LAndExp | LOrExp '||' LAndExp
 */
public class LOrExp extends BranchNode {
    private LOrExp lOrExp;
    private LAndExp lAndExp;

    public LOrExp() {
        super(SynType.LOrExp);
        this.lOrExp = null;
        this.lAndExp = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.LAndExp) {
            lAndExp = (LAndExp) child;
        } else if (child.getNodeType() == SynType.LOrExp) {
            lOrExp = (LOrExp) child;
        }
        super.appendChild(child);
    }

    /**
     * 获取LAndExp节点
     * @return LAndExp节点，如果不存在则返回null
     */
    public LAndExp getLAndExp() {
        return lAndExp;
    }

    /**
     * 获取LOrExp节点（用于递归结构）
     * @return LOrExp节点，如果不存在则返回null
     */
    public LOrExp getLOrExp() {
        return lOrExp;
    }
}
