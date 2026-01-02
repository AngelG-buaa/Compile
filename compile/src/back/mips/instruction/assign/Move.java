package back.mips.instruction.assign;

import back.mips.register.Reg;

public class Move extends AssignM {
    public Move(Reg target, Reg source) {
        super(target, source);
    }

    public Reg getFrom() {
        return (Reg) this.source;
    }

    @Override
    public String toString() {
        return "move " + target + ", " + source;
    }
}
