package back.mips.instruction.compute;

import back.mips.register.Reg;

public class Mult extends ComputeM {
    public Mult(Reg left, Reg right) {
        super(null, left, right);
    }
    
    @Override
    public String toString() {
        return super.toString("mult");
    }
}
