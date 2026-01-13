package front.parser.syntax.stmt;

import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;

public class Stmt extends BranchNode {
    public Stmt() {super(SynType.Stmt);}

    protected Stmt(SynType type) {
        super(type);
    }
}
