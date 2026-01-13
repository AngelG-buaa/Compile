package middle.optimize;

import middle.llvm.UseDefChain;
import middle.llvm.type.PointerType;
import middle.llvm.type.IRType;
import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRValue;
import middle.llvm.value.constant.IntegerConstant;
import middle.llvm.value.instruction.*;
import middle.llvm.type.IntegerType;

import java.util.*;

/**
 * 插入并填充 Phi 节点（Mem2Reg 核心）
 *
 * 职责：针对单个 `alloca`（非数组局部变量），在其支配边界插入对应的 `phi`，并通过 DFS 重命名与值栈实现
 * load/store 的消除与值替换，最终将内存上的临时变量转换为 SSA 寄存器值。
 *
 * 核心思路：
 * - 构建该 `alloca` 的定义-使用集合（store 属于 define，load 属于 use，phi 同时属于 define/use）。
 * - 在所有定义点的支配边界插入 phi（每个块最多一个、只针对该变量）。
 * - 以函数入口为根沿支配树 DFS：维护“可见值”栈，遇到 store/phi 将值压栈，遇到 load 用当前可见值替换并删除。
 * - 为每个后继块的块首 phi 精准匹配本变量并填充来自当前块的 incoming 值。
 *
 * 重要细节：
 * - “块首多 Phi”场景：本实现会从块头连续扫描所有 Phi，并仅对属于当前 `alloca` 的 Phi 填充（通过 `useInstrs` 识别归属），
 *   而不是假定“第一个指令就是本变量的 Phi”。这在同一块存在多个不同变量的 Phi 时尤其重要。
 * - 可见性与支配约束：通过 `peekVisibleForBlock` 选择“对当前边可见”的最新值，避免引用未定义或不支配的 SSA 名（如早先的 `%phi298` 问题）。
 * - 默认值类型：`targetValueType` 为指针所指向的真实类型；当值栈为空或出现非法引用时，安全回退到对应类型的 0 常量。
 *
 * 示例（mem2reg 前后，对应单变量多分支汇合）：
 *
 *  优化前：
 *  ```llvm
 *  entry:
 *    %a = alloca i32
 *    store i32 10, i32* %a
 *    br label %b1
 *
 *  b1:
 *    %cond = icmp sgt i32 %x, 0
 *    br i1 %cond, label %b2, label %b3
 *
 *  b2:
 *    store i32 20, i32* %a
 *    br label %b3
 *
 *  b3:
 *    %v = load i32, i32* %a
 *    ret i32 %v
 *  ```
 *
 *  优化后：
 *  ```llvm
 *  entry:
 *    br label %b1
 *
 *  b1:
 *    %cond = icmp sgt i32 %x, 0
 *    br i1 %cond, label %b2, label %b3
 *
 *  b2:
 *    br label %b3
 *
 *  b3:
 *    %a.phi = phi i32 [20, %b2], [10, %b1]
 *    ret i32 %a.phi
 *  ```
 *
 *  多 Phi 场景（块首存在多个变量的 Phi，本实现逐个扫描并精准匹配当前 alloca 的 Phi）：
 *  ```llvm
 *  b_join:
 *    %x.phi = phi i32 [ ..., %pred1 ], [ ..., %pred2 ]
 *    %y.phi = phi i32 [ ..., %pred1 ], [ ..., %pred2 ]
 *    ; 之后为非 Phi 指令，扫描到此停止
 *  ```
 */
public class InsertPhi {
    private final AllocaInstruction allocaInstruction;
    private final IRBasicBlock entryBlock;
    private final HashSet<IRInstruction> defineInstrs;
    private final HashSet<IRInstruction> useInstrs;
    private final LinkedHashSet<IRBasicBlock> defineBlocks;
    private final LinkedHashSet<IRBasicBlock> useBlocks;
    private Stack<IRValue> valueStack;
    // 该 alloca 对应的实际值类型（指针所指向的类型），用于类型一致的默认值
    private final IRType targetValueType;

    public InsertPhi(AllocaInstruction allocaInstruction, IRBasicBlock entryBlock) {
        this.allocaInstruction = allocaInstruction;
        this.entryBlock = entryBlock;
        this.defineInstrs = new HashSet<>();
        this.useInstrs = new HashSet<>();
        this.defineBlocks = new LinkedHashSet<>();
        this.useBlocks = new LinkedHashSet<>();
        this.valueStack = new Stack<>();
        this.targetValueType = ((PointerType) this.allocaInstruction.getType()).getPointeeType();
    }

    /**
     * 执行Phi节点插入的完整过程
     */
    public void addPhi() {
        // 分析该allocaInstruction的define和use关系
        this.buildDefineUseRelationship();
        // 找出需要添加phi指令的基本块，并添加phi
        this.insertPhiToBlock();
        // 通过DFS进行重命名，同时将相关的allocate, store, load指令删除
        this.convertLoadStore(this.entryBlock);
        this.pruneTrivialPhis(this.entryBlock);
    }

