package back.mips.instruction.compute;

import back.mips.InstrM;
import back.mips.register.Reg;

public class Sra extends InstrM {
    private final Reg destination;
    private final Reg source;
    private final int shift;

    public Sra(Reg destination, Reg source, int shift) {
        this.destination = destination;
        this.source = source;
        this.shift = shift;
    }

    @Override
    public String toString() {
        // 说明：sra 为算术右移，保留符号位，适合有符号数的位移与近似除法
        return String.format("sra %s, %s, %d", destination, source, shift);
    }
}
