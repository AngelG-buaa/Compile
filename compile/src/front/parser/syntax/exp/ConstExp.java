package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;


/**
 * ConstExp → AddExp
 */
public class ConstExp extends BranchNode {
    public AddExp addExp;

    public ConstExp() {
        super(SynType.ConstExp);
        addExp = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() != SynType.AddExp) {
            System.out.println("ConstExp with a wrong child");
        } else {
            this.addExp = (AddExp) child;
        }
        super.appendChild(child);
    }

    public AddExp getAddExp() {
        return addExp;
    }
}
