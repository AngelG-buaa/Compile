package back.mips.instruction.jump;

public class Jal extends JumpM {
    public Jal(String targetLabel) {
        super(targetLabel);
    }

    @Override
    public String toString() {
        return super.toString("jal");
    }
}
