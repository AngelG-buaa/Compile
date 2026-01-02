package front.parser.syntax.func;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.stmt.Block;


/**
 * MainFuncDef → 'int' 'main' '(' ')' Block
 */
public class MainFuncDef extends BranchNode {
    private TokenNode intk;
    private TokenNode maink;
    private Block body;

    public MainFuncDef() {super(SynType.MainFuncDef);}

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.INTTK) {
            intk = (TokenNode) child;
        } else if (child.getNodeType() == SynType.MAINTK) {
            maink = (TokenNode) child;
        } else if (child.getNodeType() == SynType.Block) {
            body = (Block) child;
        }

        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public TokenNode getIntToken() {
        return intk;
    }

    public TokenNode getMainToken() {
        return maink;
    }

    public Block getBody() {
        return body;
    }

    public String getFunctionName() {
        return "main";
    }

    public int getLineNumber() {
        return maink != null ? maink.getLineNumber() : -1;
    }

    public String getReturnType() {
        return "int";
    }
}
