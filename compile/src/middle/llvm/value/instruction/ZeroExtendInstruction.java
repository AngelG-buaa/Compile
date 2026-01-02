package middle.llvm.value.instruction;

import middle.llvm.type.IRType;
import middle.llvm.value.IRValue;

/**
 * LLVM IR零扩展指令
 * <result> = zext <ty> <value> to <ty2>
 * 将较窄的整数类型零扩展为较宽的整数类型
 */
public class ZeroExtendInstruction extends IRInstruction {
    
    public ZeroExtendInstruction(IRValue parentBlock, int nameCounter, IRValue originalValue, IRType targetType) {
        super(parentBlock, "%zext" + nameCounter, targetType);
        addOperand(originalValue);
    }
    
    /**
     * 获取原始值操作数
     */
    public IRValue getOriginalValue() {
        return getOperand(0);
    }
    
    /**
     * 获取目标类型
     */
    public IRType getTargetType() {
        return getType();
    }
    
    /**
     * 获取源类型
     */
    public IRType getSourceType() {
        return getOriginalValue().getType();
    }
    
    @Override
    public String getOpcodeName() {
        return "zext";
    }
    
    @Override
    public String toString() {
        IRValue originalValue = getOriginalValue();
        return getName() + " = zext " + originalValue.getType() + " " +
               originalValue.getName() + " to " + getType();
    }
}