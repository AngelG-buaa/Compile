package back.mips.instruction.hilo;

import back.mips.InstrM;
import back.mips.register.Reg;

public abstract class HiLoM extends InstrM {
    protected Reg targetReg;

    public HiLoM(Reg targetReg) {
        this.targetReg = targetReg;
    }

    public String toString(String opCode) {
        return opCode + " " + targetReg;
    }
}
