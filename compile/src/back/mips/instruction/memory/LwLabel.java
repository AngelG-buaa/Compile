package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public class LwLabel extends InstrM {
    private final Reg destination;
    private final String label;

    public LwLabel(Reg destination, String label) {
        this.destination = destination;
        this.label = label;
    }

    @Override
    public String toString() {
        // 说明：lw 以标签为基址的伪指令，折叠 la+lw 以减少一条指令
        return String.format("lw %s, %s", destination, label);
    }
}
