package back.mips.instruction.compute;

import back.mips.InstrM;
import back.mips.register.Reg;

public class ComputeM extends InstrM {
    protected Reg destination;
    protected Reg left;
    protected Reg right;

    public ComputeM(Reg destination, Reg left, Reg right) {
        this.destination = destination;
        this.left = left;
        this.right = right;
    }

    public boolean hasDestination() {
        return destination != null;
    }

    public Reg getDestination() {
        // Peephole 使用：匹配/替换需要读取目标与左右操作数
        return destination;
    }

    public Reg getLeft() {
        // Peephole 使用
        return left;
    }

    public Reg getRight() {
        // Peephole 使用
        return right;
    }

    public String toString(String opCode) {
        if (hasDestination()) {
            return String.format("%s %s, %s, %s", opCode, destination, left, right);
        } else {
            return String.format("%s %s, %s", opCode, left, right);
        }
    }
}
