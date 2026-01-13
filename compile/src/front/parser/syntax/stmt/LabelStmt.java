package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;

public class LabelStmt extends Stmt {
    private TokenNode identifier;
    private Stmt stmt;

    public LabelStmt() {
        super(SynType.LabelStmt);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.IDENFR) {
            this.identifier = (TokenNode) child;
        } else if (child instanceof Stmt) {
            this.stmt = (Stmt) child;
        }
        super.appendChild(child);
    }

    public TokenNode getIdentifier() { return identifier; }
    public Stmt getStmt() { return stmt; }
}
