package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public abstract class MemoryM extends InstrM {
    protected Reg targetReg;
    protected int offset;
    protected Reg baseReg;

    protected MemoryM(Reg targetReg, int offset, Reg baseReg) {
        this.targetReg = targetReg;
        this.offset = offset;
        this.baseReg = baseReg;
    }

    public String toString(String opCode) {
        return String.format("%s %s, %d(%s)", opCode, targetReg, offset, baseReg);
    }
}
