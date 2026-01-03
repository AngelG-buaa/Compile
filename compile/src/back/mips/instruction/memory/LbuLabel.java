package back.mips.instruction.memory;

import back.mips.InstrM;
import back.mips.register.Reg;

public class LbuLabel extends InstrM {
    private final Reg destination;
    private final String label;

    public LbuLabel(Reg destination, String label) {
        this.destination = destination;
        this.label = label;
    }

    @Override
    public String toString() {
        // 说明：lbu 以标签为基址的伪指令，折叠 la+lbu，保证无符号装载
        return String.format("lbu %s, %s", destination, label);
    }
}
