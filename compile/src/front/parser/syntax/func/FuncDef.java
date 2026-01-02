package front.parser.syntax.func;


import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.stmt.Block;

/**
 * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
 */
public class FuncDef extends BranchNode {
    private FuncType funcType;
    private TokenNode identifier;
    private FuncFParams params;
    private Block body;

    public FuncDef() {
        super(SynType.FuncDef);
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.FuncType) {
            this.funcType = (FuncType) child;
        } else if (child.getNodeType() == SynType.IDENFR) {
            this.identifier = (TokenNode) child;
        } else if (child.getNodeType() == SynType.FuncFParams) {
            this.params = (FuncFParams) child;
        } else if (child.getNodeType() == SynType.Block) {
            this.body = (Block) child;
        }

        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public FuncType getFuncType() {
        return funcType;
    }

    public TokenNode getIdentifier() {
        return identifier;
    }

    public FuncFParams getParams() {
        return params;
    }

    public FuncFParam getParamAt(int index) {
        return params.getParam(index);
    }

    public Block getBody() {
        return body;
    }

    public boolean hasParams() {
        return params != null;
    }

    public String getFunctionName() {
        return identifier != null ? identifier.getContent() : null;
    }

    public int getLineNumber() {
        return identifier != null ? identifier.getLineNumber() : -1;
    }
}
