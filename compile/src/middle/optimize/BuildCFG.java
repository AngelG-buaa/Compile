package middle.optimize;

import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRFunction;
import middle.llvm.value.instruction.BranchInstruction;
import middle.llvm.value.instruction.IRInstruction;
import middle.llvm.value.instruction.JumpInstruction;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * 控制流图构建器（BuildCFG）
 *
 * 职责：
 * - 从终结指令（br/jump/ret）建立基本块之间的前驱/后继边；
 * - 计算支配集合：若删除块 D 后，块 B 不可达，则 D 支配 B；
 * - 根据支配集合计算“直接支配者”（Immediate Dominator）；
 * - 基于支配树计算“支配边界”（Dominance Frontier），用于 Phi 插入；
 *
 * 示例：
 *  未优化 IR：
 *    entry: br i1 %cond, label %b1, label %b2
 *    b1:    br label %b3
 *    b2:    br label %b3
 *    b3:    ret i32 0
 *  支配关系：entry 支配 {b1,b2,b3}; b3 的前驱为 {b1,b2}；
 *  支配边界：DF(entry) 不含 b3；DF(b1) ∋ b3，DF(b2) ∋ b3；在 b3 可能需要为合并的变量插入 Phi。
 */
public class BuildCFG extends Optimizer {

    @Override
    public void optimize() {
        // 1) 清除之前生成的支配/CFG数据，防止脏数据影响分析
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            for (IRBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                irBasicBlock.clearCFG();
            }
        }
        // 2) 构建CFG：根据终结指令建立边
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            for (IRBasicBlock visitBlock : irFunction.getBasicBlocks()) {
                // 获取基本块的最后一条指令（终结指令）
                IRInstruction lastInstr = visitBlock.getLastInstruction();

                if (lastInstr instanceof JumpInstruction jumpInstr) {
                    // 无条件跳转
                    IRBasicBlock targetBlock = (IRBasicBlock) jumpInstr.getTargetBlock();
                    visitBlock.addSuccessor(targetBlock);
                    targetBlock.addPredecessor(visitBlock);
                } else if (lastInstr instanceof BranchInstruction branchInstr) {
                    // 条件分支
                    IRBasicBlock trueBlock = (IRBasicBlock) branchInstr.getTrueBranch();
                    IRBasicBlock falseBlock = (IRBasicBlock) branchInstr.getFalseBranch();

                    visitBlock.addSuccessor(trueBlock);
                    visitBlock.addSuccessor(falseBlock);
                    trueBlock.addPredecessor(visitBlock);
                    falseBlock.addPredecessor(visitBlock);
                }
            }
        }
        // 3) 构建支配关系：删除某块后不可达 → 该块支配不可达块
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            LinkedList<IRBasicBlock> blockList = irFunction.getBasicBlocks();
            if (blockList.isEmpty()) continue;

            for (IRBasicBlock deleteBlock : blockList) {
                Set<IRBasicBlock> visited = new HashSet<>();
                // 从入口基本块开始搜索，跳过被删除的基本块
                this.searchDfs(blockList.get(0), deleteBlock, visited);

                for (IRBasicBlock visitBlock : blockList) {
                    // 如果删除deleteBlock后visitBlock不可达，则deleteBlock支配visitBlock
                    if (!visited.contains(visitBlock)) {
                        visitBlock.getDominatedBy().add(deleteBlock);
                    }
                }
            }
        }
        // 4) 构建直接支配关系：在支配集合中找出直接支配者（最近的支配者）
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            for (IRBasicBlock visitBlock : irFunction.getBasicBlocks()) {
                if (visitBlock == irFunction.getEntryBlock()) {
                    continue;
                }
                
                IRBasicBlock idom = null;
                for (IRBasicBlock dominator : visitBlock.getDominatedBy()) {
                    if (dominator == visitBlock) {
                        continue;
                    }
                    
                    if (idom == null) {
                        idom = dominator;
                    } else if (dominator.getDominatedBy().contains(idom)) {
                        // 如果 dominator 被当前的 idom 支配，说明 dominator 更接近 visitBlock
                        idom = dominator;
                    }
                }
                
                if (idom != null) {
                    visitBlock.setImmediateDominator(idom);
                    idom.getImmediateDominated().add(visitBlock);
                }
            }
        }
        // 5) 构建支配边界：沿支配树从前驱向上，直到遇到 visitBlock 的支配者
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            for (IRBasicBlock visitBlock : irFunction.getBasicBlocks()) {
                for (IRBasicBlock predecessor : visitBlock.getPredecessors()) {
                    IRBasicBlock runner = predecessor;

                    // 沿着支配树向上遍历，直到找到visitBlock的直接支配者
                    while (runner != null && runner != visitBlock.getImmediateDominator()) {
                        runner.getDominanceFrontier().add(visitBlock);
                        runner = runner.getImmediateDominator();
                    }
                }
            }
        }
    }

    /**
     * 深度优先搜索，用于支配关系分析
     *
     * @param visitBlock 当前访问的基本块
     * @param deleteBlock 被删除的基本块
     * @param visited 已访问的基本块集合
     */
    private void searchDfs(IRBasicBlock visitBlock, IRBasicBlock deleteBlock, Set<IRBasicBlock> visited) {
        if (visitBlock == deleteBlock) {
            return;
        }

        visited.add(visitBlock);
        for (IRBasicBlock nextBlock : visitBlock.getSuccessors()) {
            if (!visited.contains(nextBlock) && nextBlock != deleteBlock) {
                this.searchDfs(nextBlock, deleteBlock, visited);
            }
        }
    }

    public String OptimizerName() {
        return "BuildCFG";
    }
}
