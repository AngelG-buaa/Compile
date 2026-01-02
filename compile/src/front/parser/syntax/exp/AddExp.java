package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import java.util.ArrayList;
import java.util.List;

/**
 * AddExp → MulExp | AddExp ('+' | '−') MulExp
 */
public class AddExp extends BranchNode {
    private MulExp mulExp;
    private TokenNode operator;
    private AddExp addExp;

    public AddExp() {
        super(SynType.AddExp);
        this.mulExp = null;
        this.operator = null;
        this.addExp = null;
    }

    @Override
    public void appendChild(AstNode child) {
        super.appendChild(child);
        if (child.getNodeType() == SynType.MulExp) {
            this.mulExp = (MulExp) child;
        } else if (child.getNodeType() == SynType.AddExp) {
            this.addExp = (AddExp) child;
        } else if (child.getNodeType() == SynType.PLUS
                || child.getNodeType() == SynType.MINU) {
            this.operator = (TokenNode) child;
        }
    }
    
    public boolean isMulExp() {
        return addExp == null;
    }

    public MulExp getMulExp() {
        return mulExp;
    }

    public TokenNode getOperator() {
        return operator;
    }

    public AddExp getAddExp() {
        return addExp;
    }
}