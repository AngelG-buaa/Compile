package back.mips.instruction.compute;

import back.mips.InstrM;
import back.mips.register.Reg;

public class Srl extends InstrM {
    private final Reg destination;
    private final Reg source;
    private final int shift;

    public Srl(Reg destination, Reg source, int shift) {
        this.destination = destination;
        this.source = source;
        this.shift = shift;
    }

    @Override
    public String toString() {
        // 说明：srl 为逻辑右移，使用零填充高位，适合无符号除以 2 的幂等
        return String.format("srl %s, %s, %d", destination, source, shift);
    }
}
