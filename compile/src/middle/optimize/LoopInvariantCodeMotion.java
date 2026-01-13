package middle.optimize;

import middle.llvm.type.IRType;
import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRFunction;
import middle.llvm.value.IRValue;
import middle.llvm.value.instruction.*;
import middle.llvm.value.constant.IRConstant;
import middle.llvm.value.constant.IntegerConstant;
import middle.llvm.type.IntegerType;

import java.util.*;

/**
 * 循环不变式代码外提 (Loop Invariant Code Motion, LICM)
 * 
 * 作用：
 * 将循环中计算结果不随循环迭代改变的指令移动到循环前置节点（Pre-Header），
 * 从而减少循环内部的计算量。
 */
public class LoopInvariantCodeMotion extends Optimizer {

    @Override
    public void optimize() {
        boolean hasChange = true;
        while (hasChange) {
            hasChange = false;
            // 每次大循环可能改变 CFG，所以重新 BuildCFG 最好在外部控制，
            // 但这里为了简化，我们在内部不重新 BuildCFG，而是假设 CFG 有效。
            // 如果 LoopInvariantCodeMotion 修改了 CFG（添加 PreHeader），
            // 它是局部修改，保持了支配性质（PreHeader 支配 Header），
            // 但为了保险，通常建议一次 Pass 只做一轮或依靠 Manager 重建 CFG。
            // 这里我们只做一轮遍历。
            
            for (IRFunction function : irModule.getFunctionDefinitions()) {
                if (function.getBasicBlocks().isEmpty()) continue;
                
                List<Loop> loops = findLoops(function);
                // 简单的循环顺序处理
                for (Loop loop : loops) {
                    if (runOnLoop(loop)) {
                        hasChange = true;
                    }
                }
            }
            
            // 如果发生了改变，我们这里 break，让 OptimizeManager 来处理重建 CFG
            // 或者是 LoopInvariantCodeMotion 自己维护 CFG？
            // 鉴于 BuildCFG 开销，我们最好一次性处理完。
            // 但添加 PreHeader 会改变 predecessors，可能影响后续循环识别。
            // 所以简单起见，每次只运行一次，依靠 OptimizeManager 的循环来多次执行（如果需要）。
            break; 
        }
    }

    private boolean runOnLoop(Loop loop) {
        boolean changed = false;
        
        // 1. 获取或创建 Pre-Header
        IRBasicBlock preHeader = getOrCreatePreHeader(loop);
        if (preHeader == null) {
            return false;
        }

        // 2. 识别并移动不变式
        // 这是一个迭代过程，因为移动一个不变式可能让依赖它的指令也变成不变式
        boolean loopChanged = true;
        while (loopChanged) {
            loopChanged = false;
            List<IRInstruction> invariants = findInvariants(loop);
            
            for (IRInstruction instr : invariants) {
                if (canMove(instr, loop)) {
                    moveInstruction(instr, preHeader);
                    changed = true;
                    loopChanged = true;
                }
            }
        }
        return changed;
    }

    // ==================== 循环识别 ====================

    private static class Loop {
        IRBasicBlock header;
        Set<IRBasicBlock> blocks = new HashSet<>();
        List<IRBasicBlock> backEdges = new ArrayList<>();

        Loop(IRBasicBlock header) {
            this.header = header;
            this.blocks.add(header);
        }
    }

    private List<Loop> findLoops(IRFunction function) {
        List<Loop> loops = new ArrayList<>();
        Map<IRBasicBlock, Loop> headerToLoop = new HashMap<>();

        for (IRBasicBlock block : function.getBasicBlocks()) {
            for (IRBasicBlock succ : block.getSuccessors()) {
                // 检查后继是否支配当前块（回边）
                if (block.getDominatedBy().contains(succ)) {
                    Loop loop = headerToLoop.computeIfAbsent(succ, Loop::new);
                    loop.backEdges.add(block);
                    fillLoopBody(loop, block);
                }
            }
        }
        
        loops.addAll(headerToLoop.values());
        return loops;
    }

