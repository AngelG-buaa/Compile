package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.decl.Decl;


/**
 * BlockItem → Decl | Stmt
 */
public class BlockItem extends BranchNode {
    private Decl decl;
    private Stmt stmt;

    public BlockItem() {
        super(SynType.BlockItem);
        this.decl = null;
        this.stmt = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.Decl) {
            decl = (Decl) child;
        } else if (child.getNodeType() == SynType.Stmt) {
            stmt = (Stmt) child;
        }

        super.appendChild(child);
    }
    
    // Getter methods
    public Decl getDecl() {
        return decl;
    }
    
    public Stmt getStmt() {
        return stmt;
    }
    
    public boolean isDecl() {
        return decl != null;
    }
    
    public boolean isStmt() {
        return stmt != null;
    }

}
