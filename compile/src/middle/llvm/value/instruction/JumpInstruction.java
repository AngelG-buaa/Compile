package middle.llvm.value.instruction;

import middle.llvm.type.VoidType;
import middle.llvm.value.IRValue;

/**
 * LLVM IR无条件跳转指令
 * br label <dest>
 */
public class JumpInstruction extends IRInstruction {
    
    public JumpInstruction(IRValue parentBlock, IRValue targetBlock) {
        super(parentBlock, new VoidType());
        addOperand(targetBlock);
    }
    
    /**
     * 获取跳转目标基本块
     */
    public IRValue getTargetBlock() {
        return getOperand(0);
    }

    /**
     * 设置跳转目标基本块
     */
    public void setTargetBlock(IRValue targetBlock) {
        replaceOperand(0, targetBlock);
    }
    
    @Override
    public boolean isTerminatorInstruction() {
        return true; // 跳转指令是终结指令
    }
    
    @Override
    public String getOpcodeName() {
        return "br";
    }
    
    @Override
    public String toString() {
        return "br label " + getTargetBlock().getName();
    }
}