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
    private ArrayList<Exp> exps;

    public InitVal() {
        super(SynType.InitVal);
        exps = new ArrayList<>();
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.Exp) {
            exps.add((Exp) child);
        }
        super.appendChild(child);
    }

    public ArrayList<Exp> getExps() {
        return exps;
    }
}
