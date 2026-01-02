package front.parser.syntax.stmt;

// TODO

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.exp.Exp;
import front.parser.syntax.exp.LVal;

import java.util.ArrayList;

/**
 * Stmt -> ForLoopStmt
 * ForLoopStmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
 * 1. 无缺省，1种情况
 * 2. ForStmt与 Cond中缺省一个，3种情况
 * 3. ForStmt与Cond中缺省两个，3种情况
 * 4. ForStmt与Cond全部缺省，1种情况
 *
 * ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
 */
public class ForStmt extends BranchNode {
    private ArrayList<LVal> lVals = new ArrayList<>();
    private ArrayList<Exp> exps = new ArrayList<>();
    private int lenth = 0;

    public ForStmt() {
        super(SynType.ForStmt);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.LVal) {
            lVals.add((LVal) child);
            lenth++;
        } else if (child.getNodeType() == SynType.Exp) {
            exps.add((Exp) child);
        }

        super.appendChild(child);
    }

    public ArrayList<LVal> getLVals() {
        return lVals;
    }

    public ArrayList<Exp> getExps() {
        return exps;
    }

    public int getLenth() {
        return lenth;
    }
}