    private void fillLoopBody(Loop loop, IRBasicBlock backEdgeNode) {
        if (loop.blocks.contains(backEdgeNode)) return;
        
        Queue<IRBasicBlock> workList = new LinkedList<>();
        workList.add(backEdgeNode);
        loop.blocks.add(backEdgeNode);

        while (!workList.isEmpty()) {
            IRBasicBlock curr = workList.poll();
            if (curr == loop.header) continue;

            for (IRBasicBlock pred : curr.getPredecessors()) {
                if (!loop.blocks.contains(pred)) {
                    loop.blocks.add(pred);
                    workList.add(pred);
                }
            }
        }
    }

    // ==================== Pre-Header 管理 ====================

    private IRBasicBlock getOrCreatePreHeader(Loop loop) {
        List<IRBasicBlock> outsidePredecessors = new ArrayList<>();
        for (IRBasicBlock pred : loop.header.getPredecessors()) {
            if (!loop.blocks.contains(pred)) {
                outsidePredecessors.add(pred);
            }
        }

        if (outsidePredecessors.isEmpty()) return null; // 死循环或不可达

        // 如果只有一个外部前驱，且该前驱只有一条出边（指向header），那么它就是天然的 Pre-Header
        if (outsidePredecessors.size() == 1) {
            IRBasicBlock pred = outsidePredecessors.get(0);
            if (pred.getSuccessors().size() == 1) {
                return pred;
            }
        }

        return createPreHeader(loop, outsidePredecessors);
    }

