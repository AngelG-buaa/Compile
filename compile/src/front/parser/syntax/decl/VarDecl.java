package front.parser.syntax.decl;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

import java.util.ArrayList;

/**
 * VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
 */
public class VarDecl extends BranchNode {
    private BType bType;
    private ArrayList<VarDef> varDefs;
    private boolean isStatic;

    public VarDecl() {
        super(SynType.VarDecl);
        this.varDefs = new ArrayList<>();
        this.bType = null;
        this.isStatic = false;
    }

    @Override
    public void appendChild(AstNode child) {
        super.appendChild(child);
        if (child.getNodeType() == SynType.BType) {
            this.bType = (BType) child;
        } else if (child.getNodeType() == SynType.VarDef) {
            this.varDefs.add((VarDef) child);
        } else if (child.getNodeType() == SynType.STATICTK) {
            this.isStatic = true;
        }
    }

    public BType getBType() {
        return bType;
    }

    public ArrayList<VarDef> getVarDefs() {
        return varDefs;
    }

    public boolean isStatic() {
        return isStatic;
    }

}
