package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public class LbLabelOff extends InstrM {
    private final Reg destination;
    private final String label;
    private final int offset;

    public LbLabelOff(Reg destination, String label, int offset) {
        this.destination = destination;
        this.label = label;
        this.offset = offset;
    }

    @Override
    public String toString() {
        // 说明：lb 以标签+偏移的伪指令，用于折叠 la+addiu+lb
        String offStr = offset >= 0 ? ("+" + offset) : String.valueOf(offset);
        return String.format("lb %s, %s%s", destination, label, offStr);
    }
}
