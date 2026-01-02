package back.mips.instruction.compute;

import back.mips.register.Reg;

public class Subu extends ComputeM {
    public Subu(Reg destination, Reg left, Reg right) {
        super(destination, left, right);
    }

    @Override
    public String toString() {
        return super.toString("subu");
    }
}
