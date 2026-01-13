package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;

public class GotoStmt extends Stmt {
    private TokenNode identifier;

    public GotoStmt() {
        super(SynType.GotoStmt);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.IDENFR) {
            this.identifier = (TokenNode) child;
        }
        super.appendChild(child);
    }

    public TokenNode getIdentifier() { return identifier; }
}
