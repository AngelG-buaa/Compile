package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public class SwLabel extends InstrM {
    private final Reg source;
    private final String label;

    public SwLabel(Reg source, String label) {
        this.source = source;
        this.label = label;
    }

    @Override
    public String toString() {
        // 说明：sw 以标签为基址的伪指令，折叠 la+sw
        return String.format("sw %s, %s", source, label);
    }
}