    private IRBasicBlock createPreHeader(Loop loop, List<IRBasicBlock> outsidePreds) {
        IRFunction function = (IRFunction) loop.header.getContainer(); // 假设 getContainer 返回 Function
        // 如果 getContainer 返回 null 或不对，尝试从 blocks 获取
        if (function == null && !loop.blocks.isEmpty()) {
             // IRBasicBlock 应该有 getParent() ? IRValue 只有 getContainer
             // 实际上 IRBasicBlock 的 containerValue 是 IRFunction
             function = (IRFunction) loop.header.getContainer();
        }
        
        // 创建新的基本块
        // 注意：nameCounter 只是用于生成名字，我们这里用 list size 模拟
        IRBasicBlock preHeader = new IRBasicBlock(function, function.getBasicBlocks().size());
        preHeader.setName("%preheader_b" + loop.header.getName().substring(2)); // 优化命名
        
        // 插入到函数块列表中，在 header 之前
        int headerIndex = function.getBasicBlocks().indexOf(loop.header);
        if (headerIndex != -1) {
            function.getBasicBlocks().add(headerIndex, preHeader);
        } else {
            function.getBasicBlocks().add(preHeader);
        }

        // 1. 修正 CFG 连接：将 outsidePreds 的跳转目标从 header 改为 preHeader
        for (IRBasicBlock pred : outsidePreds) {
            IRInstruction terminator = pred.getLastInstruction();
            if (terminator instanceof BranchInstruction) {
                BranchInstruction br = (BranchInstruction) terminator;
                if (br.getTrueBranch() == loop.header) br.setTrueBranch(preHeader);
                if (br.getFalseBranch() == loop.header) br.setFalseBranch(preHeader);
            } else if (terminator instanceof JumpInstruction) {
                JumpInstruction jmp = (JumpInstruction) terminator;
                if (jmp.getTargetBlock() == loop.header) jmp.setTargetBlock(preHeader);
            }
            
            // 维护前驱后继关系
            pred.removeSuccessor(loop.header);
            pred.addSuccessor(preHeader);
            preHeader.addPredecessor(pred);
            loop.header.removePredecessor(pred);
        }
        
        // 2. PreHeader 跳转到 Header
        JumpInstruction jump = new JumpInstruction(preHeader, loop.header);
        preHeader.addInstructionToTail(jump);
        preHeader.addSuccessor(loop.header);
        loop.header.addPredecessor(preHeader);

        // 3. 处理 Phi 指令
        List<PhiInstruction> phis = loop.header.getPhiInstructions();
        // 我们需要修改这些 Phi，但不能在遍历时修改 list，所以先复制一份
        // 注意：getPhiInstructions 返回的是新 list，所以安全
        
        for (PhiInstruction phi : phis) {
            List<IRValue> outVals = new ArrayList<>();
            List<IRBasicBlock> outBlks = new ArrayList<>();
            List<IRValue> inVals = new ArrayList<>();
            List<IRBasicBlock> inBlks = new ArrayList<>();

            // 遍历 Phi 的 incoming values
            // PhiInstruction 提供了 getPredecessorBlocks 和 getIncomingValue
            List<IRBasicBlock> incomingBlocks = phi.getPredecessorBlocks();
            for (IRBasicBlock blk : incomingBlocks) {
                IRValue val = phi.getIncomingValue(blk);
                // 注意：此时 outsidePreds 已经不再是 header 的 predecessor 了，
                // 但 phi 中的 block 引用还是旧的 block。
                // 我们根据 block 是否在 outsidePreds 中判断
                if (outsidePreds.contains(blk)) {
                    outVals.add(val);
                    outBlks.add(blk);
                } else {
                    inVals.add(val);
                    inBlks.add(blk);
                }
            }
            
            if (outVals.isEmpty()) continue; // 理论上不应该发生，除非死代码

            IRValue valForHeader;
            if (outVals.size() == 1 && outsidePreds.size() == 1) {
                valForHeader = outVals.get(0);
            } else {
                // 在 PreHeader 创建新的 Phi
                PhiInstruction prePhi = new PhiInstruction(preHeader, phi.getType(), new ArrayList<>());
                for (int i = 0; i < outVals.size(); i++) {
                    prePhi.fillIncomingValue(outVals.get(i), outBlks.get(i));
                }
                preHeader.addInstructionToHead(prePhi);
                valForHeader = prePhi;
            }

            // 构造新的 Header Phi
            // 注意：我们创建一个新的 Phi 替换旧的，因为旧的 Phi 内部 map 很难清理干净
            PhiInstruction newHeaderPhi = new PhiInstruction(loop.header, phi.getType(), new ArrayList<>());
            for (int i = 0; i < inVals.size(); i++) {
                newHeaderPhi.fillIncomingValue(inVals.get(i), inBlks.get(i));
            }
            newHeaderPhi.fillIncomingValue(valForHeader, preHeader);
            
            // 替换旧 Phi 的使用
            phi.replaceAllUsesWith(newHeaderPhi);
            
            // 从基本块指令列表中移除旧 Phi，添加新 Phi
            // IRBasicBlock 使用 LinkedList 存储 instructions
            // 我们需要手动移除
            loop.header.getAllInstructions().remove(phi);
            loop.header.addInstructionToHead(newHeaderPhi);
        }

        return preHeader;
    }

    // ==================== 不变式识别 ====================

    private List<IRInstruction> findInvariants(Loop loop) {
        List<IRInstruction> invariants = new ArrayList<>();
        Set<IRInstruction> definedInLoop = new HashSet<>();
        
        // 预处理：收集循环内定义的指令
        for (IRBasicBlock block : loop.blocks) {
            for (IRInstruction instr : block.getAllInstructions()) {
                definedInLoop.add(instr);
            }
        }
        
        for (IRBasicBlock block : loop.blocks) {
            for (IRInstruction instr : block.getAllInstructions()) {
                // 必须在循环内（没被移走），且是 invariant
                if (definedInLoop.contains(instr) && isInvariant(instr, loop, definedInLoop)) {
                    invariants.add(instr);
                }
            }
        }
        return invariants;
    }

