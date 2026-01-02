package back.mips.instruction.compute;

import back.mips.register.Reg;

public class Div extends ComputeM {
    public Div(Reg left, Reg right) {
        super(null, left, right);
    }

    @Override
    public String toString() {
        return super.toString("div");
    }
}
