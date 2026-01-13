package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.exp.Exp;

public class SwitchStmt extends Stmt {
    private Exp exp;
    private Stmt stmt;

    public SwitchStmt() {
        super(SynType.SwitchStmt);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.Exp) {
            this.exp = (Exp) child;
        } else if (child instanceof Stmt) {
            this.stmt = (Stmt) child;
        }
        super.appendChild(child);
    }

    public Exp getExp() { return exp; }
    public Stmt getStmt() { return stmt; }
}
