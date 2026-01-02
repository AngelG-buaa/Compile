package back.mips.instruction.assign;

import back.mips.register.Reg;

public class Li extends AssignM {
    public Li(Reg target, int source) {
        super(target, Integer.valueOf(source));
    }

    public int getFrom() {
        return ((Integer) this.source).intValue();
    }

    @Override
    public String toString() {
        return "li " + target + ", " + getFrom();
    }
}
