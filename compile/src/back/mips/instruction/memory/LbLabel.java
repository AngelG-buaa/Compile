package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public class LbLabel extends InstrM {
    private final Reg destination;
    private final String label;

    public LbLabel(Reg destination, String label) {
        this.destination = destination;
        this.label = label;
    }

    @Override
    public String toString() {
        // 说明：lb 以标签为基址的伪指令，折叠 la+lb
        return String.format("lb %s, %s", destination, label);
    }
}
