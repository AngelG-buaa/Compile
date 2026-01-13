package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.exp.ConstExp;

public class CaseStmt extends Stmt {
    private ConstExp constExp; // Null for default
    private Stmt stmt;

    public CaseStmt() {
        super(SynType.CaseStmt);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.ConstExp) {
            this.constExp = (ConstExp) child;
        } else if (child instanceof Stmt) {
            this.stmt = (Stmt) child;
        }
        super.appendChild(child);
    }

    public ConstExp getConstExp() { return constExp; }
    public Stmt getStmt() { return stmt; }
    public boolean isDefault() { return constExp == null; }
}
