package middle.llvm.value.instruction;

import middle.llvm.type.VoidType;
import middle.llvm.value.IRValue;

/**
 * LLVM IR返回指令
 * 支持两种情况:
 *   ret <type> <value>  - 有返回值
 *   ret void           - 无返回值
 */
public class ReturnInstruction extends IRInstruction {
    private final boolean hasReturnValue;
    
    /**
     * 创建有返回值的返回指令
     */
    public ReturnInstruction(IRValue parentBlock, IRValue returnValue) {
        super(parentBlock, returnValue.getType());
        addOperand(returnValue);
        hasReturnValue = true;
    }
    
    /**
     * 创建无返回值的返回指令
     */
    public ReturnInstruction(IRValue parentBlock) {
        super(parentBlock, new VoidType());
        hasReturnValue = false;
    }
    
    /**
     * 判断是否有返回值
     */
    public boolean hasReturnValue() {
        return hasReturnValue;
    }
    
    /**
     * 获取返回值（如果有的话）
     */
    public IRValue getReturnValue() {
        return hasReturnValue ? getOperand(0) : null;
    }
    
    @Override
    public boolean isTerminatorInstruction() {
        return true; // ret指令是终结指令
    }
    
    @Override
    public String getOpcodeName() {
        return "ret";
    }
    
    @Override
    public String toString() {
        if (hasReturnValue) {
            IRValue returnValue = getReturnValue();
            return "ret " + returnValue.getType() + " " + returnValue.getName();
        } else {
            return "ret void";
        }
    }
}