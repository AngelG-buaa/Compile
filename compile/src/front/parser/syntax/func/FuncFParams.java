package front.parser.syntax.func;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

import java.util.ArrayList;


/**
 * FuncFParams → FuncFParam { ',' FuncFParam }
 */
public class FuncFParams extends BranchNode {
    private ArrayList<FuncFParam> params;

    public FuncFParams() {
        super(SynType.FuncFParams);
        params = new ArrayList<>();
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.FuncFParam) {
            params.add((FuncFParam) child);
        }
        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public ArrayList<FuncFParam> getParams() {
        return params;
    }

    public int getParamCount() {
        return params.size();
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public FuncFParam getParam(int index) {
        if (index >= 0 && index < params.size()) {
            return params.get(index);
        }
        return null;
    }
}
