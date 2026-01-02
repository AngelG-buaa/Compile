package front.parser.syntax.stmt;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.exp.Exp;

/**
 * Stmt → [Exp] ';'
 */
public class ExpStmt extends Stmt {
    private Exp exp;

    public ExpStmt() {
        super();
        exp = null;
    }

    @Override
    public void appendChild(AstNode child) {
        super.appendChild(child);
        if (child.getNodeType() == SynType.Exp) {
            exp = (Exp) child;
        }
    }

    public Exp getExp() {
        return exp;
    }
}
