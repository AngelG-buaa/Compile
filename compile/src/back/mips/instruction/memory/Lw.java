package back.mips.instruction.memory;

import back.mips.register.Reg;

public class Lw extends MemoryM {
    public Lw(Reg destination, int offset, Reg base) {
        super(destination, offset, base);
    }

    @Override
    public String toString() {
        return super.toString("lw");
    }
}
