package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

/**
 * Exp → AddExp
 */
public class Exp extends BranchNode {
    private AddExp addExp;

    public Exp() {
        super(SynType.Exp);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() != SynType.AddExp) {
            System.out.println("Exp with a wrong child");
        } else {
            addExp = (AddExp) child;
        }
        super.appendChild(child);
    }
    
    /**
     * 获取表达式中的AddExp节点
     * @return AddExp节点，如果不存在则返回null
     */
    public AddExp getAddExp() {
        return addExp;
    }
}
