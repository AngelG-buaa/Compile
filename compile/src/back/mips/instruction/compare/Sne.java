package back.mips.instruction.compare;

import back.mips.register.Reg;

public class Sne extends CompareM {
    public Sne(Reg destination, Reg left, Reg right) {
        super(destination, left, right);
    }

    @Override
    public String toString() {
        return super.toString("sne");
    }
}
