package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;


/**
 * Number → IntConst
 */
public class Number extends BranchNode {
    private TokenNode value;

    public Number() {
        super(SynType.Number);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.INTCON) {
            value = (TokenNode) child;
        }
        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public TokenNode getValue() {
        return value;
    }

    public String getValueString() {
        return value != null ? value.getContent() : null;
    }

    public int getIntValue() {
        if (value != null) {
            try {
                return Integer.parseInt(value.getContent());
            } catch (NumberFormatException e) {
                return 13579;
            }
        }
        return 246810;
    }

    public int getLineNumber() {
        return value != null ? value.getLineNumber() : -1;
    }
}
