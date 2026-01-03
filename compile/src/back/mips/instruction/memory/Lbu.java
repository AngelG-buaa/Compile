package back.mips.instruction.memory;

import back.mips.register.Reg;

public class Lbu extends MemoryM {
    public Lbu(Reg destination, int offset, Reg base) {
        super(destination, offset, base);
    }

    @Override
    public String toString() {
        // 说明：lbu 为无符号字节装载，防止符号扩展影响上层零扩展语义
        return super.toString("lbu");
    }
}
