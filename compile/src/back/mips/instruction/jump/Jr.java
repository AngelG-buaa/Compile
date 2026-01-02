package back.mips.instruction.jump;

import back.mips.register.Reg;

public class Jr extends JumpM {
    public Jr(Reg targetReg) {
        super(targetReg);
    }

    @Override
    public String toString() {
        return super.toString("jr");
    }
}
