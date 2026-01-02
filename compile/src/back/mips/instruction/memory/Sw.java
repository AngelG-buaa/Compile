package back.mips.instruction.memory;

import back.mips.register.Reg;

public class Sw extends MemoryM {
    public Sw(Reg source, int offset, Reg base) {
        super(source, offset, base);
    }

    @Override
    public String toString() {
        return super.toString("sw");
    }
}
