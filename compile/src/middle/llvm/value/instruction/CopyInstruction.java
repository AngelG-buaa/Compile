package middle.llvm.value.instruction;

import middle.llvm.type.VoidType;
import middle.llvm.value.IRValue;

/**
 * LLVM IR复制指令
 * 用于优化过程中的值复制操作
 */
public class CopyInstruction extends IRInstruction {
    
    public CopyInstruction(IRValue parentBlock, IRValue targetValue, IRValue sourceValue) {
        super(parentBlock, new VoidType());
        addOperand(targetValue);
        addOperand(sourceValue);
    }
    
    /**
     * 获取目标值
     */
    public IRValue getTargetValue() {
        return getOperand(0);
    }
    
    /**
     * 获取源值
     */
    public IRValue getSourceValue() {
        return getOperand(1);
    }
    
    /**
     * 设置新的源值
     */
    public void setSourceValue(IRValue newSourceValue) {
        if (newSourceValue != null) {
            replaceOperand(1, newSourceValue);
        }
    }
    
    @Override
    public boolean hasSideEffects() {
        return true; // 复制操作有副作用
    }
    
    @Override
    public String getOpcodeName() {
        return "copy";
    }
    
    @Override
    public String toString() {
        return "copy " + getTargetValue().getName() + " <- " + getSourceValue().getName();
    }
}