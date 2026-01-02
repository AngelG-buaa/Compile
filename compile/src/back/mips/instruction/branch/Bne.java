package back.mips.instruction.branch;

import back.mips.register.Reg;

public class Bne extends BranchM {
    public Bne(Reg left, Reg right, String targetLabel) {
        super(left, right, targetLabel);
    }

    @Override
    public String toString() {
        return super.toString("bne");
    }
}
