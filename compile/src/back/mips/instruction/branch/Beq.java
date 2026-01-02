package back.mips.instruction.branch;

import back.mips.register.Reg;

public class Beq extends BranchM {
    public Beq(Reg left, Reg right, String targetLabel) {
        super(left, right, targetLabel);
    }

    @Override
    public String toString() {
        return super.toString("beq");
    }
}
