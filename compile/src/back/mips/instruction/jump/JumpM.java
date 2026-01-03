package back.mips.instruction.jump;

import back.mips.InstrM;

public abstract class JumpM extends InstrM {
    protected Object destination;

    protected JumpM(Object destination) {
        this.destination = destination;
    }

    public Object getDestination() {
        return destination;
    }

    public String toString(String opCode) {
        return opCode + " " + destination;
    }
}
