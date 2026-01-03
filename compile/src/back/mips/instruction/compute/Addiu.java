package back.mips.instruction.compute;

import back.mips.InstrM;
import back.mips.register.Reg;

public class Addiu extends InstrM {
    private final Reg destination;
    private final Reg source;
    private final int immediate;

    public Addiu(Reg destination, Reg source, int immediate) {
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
        // 说明：addiu 使用 16 位有符号立即数，常用于地址调整与栈指针偏移
        return String.format("addiu %s, %s, %d", destination, source, immediate);
    }
}
