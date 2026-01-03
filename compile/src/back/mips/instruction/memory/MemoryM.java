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

    public Reg getTargetReg() {
        // Peephole 使用：读取访存目标寄存器
        return targetReg;
    }

    public int getOffset() {
        // Peephole 使用：读取偏移以判断是否可折叠到基址+立即数
        return offset;
    }

    public Reg getBaseReg() {
        // Peephole 使用：读取基址寄存器
        return baseReg;
    }

    public String toString(String opCode) {
        return String.format("%s %s, %d(%s)", opCode, targetReg, offset, baseReg);
    }
}
