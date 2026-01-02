package back.mips.instruction.compute;

import back.mips.InstrM;
import back.mips.register.Reg;

public class ComputeM extends InstrM {
    protected Reg destination;
    protected Reg left;
    protected Reg right;

    public ComputeM(Reg destination, Reg left, Reg right) {
        this.destination = destination;
        this.left = left;
        this.right = right;
    }

    public boolean hasDestination() {
        return destination != null;
    }

    public String toString(String opCode) {
        if (hasDestination()) {
            return String.format("%s %s, %s, %s", opCode, destination, left, right);
        } else {
            return String.format("%s %s, %s", opCode, left, right);
        }
    }
}
