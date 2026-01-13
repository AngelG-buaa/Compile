package front.parser.syntax.decl;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.exp.Exp;

import java.util.ArrayList;
import java.util.List;


/**
 * InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
 * 1.表达式初值 2.一维数组初值
 * 我这里的做法支持多维数组
 */
public class InitVal extends BranchNode {
    private Exp exp;
    private ArrayList<InitVal> initVals;

    public InitVal() {
        super(SynType.InitVal);
        this.initVals = new ArrayList<>();
        this.exp = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.Exp) {
            this.exp = (Exp) child;
        } else if (child.getNodeType() == SynType.InitVal) {
            this.initVals.add((InitVal) child);
        }
        super.appendChild(child);
    }

    public Exp getExp() {
        return exp;
    }
    
    public ArrayList<InitVal> getInitVals() {
        return initVals;
    }
    
    public boolean isLeaf() {
        return exp != null;
    }
}
