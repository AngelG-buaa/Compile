package middle.optimize;

import middle.llvm.value.constant.IntegerConstant;
import middle.llvm.value.instruction.BranchInstruction;
import middle.llvm.value.instruction.CallInstruction;
import middle.llvm.value.instruction.IRInstruction;
import middle.llvm.value.instruction.JumpInstruction;
import middle.llvm.value.instruction.ReturnInstruction;
import middle.llvm.value.instruction.StoreInstruction;
import middle.llvm.value.instruction.PhiInstruction;
import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRFunction;
import middle.llvm.value.IRValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.List;

/**
 * 死代码消除（DCE）与结构简化
 *
 * 目标：清理程序中“对外表现无影响”的代码与结构，使 IR 更简洁、可优化。
 *
 * 包含的处理：
 * - 无用函数删除：从 `main` 出发未被调用，且无副作用的函数（副作用会自上而下传播）。
 * - 不可达基本块删除：从入口不可到达的块（配合 CFG/URC，保持图连通性合理）。
 * - 无用指令删除：结果无人使用且无副作用的指令（以 `isCriticalInstr` 判定副作用与必要性）。
 * - 无用 Phi 删除：只有一个 incoming 的 Phi，直接用该值替换 Phi 的所有使用并移除。
 * - 死分支（可选）：条件恒真/恒假的分支可折叠（本代码中保留接口，默认不启用）。
 * - 基本块合并：可顺序连接且合并后不破坏 CFG 的相邻块，进行拼接以减少跳转。
 *
 * 副作用建模：
 * - 标准库函数（无函数体、IO 类）视为有副作用；若某用户函数调用了带副作用的函数，则该用户函数也有副作用。
 * - `store` 指令被视为副作用；因此包含 `store` 的函数不会被当作“无用函数”。
 *
 * 迭代策略：
 * - DCE 通常需要多轮迭代以达到不动点：删除不可达块后可能产生新的无用指令或可合并块，故采用上限迭代并在稳定后退出。
 *
 * 示例（Phi 简化）：
 *
 *  优化前：
 *  ```llvm
 *  b3:
 *    %p = phi i32 [42, %b1]
 *    %x = add i32 %p, 1
 *    ret i32 %x
 *  ```
 *
 *  优化后：
 *  ```llvm
 *  b3:
 *    %x = add i32 42, 1
 *    ret i32 %x
 *    ; %p 被删除，因为只有一个 incoming
 *  ```
 *
 * 示例（合并基本块）：
 *
 *  优化前：
 *  ```llvm
 *  b1:
 *    %v = add i32 %a, %b
 *    br label %b2
 *
 *  b2:
 *    %w = mul i32 %v, 10
 *    br label %b3
 *  ```
 *
 *  优化后：
 *  ```llvm
 *  b1:
 *    %v = add i32 %a, %b
 *    %w = mul i32 %v, 10
 *    br label %b3
 *    ; b2 与 b1 合并，减少一次跳转
 *  ```
 *
 * 示例（不可达删除）：
 *
 *  优化前：
 *  ```llvm
 *  entry:
 *    br label %live
 *
 *  dead:
 *    %x = add i32 1, 2
 *    ret i32 %x
 *
 *  live:
 *    ret i32 0
 *  ```
 *
 *  优化后：
 *  ```llvm
 *  entry:
 *    ret i32 0
 *    ; 块 dead 被删除，因为不可达
 *  ```
 */
public class RemoveDeadCode extends Optimizer {
    // 记录调用了哪些函数
    private final HashMap<IRFunction, HashSet<IRFunction>> calleeMap;
    // 记录被哪些函数调用
    private final HashMap<IRFunction, HashSet<IRFunction>> callerMap;
    // 副作用：有IO操作
    private final HashSet<IRFunction> sideEffectFunctions;

    public RemoveDeadCode() {
        this.calleeMap = new HashMap<>();
        this.callerMap = new HashMap<>();
        this.sideEffectFunctions = new HashSet<>();
    }

    @Override
    public void optimize() {
        final int MAX_ITERATIONS = 1000;
        int iteration = 0;

        while (iteration < MAX_ITERATIONS) {
            iteration++;
            // System.out.println("Dead code elimination iteration: " + iteration);

            this.buildFunctionCallMap();

            boolean functionsRemoved = !removeUselessFunction();
            boolean blocksRemoved = !removeUselessBlock();
            boolean codeRemoved = !removeUselessCode();
            boolean phiRemoved = !removeUselessPhi();
            // boolean branchesRemoved = removeDeadBranch();
            boolean blocksMerged = !mergeBlock();

//            System.out.println("  Functions removed: " + functionsRemoved);
//            System.out.println("  Blocks removed: " + blocksRemoved);
//            System.out.println("  Code removed: " + codeRemoved);
//            System.out.println("  Phi removed: " + phiRemoved);
//            // System.out.println("  Branches removed: " + !branchesRemoved);
//            System.out.println("  Blocks merged: " + blocksMerged);

            if (functionsRemoved && blocksRemoved && codeRemoved && phiRemoved /*&& branchesRemoved*/ && blocksMerged) {
                break;
            }
        }

        if (iteration >= MAX_ITERATIONS) {
            System.out.println("Warning: Dead code elimination reached maximum iterations (" + MAX_ITERATIONS + "), stopping to prevent infinite loop.");
        }
    }

