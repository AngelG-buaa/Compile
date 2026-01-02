package back.mips.instruction.assign;

import back.mips.InstrM;
import back.mips.register.Reg;

public abstract class AssignM extends InstrM {
    protected Object source;
    protected Reg target;

    protected AssignM(Reg target, Object source) {
        this.target = target;
        this.source = source;
    }

    public Reg getTo() {
        return this.target;
    }
}
