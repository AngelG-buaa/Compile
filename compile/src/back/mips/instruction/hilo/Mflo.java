package back.mips.instruction.hilo;

import back.mips.register.Reg;

public class Mflo extends HiLoM {
    public Mflo(Reg destination) {
        super(destination);
    }

    @Override
    public String toString() {
        return super.toString("mflo");
    }
}
