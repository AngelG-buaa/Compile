package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.exp.Cond;

/**
 * WhileStmt → 'while' '(' Cond ')' Stmt
 */
public class WhileStmt extends Stmt {
    private Cond cond;
    private Stmt stmt;

    public WhileStmt() {
        super();
    }

    @Override
    public void appendChild(AstNode child) {
        super.appendChild(child);
        if (child instanceof Cond) {
            cond = (Cond) child;
        } else if (child instanceof Stmt) {
            stmt = (Stmt) child;
        }
    }

    public Cond getCond() {
        return cond;
    }

    public Stmt getStmt() {
        return stmt;
    }
}
