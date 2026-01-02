package middle.llvm.value.instruction;

import middle.llvm.type.IRType;
import middle.llvm.value.IRValue;

/**
 * LLVM IR截断指令
 * <result> = trunc <ty> <value> to <ty2>
 * 将较宽的整数类型截断为较窄的整数类型
 */
public class TruncateInstruction extends IRInstruction {
    
    public TruncateInstruction(IRValue parentBlock, int nameCounter, IRValue originalValue, IRType targetType) {
        super(parentBlock, "%trunc" + nameCounter, targetType);
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
        return "trunc";
    }
    
    @Override
    public String toString() {
        IRValue originalValue = getOriginalValue();
        return getName() + " = trunc " + originalValue.getType() + " " +
               originalValue.getName() + " to " + getType();
    }
}