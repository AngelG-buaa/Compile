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
     * 创建通用GEP指令 (支持任意数量索引)
     * getelementptr baseType, baseType* base, idx0, idx1, ...
     */
    public GetElementPtrInstruction(IRValue parentBlock, int nameCounter, IRValue basePointer, List<IRValue> indices) {
        super(parentBlock, "%gep" + nameCounter, calculateResultType(basePointer, indices));
        this.indexCount = indices.size();
        this.baseType = calculateBaseType(basePointer);
        addOperand(basePointer);
        for (IRValue idx : indices) {
            addOperand(idx);
        }
    }

    private static IRType calculateResultType(IRValue basePointer, List<IRValue> indices) {
        IRType currentType = calculateBaseType(basePointer);
        // indices[0] is pointer offset, doesn't change type (unless it's pointer arithmetic, but conceptually we are stepping through memory)
        // Actually, GEP type calculation:
        // GEP Ptr, Idx0, Idx1...
        // T = Pointee(Ptr)
        // For Idx0: result is T* (pointer arithmetic on base)
        // For Idx1: T must be Array or Struct. T = ElementType(T).
        // ...
        // Finally result is T*
        
        // However, standard GEP logic:
        // 1st index steps through the pointer itself.
        // Subsequent indices step into the aggregate.
        
        for (int i = 1; i < indices.size(); i++) {
            if (currentType instanceof ArrayType) {
                currentType = ((ArrayType) currentType).getElementType();
            } else {
                // Struct support if needed, otherwise error
                throw new IllegalArgumentException("Indexing into non-aggregate type: " + currentType);
            }
        }
        return new PointerType(currentType);
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
        for (int i = 2; i < indexCount + 1; i++) {
            builder.append(", ");
            IRValue index = getOperand(i);
            builder.append(index.getType()).append(" ").append(index.getName());
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