    private boolean isInvariant(IRInstruction instr, Loop loop, Set<IRInstruction> definedInLoop) {
        if (!isSafeToSpeculate(instr)) return false;

        for (IRValue operand : instr.getAllOperands()) {
            if (!isLoopInvariant(operand, definedInLoop)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSafeToSpeculate(IRInstruction instr) {
        // 允许移动的指令
        if (instr instanceof BinaryOperationInstruction) {
            BinaryOperationInstruction binOp = (BinaryOperationInstruction) instr;
            BinaryOperationInstruction.BinaryOperator op = binOp.getOperator();
            if (op == BinaryOperationInstruction.BinaryOperator.SDIV || op == BinaryOperationInstruction.BinaryOperator.SREM) {
                IRValue rhs = binOp.getRightOperand();
                if (rhs instanceof IRConstant) {
                    if (rhs instanceof middle.llvm.value.constant.IntegerConstant) {
                        return ((middle.llvm.value.constant.IntegerConstant) rhs).getConstantValue() != 0;
                    }
                    return true; // 其他常量类型视为安全
                }
                return false;
            }
            return true;
        }
        
        return instr instanceof CompareInstruction ||
               instr instanceof TruncateInstruction ||
               instr instanceof ZeroExtendInstruction ||
               instr instanceof GetElementPtrInstruction;
    }

    private boolean isLoopInvariant(IRValue value, Set<IRInstruction> definedInLoop) {
        // 常量、全局变量、参数等不在 definedInLoop 中，视为不变
        // 如果是指令且在 definedInLoop 中，则它是循环内定义的，不是不变式
        // (除非它已经被标记为 invariant? 我们的 findInvariants 是批量处理的，
        //  在一次 find 中，依赖必须已经在之前的迭代中被移出 definedInLoop 或者是外部的。
        //  Wait, definedInLoop 是静态的吗？
        //  LICM 是迭代的。如果我们把 A 移到 preHeader，它就不在 loop.blocks 里了。
        //  所以下一轮迭代，A 就不在 definedInLoop 里了。
        //  所以 isLoopInvariant Check 是正确的。)
        
        if (value instanceof IRInstruction) {
            return !definedInLoop.contains(value);
        }
        return true;
    }

    // ==================== 代码移动 ====================

    private boolean canMove(IRInstruction instr, Loop loop) {
        // 既然我们只移动 safeToSpeculate 的指令（无副作用，无异常），
        // 支配性要求可以放宽。
        // 只要它是 invariant，且是 safe 的，就可以移动到 preHeader。
        // 唯一需要注意的是 SSA 约束，但移动到 preHeader (dominator) 自然满足 def-before-use。
        return true;
    }

    private void moveInstruction(IRInstruction instr, IRBasicBlock preHeader) {
        // 1. 从原块移除
        IRBasicBlock parentBlock = (IRBasicBlock) instr.getContainer();
        if (parentBlock != null) {
            parentBlock.getAllInstructions().remove(instr);
        }
        
        // 2. 添加到 PreHeader (终结指令之前)
        // PreHeader 此时最后一条应该是 Jump loop.header
        IRInstruction terminator = preHeader.getLastInstruction();
        
        // 插入到 terminator 之前
        // LinkedList 没有 insertBeforeNode，只能通过 index 或者 iterator
        // 简单做法：remove terminator -> add instr -> add terminator
        preHeader.getAllInstructions().removeLast();
        preHeader.addInstructionToTail(instr);
        preHeader.addInstructionToTail(terminator);
        
        // 3. 更新 instr 的 parent
        instr.setContainer(preHeader);
    }


    private Set<IRInstruction> collectDefined(Loop loop) {
        Set<IRInstruction> defined = new HashSet<>();
        for (IRBasicBlock b : loop.blocks) {
            defined.addAll(b.getAllInstructions());
        }
        return defined;
    }

    private void insertBeforeTerminator(IRBasicBlock block, IRInstruction instr) {
        IRInstruction terminator = block.getLastInstruction();
        if (terminator != null && terminator.isTerminatorInstruction()) {
            block.getAllInstructions().removeLast();
            block.addInstructionToTail(instr);
            block.addInstructionToTail(terminator);
        } else {
            block.addInstructionToTail(instr);
        }
        instr.setContainer(block);
    }

    @Override
    public String OptimizerName() {
        return "LoopInvariantCodeMotion";
    }
}
