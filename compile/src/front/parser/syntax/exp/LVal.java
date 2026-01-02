package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;

import java.util.ArrayList;

/**
 * LVal → Ident {'[' Exp ']'}
 * 1.普通变量、常量 2.一维数组
 * 我这里的做法支持多维数组
 */
public class LVal extends BranchNode {
    private TokenNode identifier;
    private ArrayList<Exp> exps;

    public LVal() {
        super(SynType.LVal);
        this.identifier = null;
        this.exps = new ArrayList<>();
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.IDENFR) {
            this.identifier = (TokenNode) child;
        } else if (child.getNodeType() == SynType.Exp) {
            this.exps.add((Exp) child);
        }
        super.appendChild(child);
    }

    // Getter methods for semantic analysis
    public TokenNode getIdentifier() {
        return identifier;
    }

    public ArrayList<Exp> getExps() {
        return exps;
    }

    public String getIdentifierName() {
        return identifier != null ? identifier.getContent() : null;
    }

    public int getLineNumber() {
        return identifier != null ? identifier.getLineNumber() : -1;
    }

    public boolean isArray() {
        return !exps.isEmpty();
    }

    public int getDimension() {
        return exps.size();
    }

    public Exp getExp(int index) {
        if (index >= 0 && index < exps.size()) {
            return exps.get(index);
        }
        return null;
    }
}
