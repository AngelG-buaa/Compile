package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

/**
 * MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
 */
public class MulExp extends BranchNode {
    private UnaryExp unaryExp;
    private MulExp mulExp;
    private TokenNode operator;

    public MulExp() {
        super(SynType.MulExp);
        this.unaryExp = null;
        this.mulExp = null;
        this.operator = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.MULT
        || child.getNodeType() == SynType.DIV
        || child.getNodeType() == SynType.MOD
        || child.getNodeType() == SynType.BITANDK
        || child.getNodeType() == SynType.BITORK
        || child.getNodeType() == SynType.BITXORK
        || child.getNodeType() == SynType.SHLK
        || child.getNodeType() == SynType.ASHRK) {
            operator = (TokenNode) child;
        } else if (child.getNodeType() == SynType.UnaryExp) {
            unaryExp = (UnaryExp) child;
        } else if (child.getNodeType() == SynType.MulExp) {
            mulExp = (MulExp) child;
        }
        super.appendChild(child);
    }
    
    public boolean isUnaryExp() {
        return mulExp == null;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }

    public MulExp getMulExp() {
        return mulExp;
    }

    public TokenNode getOperator() {
        return operator;
    }
}
