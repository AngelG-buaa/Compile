package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.exp.LVal;

public class DecStmt extends Stmt {
    private LVal identifier;
    public DecStmt() {
        super();
    }

    @Override
    public void appendChild(AstNode child) {
        super.appendChild(child);
        if (child instanceof LVal) {
            identifier = (LVal) child;
        }
    }

    public LVal getIdentifier() {
        return identifier;
    }
}