    /**
     * 构建定义-使用关系
     *
     * 分析alloca指令的所有使用者：
     * - LoadInstruction被视为使用（use）
     * - StoreInstruction被视为定义（define）
     */
    private void buildDefineUseRelationship() {
        // 所有使用该allocate的user
        for (UseDefChain useChain : this.allocaInstruction.getUseList()) {
            if (useChain.user() instanceof IRInstruction userInstr) {
                // load关系为use关系
                if (userInstr instanceof LoadInstruction) {
                    this.addUseInstr(userInstr);
                }
                // store关系为define关系
                else if (userInstr instanceof StoreInstruction) {
                    this.addDefineInstr(userInstr);
                }
            }
        }
    }

    /**
     * 添加定义指令
     */
    private void addDefineInstr(IRInstruction instr) {
        this.defineInstrs.add(instr);
        IRBasicBlock parentBlock = (IRBasicBlock) instr.getContainer();
        this.defineBlocks.add(parentBlock);
    }

    /**
     * 添加使用指令
     */
    private void addUseInstr(IRInstruction instr) {
        this.useInstrs.add(instr);
        IRBasicBlock parentBlock = (IRBasicBlock) instr.getContainer();
        this.useBlocks.add(parentBlock);
    }

    /**
     * 在支配边界上插入Phi节点
     *
     * 使用支配边界算法确定需要插入Phi节点的位置
     */
    private void insertPhiToBlock() {
        // 需要添加phi的基本块的集合
        HashSet<IRBasicBlock> addedPhiBlocks = new HashSet<>();

        // 定义变量的基本块的集合
        Stack<IRBasicBlock> defineBlockStack = new Stack<>();
        for (IRBasicBlock defineBlock : this.defineBlocks) {
            defineBlockStack.push(defineBlock);
        }

        while (!defineBlockStack.isEmpty()) {
            IRBasicBlock defineBlock = defineBlockStack.pop();
            // 遍历当前基本块的所有支配边界
            for (IRBasicBlock frontierBlock : defineBlock.getDominanceFrontier()) {
                // 如果支配边界块不在已添加Phi的集合中，则添加Phi节点
                if (!addedPhiBlocks.contains(frontierBlock)) {
                    this.insertPhiInstr(frontierBlock);
                    addedPhiBlocks.add(frontierBlock);
                    // phi也进行定义变量
                    if (!this.defineBlocks.contains(frontierBlock)) {
                        defineBlockStack.push(frontierBlock);
                    }
                }
            }
        }
    }

    /**
     * 在指定基本块插入Phi指令
     */
    private void insertPhiInstr(IRBasicBlock irBasicBlock) {
        if (irBasicBlock.getPredecessors().size() <= 1) {
            return;
        }
        // 获取alloca的目标类型
        PointerType pointerType = (PointerType) this.allocaInstruction.getType();

        // 创建Phi指令，使用前驱基本块列表
        PhiInstruction phiInstr = new PhiInstruction(
                irBasicBlock,
                pointerType.getPointeeType(),
                new ArrayList<>(irBasicBlock.getPredecessors())
        );

        // 将Phi指令插入到基本块的开头
        irBasicBlock.addInstructionToHead(phiInstr);

        // phi既是define，又是use
        this.useInstrs.add(phiInstr);
        this.defineInstrs.add(phiInstr);
    }

    /**
     * 转换load/store操作为直接值传递
     *
     * 通过DFS遍历支配树，重命名变量并消除内存操作
     */
    private void convertLoadStore(IRBasicBlock renameBlock) {
        @SuppressWarnings("unchecked")
        final Stack<IRValue> stackCopy = (Stack<IRValue>) this.valueStack.clone();

        // 移除与当前allocate相关的全部的load、store指令
        this.removeBlockLoadStore(renameBlock);
        // 遍历后继基本块，将最新的define填充进每个后继块的第一个phi指令中
        this.convertPhiValue(renameBlock);
        // 对支配块进行dfs
        for (IRBasicBlock dominateBlock : renameBlock.getImmediateDominated()) {
            this.convertLoadStore(dominateBlock);
        }
        // 恢复栈
        this.valueStack = stackCopy;
    }

    /**
     * 移除基本块中的load/store指令
     */
    private void removeBlockLoadStore(IRBasicBlock visitBlock) {
        Iterator<IRInstruction> iterator = visitBlock.getAllInstructions().iterator();
        IRValue current = null;
        while (iterator.hasNext()) {
            IRInstruction instr = iterator.next();
            // store（仅保留最后一次）
            if (instr instanceof StoreInstruction storeInstr && this.defineInstrs.contains(instr)) {
                current = storeInstr.getValueOperand();
                iterator.remove();
                continue;
            }
            // load（使用当前可见值或最近一次store的值）
            if (!(instr instanceof PhiInstruction) && this.useInstrs.contains(instr)) {
                IRValue replacement = current != null ? current : this.peekVisibleForBlock(visitBlock);
                instr.replaceAllUsesWith(replacement);
                iterator.remove();
                continue;
            }
            // phi（更新当前值为phi）
            if (instr instanceof PhiInstruction && this.defineInstrs.contains(instr)) {
                current = instr;
                this.valueStack.push(instr);
                continue;
            }
            // 当前分析的allocate：使用mem2reg后不需要allocate
            if (instr == this.allocaInstruction) {
                iterator.remove();
            }
        }
        if (current != null) {
            this.valueStack.push(current);
        }
    }

