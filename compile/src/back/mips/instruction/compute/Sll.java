package back.mips.instruction.compute;

import back.mips.InstrM;
import back.mips.register.Reg;

public class Sll extends InstrM {
    protected Reg destination;
    private Reg source;
    private int shiftAmount;
    
    public Sll(Reg destination, Reg source, int shiftAmount) {
        this.destination = destination;
        this.source = source;
        this.shiftAmount = shiftAmount;
    }
    
    @Override
    public String toString() {
        return String.format("sll %s, %s, %d", destination, source, shiftAmount);
    }
}
