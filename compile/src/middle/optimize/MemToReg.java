package middle.optimize;

import middle.llvm.type.ArrayType;
import middle.llvm.type.PointerType;
import middle.llvm.type.IRType;
import middle.llvm.type.IntegerType;
import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRFunction;
import middle.llvm.value.instruction.AllocaInstruction;
import middle.llvm.value.instruction.IRInstruction;

import java.util.ArrayList;

/**
 * 内存到寄存器优化器（MemToReg）
 *
 * 目标：把可优化的局部变量从“内存模型”转为“寄存器模型”（SSA 形式）。
 * 做法：对每个可优化的 `alloca` 使用 InsertPhi：
 * - 在支配边界插入 Phi；
 * - DFS 重命名：用“当前值栈”替代 load/store；
 * - 删除冗余的内存指令。
 *
 * 适用范围：仅基本整型且非数组的 `alloca`（数组通常需要地址运算，不适合直接SSA化）。
 *
 * 示例（前后对比）：
 *  前：
 *    %var = alloca i32
 *    store i32 0, i32* %var
 *    br i1 %c, label %b1, label %b2
 *  b1:
 *    store i32 1, i32* %var
 *    br label %b3
 *  b2:
 *    store i32 2, i32* %var
 *    br label %b3
 *  b3:
 *    %x = load i32, i32* %var
 *    ret i32 %x
 *
 *  后：
 *  entry:
 *    br i1 %c, label %b1, label %b2
 *  b1:
 *    ; 当前值=1
 *    br label %b3
 *  b2:
 *    ; 当前值=2
 *    br label %b3
 *  b3:
 *    %x = phi i32 [ 1, %b1 ], [ 2, %b2 ]
 *    ret i32 %x
 */
public class MemToReg extends Optimizer {
    
    @Override
    public void optimize() {
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            processFunc(irFunction);
        }
    }

    private void processFunc(IRFunction irFunction) {
        IRBasicBlock entryBlock = irFunction.getEntryBlock();
        if (entryBlock == null) {
            return;
        }
        for (IRBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
            processBlock(irBasicBlock, entryBlock);
        }
    }

    private void processBlock(IRBasicBlock irBasicBlock, IRBasicBlock entryBlock) {
        ArrayList<IRInstruction> instrList = new ArrayList<>(irBasicBlock.getAllInstructions());
        for (IRInstruction instr : instrList) {
            processInst(entryBlock, instr);
        }
    }

    private void processInst(IRBasicBlock entryBlock, IRInstruction instr) {
        if (this.isAllocWithoutArray(instr)) {
            // 仅处理入口块中的alloca（标准mem2reg前提）
            // if (instr.getContainer() == entryBlock) {
                InsertPhi insertPhi = new InsertPhi((AllocaInstruction) instr, entryBlock);
                insertPhi.addPhi();
            // }
        }
    }

    /**
     * 判断是否为可优化的值分配指令
     * 
     * 只对非数组类型的alloca指令进行mem2reg优化，
     * 因为数组需要地址计算，不适合直接转换为寄存器操作。
     * 
     * @param instr 待检查的指令
     * @return 如果是可优化的alloca指令返回true
     */
    private boolean isAllocWithoutArray(IRInstruction instr) {
        if (instr instanceof AllocaInstruction allocateInstr) {
            IRType targetType = ((PointerType) allocateInstr.getType()).getPointeeType();
            return !(targetType instanceof ArrayType);
        }
        return false;
    }

    @Override
    public String OptimizerName() {
        return "MemToReg";
    }
}
