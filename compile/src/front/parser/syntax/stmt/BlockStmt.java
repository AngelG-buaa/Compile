package front.parser.syntax.stmt;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;

/**
 * 对应Stmt → Block
 * 其中Block → '{' { BlockItem } '}'
 */
public class BlockStmt extends Stmt {
    private Block block;

    public BlockStmt() {
        super();
        block = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.Block) {
            block = (Block) child;
        }
        super.appendChild(child);
    }
    
    // Getter methods
    public Block getBlock() {
        return block;
    }
    
    public boolean hasBlock() {
        return block != null;
    }

}
