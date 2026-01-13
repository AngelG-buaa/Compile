package front.parser.syntax.decl;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.exp.ConstExp;

import java.util.ArrayList;
import java.util.List;

/**
 * ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
 * 1.常表达式初值
 * 2.一维数组初值
 */
public class ConstInitVal extends BranchNode {
    private ConstExp constExp;
    private ArrayList<ConstInitVal> initVals;

    public ConstInitVal() {
        super(SynType.ConstInitVal);
        this.initVals = new ArrayList<>();
        this.constExp = null;
    }

    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.ConstExp) {
            this.constExp = (ConstExp) child;
        } else if (child.getNodeType() == SynType.ConstInitVal) {
            this.initVals.add((ConstInitVal) child);
        }
        super.appendChild(child);
    }

    public ConstExp getConstExp() {
        return constExp;
    }

    public ArrayList<ConstInitVal> getInitVals() {
        return initVals;
    }
    
    public boolean isLeaf() {
        return constExp != null;
    }
}
