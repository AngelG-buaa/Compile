package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

import java.util.ArrayList;

/**
 * FuncRParams → Exp { ',' Exp }
 */
public class FuncRParams extends BranchNode {
    private ArrayList<Exp> exps;

    public FuncRParams() {
        super(SynType.FuncRParams);
        this.exps = new ArrayList<>();
    }

    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.Exp) {
            exps.add((Exp) child);
        }
        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public ArrayList<Exp> getExps() {
        return exps;
    }

    public Exp get(int index) {
        return exps.get(index);
    }

    public int getParamCount() {
        return exps.size();
    }

    public boolean isEmpty() {
        return exps.isEmpty();
    }

    public Exp getParam(int index) {
        if (index >= 0 && index < exps.size()) {
            return exps.get(index);
        }
        return null;
    }
}
