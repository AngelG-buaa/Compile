package front.parser.syntax.stmt;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

import java.util.ArrayList;


/**
 * Block → '{' { BlockItem } '}'
 */
public class Block extends BranchNode {
    private ArrayList<BlockItem> blockItems;

    public Block() {
        super(SynType.Block);
        blockItems = new ArrayList<>();
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.BlockItem) {
            blockItems.add((BlockItem) child);
        }
        super.appendChild(child);
    }
    
    // Getter methods
    public ArrayList<BlockItem> getBlockItems() {
        return blockItems;
    }
    
    public int getItemCount() {
        return blockItems.size();
    }
    
    public boolean isEmpty() {
        return blockItems.isEmpty();
    }
    
    public BlockItem getItem(int index) {
        if (index >= 0 && index < blockItems.size()) {
            return blockItems.get(index);
        }
        return null;
    }
}
