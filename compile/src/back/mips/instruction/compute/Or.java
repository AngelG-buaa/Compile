package back.mips.instruction.compute;

import back.mips.register.Reg;

public class Or extends ComputeM {
    public Or(Reg destination, Reg left, Reg right) {
        super(destination, left, right);
    }

    @Override
    public String toString() {
        return super.toString("or");
    }
}
