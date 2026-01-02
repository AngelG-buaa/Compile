package back.mips.instruction.compare;

import back.mips.InstrM;
import back.mips.register.Reg;

public abstract class CompareM extends InstrM {
    protected Reg destination;
    protected Reg left;
    protected Reg right;

    public CompareM(Reg destination, Reg left, Reg right) {
        this.destination = destination;
        this.left = left;
        this.right = right;
    }

    public String toString(String opCode) {
        return opCode + " " + destination + ", " + left + ", " + right;
    }
}
