package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public class LwLabelOff extends InstrM {
    private final Reg destination;
    private final String label;
    private final int offset;

    public LwLabelOff(Reg destination, String label, int offset) {
        this.destination = destination;
        this.label = label;
        this.offset = offset;
    }

    @Override
    public String toString() {
        // 说明：lw 以标签+偏移的伪指令，用于折叠 la+addiu+lw
        String offStr = offset >= 0 ? ("+" + offset) : String.valueOf(offset);
        return String.format("lw %s, %s%s", destination, label, offStr);
    }
}
