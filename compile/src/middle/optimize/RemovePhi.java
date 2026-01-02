package middle.optimize;

import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRFunction;
import middle.llvm.value.IRValue;
import middle.llvm.value.instruction.BranchInstruction;
import middle.llvm.value.instruction.CopyInstruction;
import middle.llvm.value.instruction.IRInstruction;
import middle.llvm.value.instruction.JumpInstruction;
import middle.llvm.value.instruction.PhiInstruction;

import java.util.*;
import middle.llvm.IRInstructionFactory;

/**
 * Phi指令消除器
 *
 * 将SSA形式的Phi指令转换为非SSA形式的拷贝指令。
 * 包含关键边分割（Critical Edge Splitting）和并行拷贝冲突处理（Parallel Copy Conflict Resolution）。
 */
public class RemovePhi extends Optimizer {

    private final IRInstructionFactory factory;

    public RemovePhi(IRInstructionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void optimize() {
        for (IRFunction function : irModule.getFunctionDefinitions()) {
            // 使用副本遍历，因为 splitEdge 会向列表添加新基本块，避免 ConcurrentModificationException
            List<IRBasicBlock> blocks = new ArrayList<>(function.getBasicBlocks());
            for (IRBasicBlock block : blocks) {
                if (hasPhiInstructions(block)) {
                    eliminatePhiInBlock(function, block);
                }
            }
        }
    }

    @Override
    public String OptimizerName() {
        return "RemovePhi";
    }

    /**
     * 检查基本块开头是否有 Phi 指令
     */
    private boolean hasPhiInstructions(IRBasicBlock block) {
        if (block.getAllInstructions().isEmpty()) return false;
        return block.getAllInstructions().get(0) instanceof PhiInstruction;
    }

    /**
     * 消除指定基本块中的所有 Phi 指令
     */
    private void eliminatePhiInBlock(IRFunction function, IRBasicBlock phiBlock) {
        // 1. 收集 Phi 信息：Map<前驱块, List<并行拷贝对>>
        Map<IRBasicBlock, List<ParallelCopy>> copyMap = new HashMap<>();

        List<IRInstruction> instructions = phiBlock.getAllInstructions();
        for (IRInstruction instr : instructions) {
            if (!(instr instanceof PhiInstruction)) break;
            PhiInstruction phi = (PhiInstruction) instr;

            List<IRBasicBlock> predecessors = phi.getPredecessorBlocks();
            List<IRValue> values = phi.getIncomingValues();

            for (int i = 0; i < predecessors.size(); i++) {
                IRBasicBlock pred = predecessors.get(i);
                IRValue value = values.get(i);
                if (value != null) {
                    copyMap.computeIfAbsent(pred, k -> new ArrayList<>())
                            .add(new ParallelCopy(phi, value)); // dst=phi, src=incoming_value
                }
            }
        }

        // 2. 处理每一条前驱边，插入拷贝指令
        // 使用 KeySet 的副本，防止在 splitEdge 中修改图结构导致迭代器失效
        List<IRBasicBlock> predecessors = new ArrayList<>(copyMap.keySet());

        for (IRBasicBlock pred : predecessors) {
            List<ParallelCopy> parallelCopies = copyMap.get(pred);
            if (parallelCopies == null || parallelCopies.isEmpty()) continue;

            IRBasicBlock insertBlock;

            // --- 关键边分割 (Critical Edge Splitting) ---
            // 如果前驱有多个后继（说明是条件跳转），必须拆分边插入中间块
            // 否则拷贝指令的插入会影响到其他分支
            if (pred.getSuccessors().size() > 1) {
                insertBlock = splitEdge(function, pred, phiBlock);
            } else {
                insertBlock = pred;
            }

            // --- 并行拷贝转顺序 Move (处理依赖冲突) ---
            // 解决 a = b; b = a 或 b = a; a = new_val 这种依赖问题
            List<CopyInstruction> moves = convertParallelCopiesToMoves(function, insertBlock, parallelCopies);

            // 将生成的 Move 指令插入到 insertBlock 的跳转指令之前
            insertMovesToBlock(insertBlock, moves);
        }

        // 3. 移除 Phi 指令
        instructions.removeIf(instr -> instr instanceof PhiInstruction);
    }

    /**
     * 关键边分割：在 pred 和 succ 之间插入一个新的基本块
     * 逻辑：Pred -> MiddleBlock -> Succ
     */
    private IRBasicBlock splitEdge(IRFunction function, IRBasicBlock pred, IRBasicBlock succ) {
        // 【重要步骤 1】设置工厂的当前函数上下文
        // 因为 factory.createBasicBlock() 内部检查了 currentFunction 且会将块加入该函数
        factory.setCurrentFunction(function);

        // 【重要步骤 2】使用工厂创建基本块
        // 这会自动调用 getNextNameCounter() 获得如 %b105 的唯一标签
        // 且自动执行了 function.addBasicBlock(middleBlock)
        IRBasicBlock middleBlock = factory.createBasicBlock();

        // 【可选步骤】如果你想 fully use factory，可以 factory.setCurrentBasicBlock(middleBlock)
        // 然后调用 factory.createJump(succ)。这里为了保持你原有逻辑，手动添加指令也可以。
        middleBlock.addInstructionToTail(new JumpInstruction(middleBlock, succ));

        // 修改前驱块的跳转目标
        IRInstruction terminator = pred.getLastInstruction();
        boolean replaced = false;

        if (terminator instanceof BranchInstruction) {
            BranchInstruction br = (BranchInstruction) terminator;
            if (br.getTrueBranch() == succ) {
                br.setTrueBranch(middleBlock);
                replaced = true;
            }
            if (br.getFalseBranch() == succ) {
                br.setFalseBranch(middleBlock);
                replaced = true;
            }
        } else if (terminator instanceof JumpInstruction) {
            JumpInstruction jump = (JumpInstruction) terminator;
            if (jump.getTargetBlock() == succ) {
                jump.setTargetBlock(middleBlock);
                replaced = true;
            }
        }

        // 4. 维护前驱后继关系图 (CFG)
        if (replaced) {
            pred.getSuccessors().remove(succ);
            pred.addSuccessor(middleBlock);

            middleBlock.addPredecessor(pred);
            middleBlock.addSuccessor(succ);

            succ.getPredecessors().remove(pred);
            succ.addPredecessor(middleBlock);
        }

        return middleBlock;
    }

    /**
     * 将并行拷贝列表转换为无冲突的顺序 Move 指令列表
     * 解决 Read-After-Write (RAW) 依赖冲突，例如 loop 中的 b=a; a=a+10
     */
    private List<CopyInstruction> convertParallelCopiesToMoves(IRFunction function, IRBasicBlock block, List<ParallelCopy> copies) {
        List<CopyInstruction> moves = new ArrayList<>();

        // 创建工作列表副本，因为我们需要修改引用以解决冲突
        List<ParallelCopy> workList = new ArrayList<>();
        for (ParallelCopy pc : copies) {
            workList.add(new ParallelCopy(pc.dst, pc.src));
        }

        // 顺序处理每一个拷贝
        for (int i = 0; i < workList.size(); i++) {
            ParallelCopy current = workList.get(i);

            // 检查冲突：当前指令的 dst (即将被覆盖的值) 是否是后续指令的 src (需要读取的旧值)
            boolean conflict = false;
            for (int j = i + 1; j < workList.size(); j++) {
                if (workList.get(j).src.equals(current.dst)) {
                    conflict = true;
                    break;
                }
            }

            if (conflict) {
                // 冲突处理：
                // 1. 创建临时变量 temp
                // 2. 插入 temp = current.dst (备份旧值)
                // 3. 将后续所有引用 current.dst 的 src 替换为 temp
                // 4. 插入 current.dst = current.src (执行写入)

                // 创建临时变量，使用 block 的父函数作为 parent，类型与 dst 相同
                IRValue temp = new IRValue(function, "%phi_tmp_" + i, current.dst.getType());

                // 生成备份指令：temp = dst
                moves.add(new CopyInstruction(block, temp, current.dst));

                // 更新后续依赖：将引用 dst 的地方改为引用 temp
                for (int j = i + 1; j < workList.size(); j++) {
                    if (workList.get(j).src.equals(current.dst)) {
                        workList.get(j).src = temp;
                    }
                }

                // 生成原始拷贝：dst = src
                moves.add(new CopyInstruction(block, current.dst, current.src));

            } else {
                // 无冲突，直接生成：dst = src
                moves.add(new CopyInstruction(block, current.dst, current.src));
            }
        }

        return moves;
    }

    /**
     * 将生成的 Move 指令插入到基本块中（跳转指令之前）
     */
    private void insertMovesToBlock(IRBasicBlock block, List<CopyInstruction> moves) {
        List<IRInstruction> instrs = block.getAllInstructions();
        int insertPos = instrs.size();

        // 找到插入位置：在最后一条指令（通常是跳转/分支）之前
        if (!instrs.isEmpty()) {
            IRInstruction last = instrs.get(instrs.size() - 1);
            if (last instanceof JumpInstruction || last instanceof BranchInstruction) {
                insertPos = instrs.size() - 1;
            }
        }
        instrs.addAll(insertPos, moves);
    }

    /**
     * 简单的结构体用于暂存并行拷贝信息
     */
    private static class ParallelCopy {
        IRValue dst; // Phi 指令定义的结果
        IRValue src; // 来自前驱块的值

        ParallelCopy(IRValue dst, IRValue src) {
            this.dst = dst;
            this.src = src;
        }
    }
}