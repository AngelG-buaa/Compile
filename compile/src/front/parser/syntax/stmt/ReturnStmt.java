package front.parser.syntax.stmt;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.exp.Exp;

/**
 * Stmt → 'return' [Exp] ';'
 */
public class ReturnStmt extends Stmt {
    private TokenNode returnToken;
    private Exp val;

    public ReturnStmt() {
        super();
        this.returnToken = null;
        this.val = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.RETURNTK) {
            this.returnToken = (TokenNode) child;
        } else if (child.getNodeType() == SynType.Exp) {
            this.val = (Exp) child;
        }

        super.appendChild(child);
    }
    
    // Getter methods
    public TokenNode getReturnToken() {
        return returnToken;
    }
    
    public Exp getVal() {
        return val;
    }
    
    public boolean hasReturnValue() {
        return val != null;
    }
    
    public int getLineNumber() {
        return returnToken != null ? returnToken.getLineNumber() : 0;
    }

}
