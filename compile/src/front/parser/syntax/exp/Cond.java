package front.parser.syntax.exp;


import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

/**
 * Cond → LOrExp
 */
public class Cond extends BranchNode {
    private LOrExp lorexp;

    public Cond() {
        super(SynType.Cond);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() != SynType.LOrExp) {
            System.out.println("Cond with a wrong child");
        } else {
            lorexp = (LOrExp) child;
        }
        super.appendChild(child);
    }

    /**
     * 获取LOrExp节点
     * @return LOrExp节点，如果不存在则返回null
     */
    public LOrExp getLOrExp() {
        return lorexp;
    }
}
