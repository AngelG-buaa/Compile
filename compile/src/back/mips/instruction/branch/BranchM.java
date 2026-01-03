package back.mips.instruction.branch;

import back.mips.InstrM;
import back.mips.register.Reg;

public abstract class BranchM extends InstrM {
    protected Reg left;
    protected Reg right;
    protected String targetLabel;

    public BranchM(Reg left, Reg right, String targetLabel) {
        this.left = left;
        this.right = right;
        this.targetLabel = targetLabel;
    }

    public Reg getLeft() {
        return left;
    }

    public Reg getRight() {
        return right;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String toString(String opCode) {
        return String.format("%s %s, %s, %s", opCode, left, right, targetLabel);
    }
}