    /**
     * 转换Phi节点的值
     */
    private void convertPhiValue(IRBasicBlock visitBlock) {
        for (IRBasicBlock nextBlock : visitBlock.getSuccessors()) {
            // 旧实现只检查“第一条指令”，当后继块存在多个 Phi（对应不同 alloca）时，会漏填本变量的 Phi。
            // 新实现：从块头连续扫描所有 Phi，精准匹配属于本次 alloca 的 Phi，再进行填充。
            IRValue latest = this.peekVisibleForBlock(visitBlock);
            for (IRInstruction instr : nextBlock.getAllInstructions()) {
                if (!(instr instanceof PhiInstruction)) {
                    // Phi 按约定位于块头，遇到非 Phi 即停止扫描
                    break;
                }
                if (this.useInstrs.contains(instr)) {
                    ((PhiInstruction) instr).fillIncomingValue(latest, visitBlock);
                    // 每个块针对同一 alloca 只会插入一个 Phi，完成填充后即可结束对该块的扫描
                    break;
                }
            }
        }
    }

    /**
     * 获取值栈顶的值，如果栈为空则返回默认值0
     */
    private IRValue peekValueStack() {
        if (!this.valueStack.isEmpty()) {
            IRValue top = this.valueStack.peek();
            // 安全回退：若栈顶是指令，但已不在其容器基本块的指令列表中（可能被其他优化移除），则返回默认0
            if (top instanceof IRInstruction topInstr) {
                IRValue container = topInstr.getContainer();
                if (container instanceof IRBasicBlock parentBlock) {
                    if (!parentBlock.getAllInstructions().contains(topInstr)) {
                        return defaultZeroForTargetType();
                    }
                }
            }
            return top;
        }
        // 栈为空时，返回与 alloca 对应类型一致的零常量，避免类型不匹配
        return defaultZeroForTargetType();
    }

    private void pruneTrivialPhis(IRBasicBlock startBlock) {
        HashSet<IRBasicBlock> visited = new HashSet<>();
        ArrayDeque<IRBasicBlock> q = new ArrayDeque<>();
        visited.add(startBlock);
        q.add(startBlock);
        while (!q.isEmpty()) {
            IRBasicBlock b = q.poll();
            Iterator<IRInstruction> it = b.getAllInstructions().iterator();
            while (it.hasNext()) {
                IRInstruction instr = it.next();
                if (!(instr instanceof PhiInstruction)) {
                    break;
                }
                if (!this.useInstrs.contains(instr)) {
                    continue;
                }
                PhiInstruction phi = (PhiInstruction) instr;
                List<IRBasicBlock> preds = phi.getPredecessorBlocks();
                if (preds.isEmpty()) {
                    it.remove();
                    continue;
                }
                IRValue first = phi.getIncomingValue(preds.get(0));
                boolean same = true;
                for (int i = 1; i < preds.size(); i++) {
                    IRValue v = phi.getIncomingValue(preds.get(i));
                    if (v != first) {
                        same = false;
                        break;
                    }
                }
                if (same && first != null) {
                    phi.replaceAllUsesWith(first);
                    it.remove();
                }
            }
            for (IRBasicBlock s : b.getSuccessors()) {
                if (visited.add(s)) {
                    q.add(s);
                }
            }
        }
    }

    private IRValue defaultZeroForTargetType() {
        if (this.targetValueType instanceof IntegerType integerType) {
            return new IntegerConstant(integerType, 0);
        }
        return new IntegerConstant(IntegerType.I32, 0);
    }

    /**
     * 在给定上下文块出口处可见的“当前值”
     *
     * 规则：
     * - 从栈顶向下寻找第一个满足可见性的值；
     * - 常量或参数视为全局可见，直接返回；
     * - 指令值需满足：
     *   1) 仍存在于其容器基本块的指令列表中；
     *   2) 其定义基本块支配当前上下文块（或等于该块），保证沿该来边可见；
     * - 若未找到，返回类型一致的 0。
     */
    private IRValue peekVisibleForBlock(IRBasicBlock contextBlock) {
        for (int i = this.valueStack.size() - 1; i >= 0; i--) {
            IRValue candidate = this.valueStack.get(i);
            if (candidate instanceof IRInstruction candInstr) {
                IRValue container = candInstr.getContainer();
                if (!(container instanceof IRBasicBlock defBlock)) {
                    continue;
                }
                // 候选指令必须仍存活于其所在块
                if (!defBlock.getAllInstructions().contains(candInstr)) {
                    continue;
                }
                // 可见性：定义块支配当前块，或定义就在当前块
                if (contextBlock == defBlock || contextBlock.getDominatedBy().contains(defBlock)) {
                    return candidate;
                }
                // 不可见则继续向下寻找更早的值
            } else {
                // 常量/函数参数等非指令值，一律视为可见
                return candidate;
            }
        }
        return defaultZeroForTargetType();
    }
}
