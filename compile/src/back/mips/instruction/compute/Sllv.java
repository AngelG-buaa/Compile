package back.mips.instruction.compute;

import back.mips.register.Reg;

public class Sllv extends ComputeM {
    public Sllv(Reg destination, Reg left, Reg right) {
        super(destination, left, right);
    }

    @Override
    public String toString() {
        return super.toString("sllv");
    }
}
