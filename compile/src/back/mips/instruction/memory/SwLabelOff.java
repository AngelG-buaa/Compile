package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public class SwLabelOff extends InstrM {
    private final Reg source;
    private final String label;
    private final int offset;

    public SwLabelOff(Reg source, String label, int offset) {
        this.source = source;
        this.label = label;
        this.offset = offset;
    }

    @Override
    public String toString() {
        // 说明：sw 以标签+偏移的伪指令，用于折叠 la+addiu+sw
        String offStr = offset >= 0 ? ("+" + offset) : String.valueOf(offset);
        return String.format("sw %s, %s%s", source, label, offStr);
    }
}
