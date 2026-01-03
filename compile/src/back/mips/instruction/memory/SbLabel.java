package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public class SbLabel extends InstrM {
    private final Reg source;
    private final String label;

    public SbLabel(Reg source, String label) {
        this.source = source;
        this.label = label;
    }

    @Override
    public String toString() {
        // 说明：sb 以标签为基址的伪指令，折叠 la+sb
        return String.format("sb %s, %s", source, label);
    }
}
