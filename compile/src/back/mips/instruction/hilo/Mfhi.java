package back.mips.instruction.hilo;

import back.mips.register.Reg;

public class Mfhi extends HiLoM {
    public Mfhi(Reg destination) {
        super(destination);
    }

    @Override
    public String toString() {
        return super.toString("mfhi");
    }
}
