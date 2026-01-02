package back.mips.instruction.jump;

public class J extends JumpM {
    public J(String targetLabel) {
        super(targetLabel);
    }

    @Override
    public String toString() {
        return super.toString("j");
    }
}
