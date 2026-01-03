package back.mips.instruction.compute;

import back.mips.InstrM;
import back.mips.register.Reg;

public class Andi extends InstrM {
    private final Reg destination;
    private final Reg source;
    private final int immediate;

    public Andi(Reg destination, Reg source, int immediate) {
        this.destination = destination;
        this.source = source;
        this.immediate = immediate;
    }

    public Reg getDestination() {
        return destination;
    }

    public Reg getSource() {
        return source;
    }

    public int getImmediate() {
        return immediate;
    }

    @Override
    public String toString() {
        // 说明：andi 用于与 16 位无符号立即数按位与，常用于掩码 (如 0xFF)
        return String.format("andi %s, %s, %d", destination, source, immediate);
    }
}
