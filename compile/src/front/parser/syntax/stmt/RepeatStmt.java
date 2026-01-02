package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;

/**
 * 'repeat' Stmt 'until' '(' Cond ')' ';'
 */
public class RepeatStmt extends Stmt {
    @Override
    public void appendChild(AstNode child) {
        super.appendChild(child);
    }
}
