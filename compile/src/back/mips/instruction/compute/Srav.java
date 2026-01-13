package back.mips.instruction.compute;

import back.mips.register.Reg;

public class Srav extends ComputeM {
    public Srav(Reg destination, Reg left, Reg right) {
        super(destination, left, right);
    }

    @Override
    public String toString() {
        return super.toString("srav");
    }
}
