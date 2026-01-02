package middle.llvm.value.instruction;

import middle.llvm.type.IRType;
import middle.llvm.value.IRValue;
import middle.llvm.value.IRBasicBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLVM IR Phi指令
 * <result> = phi [fast-math-flags] <ty> [<val0>, <label0>], ...
 * Phi指令只能出现在基本块的开头，用于SSA形式中的值合并
 */
public class PhiInstruction extends IRInstruction {
    private final Map<IRBasicBlock, IRValue> incomingValues; // 基本块 -> 对应的值

    /**
     * 构造 Phi 指令，自动生成唯一名称，避免重复定义
     */
    public PhiInstruction(IRValue parentBlock, IRType resultType, List<IRBasicBlock> predecessorBlocks) {
        // 使用自动名称构造，再基于唯一ID设置稳定且唯一的名称
        super(parentBlock, resultType);
        // 以该值的全局唯一ID生成名称，避免跨优化阶段的重复
        setName("%phi" + getUniqueId());

        this.incomingValues = new LinkedHashMap<>();

        // 初始化时为每个前驱基本块添加null值占位符
        for (IRBasicBlock block : predecessorBlocks) {
            incomingValues.put(block, null);
            addOperand(null); // 添加null占位符
        }
    }
    
    /**
     * 获取前驱基本块列表
     */
    public List<IRBasicBlock> getPredecessorBlocks() {
        return new ArrayList<>(incomingValues.keySet());
    }
    
    /**
     * 获取所有传入值
     */
    public List<IRValue> getIncomingValues() {
        return new ArrayList<>(incomingValues.values());
    }
    
    /**
     * 填充指定基本块对应的值
     */
    public void fillIncomingValue(IRValue value, IRBasicBlock fromBlock) {
        // 若不存在该前驱块（例如支配边界或CFG更新导致的缺失），动态建立映射占位
        if (!incomingValues.containsKey(fromBlock)) {
            incomingValues.put(fromBlock, null);
            // 操作数列表与incomingValues保持一一对应关系，先添加占位
            addOperand(null);
        }

        // 更新映射中的值
        incomingValues.put(fromBlock, value);

        // 同步更新操作数列表中对应位置的值
        List<IRBasicBlock> blocks = getPredecessorBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).equals(fromBlock)) {
                replaceOperand(i, value);
                break;
            }
        }
    }
    
    /**
     * 获取指定基本块对应的传入值
     */
    public IRValue getIncomingValue(IRBasicBlock fromBlock) {
        return incomingValues.get(fromBlock);
    }
    
    /**
     * 获取传入值的数量
     */
    public int getIncomingValueCount() {
        return incomingValues.size();
    }
    
    /**
     * 移除指定基本块的传入值
     */
    public void removeIncomingBlock(IRBasicBlock block) {
        if (incomingValues.containsKey(block)) {
            IRValue removedValue = incomingValues.remove(block);
            
            // 更新操作数列表
            List<IRBasicBlock> blocks = new ArrayList<>(incomingValues.keySet());
            clearAllOperands();
            for (IRBasicBlock remainingBlock : blocks) {
                addOperand(incomingValues.get(remainingBlock));
            }
        }
    }
    
    /**
     * 替换传入基本块
     */
    public void replaceIncomingBlock(IRBasicBlock oldBlock, IRBasicBlock newBlock) {
        if (incomingValues.containsKey(oldBlock)) {
            IRValue value = incomingValues.remove(oldBlock);
            incomingValues.put(newBlock, value);
            
            // 更新操作数列表
            List<IRBasicBlock> blocks = getPredecessorBlocks();
            clearAllOperands();
            for (IRBasicBlock block : blocks) {
                addOperand(incomingValues.get(block));
            }
        }
    }
    
    /**
     * 判断是否所有传入值都已填充
     */
    public boolean isComplete() {
        for (IRValue value : incomingValues.values()) {
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void replaceOperand(IRValue oldOperand, IRValue newOperand) {
        super.replaceOperand(oldOperand, newOperand);
        for (Map.Entry<IRBasicBlock, IRValue> entry : incomingValues.entrySet()) {
            if (entry.getValue() == oldOperand) {
                entry.setValue(newOperand);
            }
        }
    }
    
    @Override
    public String getOpcodeName() {
        return "phi";
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName()).append(" = phi ").append(getType()).append(" ");
        
        List<IRBasicBlock> blocks = getPredecessorBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            IRBasicBlock block = blocks.get(i);
            IRValue value = incomingValues.get(block);
            
            builder.append("[ ");
            if (value != null) {
                builder.append(value.getName());
            } else {
                builder.append("undef");
            }
            builder.append(", ").append(block.getName()).append(" ]");
            
            if (i < blocks.size() - 1) {
                builder.append(", ");
            }
        }
        
        return builder.toString();
    }
}