    private void buildFunctionCallMap() {
        // 进行初始化
        this.calleeMap.clear();
        this.callerMap.clear();
        this.sideEffectFunctions.clear();

        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            this.calleeMap.put(irFunction, new HashSet<>());
            this.callerMap.put(irFunction, new HashSet<>());
        }

        // 从main函数开始进行dfs
        IRFunction mainFunction = irModule.getMainFunction();
        if (mainFunction != null) {
            // main函数默认被认为有副作用，防止被删除
            this.sideEffectFunctions.add(mainFunction);
            this.dfsSideFunction(mainFunction, new HashSet<>());
        }
    }

    private void dfsSideFunction(IRFunction visitFunction, HashSet<IRFunction> visited) {
        if (visited.contains(visitFunction)) {
            return;
        }
        visited.add(visitFunction);

        for (IRBasicBlock irBasicBlock : visitFunction.getBasicBlocks()) {
            LinkedList<IRInstruction> instructions = irBasicBlock.getAllInstructions();
            for (IRInstruction instr : instructions) {
                // 函数调用
                if (instr instanceof CallInstruction callInstr) {
                    // 优雅识别副作用：直接基于IRFunction对象与库函数属性
                    IRValue calleeVal = callInstr.getAllOperands().get(0);
                    if (calleeVal instanceof IRFunction calleeFunc) {
                        // 标准库函数（仅声明，无函数体）视为具有副作用（SysY的IO）
                        if (calleeFunc.isLibraryFunction()) {
                            this.sideEffectFunctions.add(visitFunction);
                        } else {
                            // 普通用户函数：纳入调用图并传播副作用属性
                            this.dfsSideFunction(calleeFunc, visited);
                            this.calleeMap.get(visitFunction).add(calleeFunc);
                            this.callerMap.get(calleeFunc).add(visitFunction);
                            if (this.sideEffectFunctions.contains(calleeFunc)) {
                                this.sideEffectFunctions.add(visitFunction);
                            }
                        }
                    }
                }
                // 存储操作
                else if (instr instanceof StoreInstruction) {
                    this.sideEffectFunctions.add(visitFunction);
                }
            }
        }
    }

    private boolean isIOFunction(String functionName) {
        return functionName.equals("getint") || functionName.equals("getchar") ||
                functionName.equals("putint") || functionName.equals("putch") ||
                functionName.equals("putstr");
    }

    private IRFunction findFunctionByName(String name) {
        for (IRFunction function : irModule.getFunctionDefinitions()) {
            if (function.getName().equals(name)) {
                return function;
            }
        }
        return null;
    }

    // 删除无用函数
    private boolean removeUselessFunction() {
        boolean finished = false;

        List<IRFunction> usefulFunctions = new LinkedList<>();

        Iterator<IRFunction> iterator = irModule.getFunctionDefinitions().iterator();
        while (iterator.hasNext()) {
            IRFunction irFunction = iterator.next();
            // 无人调用，删除：即使有sideEffect也没关系
            if (!"@main".equals(irFunction.getName()) && this.callerMap.get(irFunction).isEmpty()) {
                iterator.remove();
                finished = true;
            } else {
                usefulFunctions.add(irFunction);
            }
        }

        irModule.setFunctionDefinitions(usefulFunctions);

        return finished;
    }

    // 删除无用基本块
    private boolean removeUselessBlock() {
        boolean finished = false;
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            Iterator<IRBasicBlock> iterator = irFunction.getBasicBlocks().iterator();
            while (iterator.hasNext()) {
                IRBasicBlock visitBlock = iterator.next();
                // 不可达块，删除
                if (visitBlock.getPredecessors().isEmpty() && !visitBlock.equals(irFunction.getEntryBlock())) {
                    // 改变关系
                    for (IRBasicBlock nextBlock : visitBlock.getSuccessors()) {
                        nextBlock.getPredecessors().remove(visitBlock);
                        // 消除phi
                        for (IRInstruction nextInstr : nextBlock.getAllInstructions()) {
                            if (nextInstr instanceof PhiInstruction phiInstr) {
                                phiInstr.removeIncomingBlock(visitBlock);
                            }
                        }
                    }
                    // 删除指令
                    for (IRInstruction instr : visitBlock.getAllInstructions()) {
                        instr.clearAllOperands();
                    }

                    finished = true;
                    iterator.remove();
                }
            }
        }
        return finished;
    }

    // 删除无用代码
    private boolean removeUselessCode() {
        boolean finished = false;
        HashSet<IRInstruction> activeInstrSet = this.getActiveInstrSet();

        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            for (IRBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                Iterator<IRInstruction> iterator = irBasicBlock.getAllInstructions().iterator();
                while (iterator.hasNext()) {
                    IRInstruction instr = iterator.next();
                    // 仅当指令不在活跃集合且没有任何用户时才安全删除
                    if (!activeInstrSet.contains(instr) && instr.getUseList().isEmpty()) {
                        instr.clearAllOperands();
                        iterator.remove();
                        finished = true;
                    }
                }
            }
        }

        return finished;
    }

    private HashSet<IRInstruction> getActiveInstrSet() {
        HashSet<IRInstruction> activeInstrSet = new HashSet<>();
        Stack<IRInstruction> todoInstrStack = new Stack<>();

        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            for (IRBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                for (IRInstruction instr : irBasicBlock.getAllInstructions()) {
                    if (this.isCriticalInstr(instr)) {
                        todoInstrStack.push(instr);
                    }
                }
            }
        }

        while (!todoInstrStack.isEmpty()) {
            IRInstruction todoInstr = todoInstrStack.pop();
            if (activeInstrSet.contains(todoInstr)) {
                continue;
            }
            activeInstrSet.add(todoInstr);

            for (IRValue useValue : todoInstr.getAllOperands()) {
                if (useValue instanceof IRInstruction useInstr) {
                    if (!activeInstrSet.contains(useInstr)) {
                        todoInstrStack.push(useInstr);
                    }
                }
            }
        }

        return activeInstrSet;
    }

    private boolean isCriticalInstr(IRInstruction instr) {
        if (instr instanceof ReturnInstruction ||
                instr instanceof BranchInstruction ||
                instr instanceof JumpInstruction ||
                instr instanceof StoreInstruction) {
            return true;
        }

        if (instr instanceof CallInstruction callInstr) {
            IRValue calleeVal = callInstr.getAllOperands().get(0);
            if (calleeVal instanceof IRFunction calleeFunc) {
                // 库函数调用必为关键（IO）；用户函数则取决于副作用传播结果
                if (calleeFunc.isLibraryFunction()) {
                    return true;
                }
                return this.sideEffectFunctions.contains(calleeFunc);
            }
            // 无法识别被调对象类型时保守处理为非关键
            return false;
        }

        return false;
    }

    private boolean removeUselessPhi() {
        boolean finished = false;
        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            for (IRBasicBlock irBasicBlock : irFunction.getBasicBlocks()) {
                Iterator<IRInstruction> iterator = irBasicBlock.getAllInstructions().iterator();
                while (iterator.hasNext()) {
                    IRInstruction instr = iterator.next();
                    if (!(instr instanceof PhiInstruction phiInstr)) {
                        continue;
                    }

                    List<IRValue> phiValueList = phiInstr.getAllOperands();
                    // 仅当Phi只有一个非空输入时进行安全替换
                    if (phiValueList.size() == 1 && phiValueList.get(0) != null) {
                        finished = true;
                        phiInstr.replaceAllUsesWith(phiValueList.get(0));
                        phiInstr.clearAllOperands();
                        iterator.remove();
                    }
                }
            }
        }

        return finished;
    }

    private boolean mergeBlock() {
        boolean finished = false;

        for (IRFunction irFunction : irModule.getFunctionDefinitions()) {
            Iterator<IRBasicBlock> iterator = irFunction.getBasicBlocks().iterator();
            while (iterator.hasNext()) {
                IRBasicBlock irBasicBlock = iterator.next();
                if (this.canMergeBlock(irBasicBlock)) {
                    finished = true;
                    IRBasicBlock beforeBlock = irBasicBlock.getPredecessors().iterator().next();

                    // 移除beforeBlock的最后一条跳转指令
                    beforeBlock.getAllInstructions().removeLast();

                    // 将当前块的所有指令添加到前驱块
                    for (IRInstruction instr : irBasicBlock.getAllInstructions()) {
                        beforeBlock.addInstructionToTail(instr);
                        instr.setContainer(beforeBlock);
                    }

                    // 更新后继关系
                    beforeBlock.getSuccessors().clear();
                    beforeBlock.getSuccessors().addAll(irBasicBlock.getSuccessors());

                    // 更新后继块的前驱关系
                    for (IRBasicBlock successor : irBasicBlock.getSuccessors()) {
                        successor.getPredecessors().remove(irBasicBlock);
                        successor.getPredecessors().add(beforeBlock);

                        // 更新Phi指令中的块引用
                        for (IRInstruction instr : successor.getAllInstructions()) {
                            if (instr instanceof PhiInstruction phiInstr) {
                                phiInstr.replaceIncomingBlock(irBasicBlock, beforeBlock);
                            }
                        }
                    }

                    iterator.remove();
                }
            }
        }

        return finished;
    }

    private boolean canMergeBlock(IRBasicBlock visitBlock) {
        Set<IRBasicBlock> predecessors = visitBlock.getPredecessors();
        if (predecessors.size() == 1) {
            IRBasicBlock beforeBlock = predecessors.iterator().next();
            // 前后对接上，则可以合并
            Set<IRBasicBlock> successors = beforeBlock.getSuccessors();
            return successors.size() == 1 &&
                    successors.iterator().next() == visitBlock;
        }
        return false;
    }

    @Override
    public String OptimizerName() {
        return "RemoveDeadCode";
    }
}
