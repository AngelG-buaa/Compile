package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.exp.Cond;

public class DoWhileStmt extends Stmt {
    private Stmt stmt;
    private Cond cond;

    public DoWhileStmt() {
        super(SynType.DoWhileStmt);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child instanceof Stmt) {
            this.stmt = (Stmt) child;
        } else if (child.getNodeType() == SynType.Cond) {
            this.cond = (Cond) child;
        }
        super.appendChild(child);
    }

    public Stmt getStmt() { return stmt; }
    public Cond getCond() { return cond; }
}
