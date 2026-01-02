package front.parser.syntax.stmt;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.exp.Cond;

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
public class ForLoopStmt extends Stmt {
    private ForStmt forStmt_1 = null;
    private ForStmt forStmt_2 = null;
    private Cond cond = null;
    private Stmt stmt = null;

    private int index = 1;

    @Override
    public void appendChild(AstNode child) {
        super.appendChild(child);
        if (child.getNodeType() == SynType.SEMICN) {
            index ++;
        } else if (child.getNodeType() == SynType.ForStmt) {
            if (index == 1) {
                forStmt_1 = (ForStmt) child;
            } else if (index == 3) {
                forStmt_2 = (ForStmt) child;
            } else {
                System.out.println("Strange ForLoopStmt");
            }
        } else if (child.getNodeType() == SynType.Cond) {
            cond = (Cond) child;
        } else if (child.getNodeType() == SynType.Stmt) {
            stmt = (Stmt) child;
        }
    }

    public ForStmt getInitStmt() {
        return forStmt_1;
    }

    public ForStmt getChangeStmt() {
        return forStmt_2;
    }

    public Cond getCond() {
        return cond;
    }

    public Stmt getBody() {
        return stmt;
    }
}
