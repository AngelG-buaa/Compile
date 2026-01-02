package front.parser.syntax.decl;


import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

import java.util.ArrayList;

/**
 * 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
 */
public class ConstDecl extends BranchNode {
    private BType type;
    private ArrayList<ConstDef> constDefs;

    public ConstDecl() {
        super(SynType.ConstDecl);
        constDefs = new ArrayList<>();
    }

    public void appendChild(AstNode child) {
        switch (child.getNodeType()) {
            case BType :
                type = (BType) child;
                super.appendChild(child);
                break;
            case ConstDef :
                constDefs.add((ConstDef) child);
                super.appendChild(child);
                break;
            default :
                super.appendChild(child);
                break;
        }

    }

    public BType getType() {
        return type;
    }
    public ArrayList<ConstDef> getConstDefs() {
        return constDefs;
    }
}
