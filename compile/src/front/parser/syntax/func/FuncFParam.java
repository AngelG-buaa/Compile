package front.parser.syntax.func;


import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.decl.BType;
import front.parser.syntax.exp.ConstExp;

import java.util.ArrayList;

/**
 * FuncFParam → BType Ident ['[' ']']
 * 1.普通变量 2.一维数组变量
 * FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
 */
public class FuncFParam extends BranchNode {
    private BType type;
    private TokenNode identifier;
    private ArrayList<ConstExp> exps;
    private int layerNumber;

    public FuncFParam() {
        super(SynType.FuncFParam);
        this.exps = new ArrayList<>();
        layerNumber = 0;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.BType) {
            type = (BType) child;
        } else if (child.getNodeType() == SynType.IDENFR) {
            identifier = (TokenNode) child;
        } else if (child.getNodeType() == SynType.ConstExp) {
            exps.add((ConstExp) child);
        } else if (child.getNodeType() == SynType.LBRACK) {
            layerNumber = layerNumber + 1;
        }

        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public BType getType() {
        return type;
    }

    public TokenNode getIdentifier() {
        return identifier;
    }

    public ArrayList<ConstExp> getExps() {
        return exps;
    }

    public int getLayerNumber() {
        return layerNumber;
    }

    public String getParamName() {
        return identifier != null ? identifier.getContent() : null;
    }

    public int getLineNumber() {
        return identifier != null ? identifier.getLineNumber() : -1;
    }

    public boolean isArray() {
        return layerNumber > 0;
    }

    public int getDimension() {
        return layerNumber;
    }

    public TokenNode getBaseType() {
        return type != null ? type.getType() : null;
    }
}
