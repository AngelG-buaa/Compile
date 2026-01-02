package middle.optimize;

import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRFunction;
import middle.llvm.value.instruction.BranchInstruction;
import middle.llvm.value.instruction.IRInstruction;
import middle.llvm.value.instruction.JumpInstruction;
import middle.llvm.value.instruction.ReturnInstruction;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 不可达代码清理（URC）
 *
 * 职责：
 * - 删除终结指令（`return`/`br`/`jmp`）之后的冗余指令（它们永远不会执行）。
 * - 从函数入口出发，DFS 标记可达块，移除未标记的不可达基本块。
 *
 * 与 DCE/CFG 的关系：
 * - 作为结构性清理的早期步骤，减少后续优化负担；在 `BuildCFG` 与 `RemoveDeadCode` 前后穿插运行，可更快收敛。
 *
 * 示例（删除终结后冗余）：
 * ```llvm
 * b1:
 *   ret i32 0
 *   %x = add i32 1, 2    ; 无效，终结后不可执行
 * ```
 * 处理后：
 * ```llvm
 * b1:
 *   ret i32 0
 * ```
 *
 * 示例（删除不可达块）：
 * ```llvm
 * entry:
 *   br label %live
 *
 * dead:
 *   %y = mul i32 3, 4
 *   ret i32 %y
 *
 * live:
 *   ret i32 0
 * ```
 * 处理后：
 * ```llvm
 * entry:
 *   br label %live
 *
 * live:
 *   ret i32 0
 * ; 块 dead 被移除（从入口不可达）
 * ```
 */
public class RemoveUnReachCode extends Optimizer {

    @Override
    public void optimize() {
        // 删除多余的jump
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            for (IRBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                boolean hasTerminator = false;
                Iterator<IRInstruction> iterator = irBasicBlock.getAllInstructions().iterator();
                while (iterator.hasNext()) {
                    IRInstruction instr = iterator.next();
                    if (hasTerminator) {
                        // 终结指令后的所有指令都删除
                        instr.clearAllOperands();
                        iterator.remove();
                        continue;
                    }

                    if (instr instanceof JumpInstruction) {
                        hasTerminator = true;
                    } else if (instr instanceof BranchInstruction) {
                        hasTerminator = true;
                    } else if (instr instanceof ReturnInstruction) {
                        hasTerminator = true;
                    }
                }
            }
        }
        // 删除不可达块
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            if (irFunction.getBasicBlocks().isEmpty()) {
                continue;
            }

            IRBasicBlock entryBlock = irFunction.getBasicBlocks().get(0);
            Set<IRBasicBlock> visited = new HashSet<>();
            // 使用dfs记录可达的block
            this.dfsBlock(entryBlock, visited);

            // 删除不可达的基本块
            irFunction.getBasicBlocks().removeIf(block -> !visited.contains(block));
        }
    }

    /**
     * 深度优先搜索标记可达基本块
     *
     * @param block 当前基本块
     * @param visited 已访问的基本块集合
     */
    private void dfsBlock(IRBasicBlock block, Set<IRBasicBlock> visited) {
        if (visited.contains(block)) {
            return;
        }

        visited.add(block);
        // 获取终结指令
        IRInstruction lastInstr = block.getLastInstruction();

        if (lastInstr instanceof ReturnInstruction) {
            // return指令，没有后继
            return;
        } else if (lastInstr instanceof JumpInstruction jumpInstr) {
            // 无条件跳转
            IRBasicBlock targetBlock = (IRBasicBlock) jumpInstr.getTargetBlock();
            this.dfsBlock(targetBlock, visited);
        } else if (lastInstr instanceof BranchInstruction branchInstr) {
            // 条件分支
            IRBasicBlock trueBlock = (IRBasicBlock) branchInstr.getTrueBranch();
            IRBasicBlock falseBlock = (IRBasicBlock) branchInstr.getFalseBranch();
            this.dfsBlock(trueBlock, visited);
            this.dfsBlock(falseBlock, visited);
        }
    }

    @Override
    public String OptimizerName() {
        return "RemoveUnReachCode";
    }
}