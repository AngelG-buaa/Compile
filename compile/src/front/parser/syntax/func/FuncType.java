package front.parser.syntax.func;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;

/**
 * FuncType → 'void' | 'int'
 */
public class FuncType extends BranchNode {
    private TokenNode type;

    public FuncType() {super(SynType.FuncType);}

    @Override
    public void appendChild(AstNode child) {
        type = (TokenNode) child;
        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public TokenNode getType() {
        return type;
    }

    public String getReturnType() {
        return type != null ? type.getContent() : null;
    }

    public boolean isVoid() {
        return type != null && "void".equals(type.getContent());
    }

    public boolean isInt() {
        return type != null && "int".equals(type.getContent());
    }
}
