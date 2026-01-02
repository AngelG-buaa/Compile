package middle.llvm.value.instruction;

import middle.llvm.type.ArrayType;
import middle.llvm.type.IRType;
import middle.llvm.type.PointerType;
import middle.llvm.value.IRValue;

import java.util.List;

/**
 * LLVM IR获取元素指针指令
 * <result> = getelementptr <ty>, ptr <ptrval>{, <ty> <idx>}*
 * 
 * 支持两种模式:
 * 1. 数组元素寻址: getelementptr baseType, baseType* base, A, B -> elementType*
 * 2. 指针偏移: getelementptr baseType, baseType* base, A -> baseType*
 */
public class GetElementPtrInstruction extends IRInstruction {
    private final int indexCount;
    private final IRType baseType;
    
    /**
     * 创建数组元素寻址指令 (两个索引)
     * getelementptr baseType, baseType* base, leftIndex, rightIndex
     */
    public GetElementPtrInstruction(IRValue parentBlock, int nameCounter, IRValue basePointer, 
                                  IRValue leftIndex, IRValue rightIndex) {
        super(parentBlock, "%gep" + nameCounter, 
              new PointerType(((ArrayType) calculateBaseType(basePointer)).getElementType()));
        this.indexCount = 2;
        this.baseType = calculateBaseType(basePointer);
        addOperand(basePointer);
        addOperand(leftIndex);
        addOperand(rightIndex);
    }
    
    /**
     * 创建指针偏移指令 (一个索引)
     * getelementptr baseType, baseType* base, leftIndex
     */
    public GetElementPtrInstruction(IRValue parentBlock, int nameCounter, IRValue basePointer, IRValue leftIndex) {
        super(parentBlock, "%gep" + nameCounter, (PointerType) basePointer.getType());
        this.indexCount = 1;
        this.baseType = calculateBaseType(basePointer);
        addOperand(basePointer);
        addOperand(leftIndex);
    }
    
    /**
     * 获取基指针操作数
     */
    public IRValue getBasePointer() {
        return getOperand(0);
    }
    
    /**
     * 获取索引操作数
     */
    public IRValue getIndex(int indexPosition) {
        if (indexPosition >= 0 && indexPosition < indexCount) {
            return getOperand(indexPosition + 1);
        }
        return null;
    }
    
    /**
     * 获取索引数量
     */
    public int getIndexCount() {
        return indexCount;
    }
    
    /**
     * 获取基类型
     */
    public IRType getBaseType() {
        return baseType;
    }
    
    @Override
    public PointerType getType() {
        return (PointerType) super.getType();
    }
    
    @Override
    public String getOpcodeName() {
        return "getelementptr";
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName()).append(" = getelementptr inbounds ");
        builder.append(baseType).append(", ");
        
        // base pointer
        IRValue base = getOperand(0);
        builder.append(base.getType()).append(" ").append(base.getName());
        
        // leftIndex (always exists)
        builder.append(", ");
        IRValue leftIndex = getOperand(1);
        builder.append(leftIndex.getType()).append(" ").append(leftIndex.getName());
        
        // rightIndex (only for 2-index case)
        if (indexCount == 2) {
            builder.append(", ");
            IRValue rightIndex = getOperand(2);
            builder.append(rightIndex.getType()).append(" ").append(rightIndex.getName());
        }
        
        return builder.toString();
    }
    
    /**
     * 计算基类型
     */
    private static IRType calculateBaseType(IRValue basePointer) {
        if (basePointer.getType() instanceof PointerType) {
            return ((PointerType) basePointer.getType()).getPointeeType();
        }
        throw new IllegalArgumentException("Base pointer must be of pointer type");
    }
}