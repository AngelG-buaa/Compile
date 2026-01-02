package back.mips.instruction.memory;

import back.mips.register.Reg;

public class Lb extends MemoryM {
    public Lb(Reg destination, int offset, Reg base) {
        super(destination, offset, base);
    }
    
    @Override
    public String toString() {
        return super.toString("lb");
    }
}
