package back.mips.instruction.compare;

import back.mips.register.Reg;

public class Sle extends CompareM {
    public Sle(Reg destination, Reg left, Reg right) {
        super(destination, left, right);
    }

    @Override
    public String toString() {
        return super.toString("sle");
    }
}
