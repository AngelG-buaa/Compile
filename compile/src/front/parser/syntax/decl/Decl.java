package front.parser.syntax.decl;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;


/**
 * Decl → ConstDecl | VarDecl
 * 覆盖两种声明
 */
public class Decl extends BranchNode {
    private ConstDecl constDecl;
    private VarDecl varDecl;

    public Decl() {
        super(SynType.Decl);
        this.constDecl = null;
        this.varDecl = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.ConstDecl) {
            constDecl = (ConstDecl) child;
        } else if (child.getNodeType() == SynType.VarDecl) {
            varDecl = (VarDecl) child;
        }
        super.appendChild(child);
    }

    public ConstDecl getConstDecl() {
        return constDecl;
    }
    public VarDecl getVarDecl() {
        return varDecl;
    }
    public boolean isConstDecl() {
        return constDecl != null;
    }
    public boolean isVarDecl() {
        return varDecl != null;
    }
}
