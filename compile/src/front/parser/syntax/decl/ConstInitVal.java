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
    private ArrayList<ConstExp> exps;

    public ConstInitVal() {
        super(SynType.ConstInitVal);
        this.exps = new ArrayList<>();
    }

    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.ConstExp) {
            this.exps.add((ConstExp) child);
        }
        super.appendChild(child);
    }

    public ArrayList<ConstExp> getExps() {
        return exps;
    }
}
