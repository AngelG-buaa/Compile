package middle.optimize;

import back.mips.register.Reg;
import middle.llvm.IRModule;
import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRFunction;
import middle.llvm.value.IRValue;
import middle.llvm.value.instruction.CallInstruction;
import middle.llvm.value.instruction.CopyInstruction;
import middle.llvm.value.instruction.IRInstruction;
import middle.llvm.value.instruction.PhiInstruction;
import middle.llvm.value.instruction.AllocaInstruction;

import java.util.*;

/**
 * 重新实现的寄存器分配器
 * 采用标准的线性扫描算法 (Linear Scan Register Allocation)
 * 配合严格限制的寄存器池，确保与特定后端实现的兼容性。
 */
public class RegAlloca {
    private final IRModule module;

    // 最终分配结果
    private HashMap<IRValue, Reg> globalValueRegMap;

    // 可用寄存器池 (严格限制)
    private static final List<Reg> ALLOCATABLE_REGS = Arrays.asList(
            Reg.t3, Reg.t4, Reg.t5, Reg.t6, Reg.t7, Reg.t8, Reg.t9
    );

    // 活跃区间定义
    private static class LiveInterval implements Comparable<LiveInterval> {
        IRValue value;
        int startPoint; // 第一次定义或使用的位置
        int endPoint;   // 最后一次使用的位置
        Reg assignedReg = null;

        public LiveInterval(IRValue value, int start, int end) {
            this.value = value;
            this.startPoint = start;
            this.endPoint = end;
        }

        @Override
        public int compareTo(LiveInterval o) {
            return Integer.compare(this.startPoint, o.startPoint);
        }
    }

    private static RegAlloca instance;

    private RegAlloca(IRModule module) {
        this.module = module;
    }

    public static RegAlloca getInstance(IRModule module) {
        if (instance == null) {
            instance = new RegAlloca(module);
        }
        return instance;
    }

    public void alloca() {
        for (IRFunction function : module.getFunctionDefinitions()) {
            allocateFunction(function);
        }
    }

    private void allocateFunction(IRFunction function) {
        if (function.getBasicBlocks().isEmpty()) return;

        this.globalValueRegMap = new HashMap<>();

        // 1. 指令线性化编号 (Linearization & Numbering)
        List<IRInstruction> linearInstructions = new ArrayList<>();
        Map<IRInstruction, Integer> instrIdMap = new HashMap<>();
        int instrCounter = 0;

        // 按照基本块顺序简单线性化 (对于线性扫描足够了)
        for (IRBasicBlock bb : function.getBasicBlocks()) {
            for (IRInstruction instr : bb.getAllInstructions()) {
                instrIdMap.put(instr, instrCounter);
                linearInstructions.add(instr);
                instrCounter += 2; // 步长为2，方便后续插入（如果有需要）
            }
        }

        // 2. 活跃性分析 (Liveness Analysis)
        LivenessAnalyzer analyzer = new LivenessAnalyzer();
        analyzer.analyze(function);

        // 3. 构建活跃区间 (Build Live Intervals)
        List<LiveInterval> intervals = buildIntervals(function, instrIdMap, analyzer, instrCounter);

        // 4. 线性扫描分配 (Linear Scan Allocation)
        linearScanAllocate(intervals);

        // 5. 保存结果并更新 Call 指令的 Caller-Save 信息
        function.setValue2reg(globalValueRegMap);

        // 6. !!! 修复点：基于区间和指令ID精确更新 Call 的 Caller-Save !!!
        updateCallLiveRegs(function, intervals, instrIdMap);
    }

    /**
     * 构建变量的生命周期区间
     */
    private List<LiveInterval> buildIntervals(IRFunction function,
                                              Map<IRInstruction, Integer> instrIdMap,
                                              LivenessAnalyzer analyzer,
                                              int maxId) {
        Map<IRValue, Integer> startMap = new HashMap<>();
        Map<IRValue, Integer> endMap = new HashMap<>();

        // !!! 新增修复代码开始 !!!
        // 显式将参数的起始活跃位置设为 -1。
        // 这样当 Call 指令恰好是函数第一条指令(ID=0)时，-1 < 0 条件成立，
        // 确保参数寄存器被判定为“跨越 Call 活跃”，从而被正确保存。
        for (middle.llvm.value.IRFunctionParameter param : function.getParameters()) {
            startMap.put(param, -1);
            endMap.put(param, -1); // endMap 会在后续遍历 Use 时被更新扩展
        }
        // !!! 新增修复代码结束 !!!

        // 初始化：遍历所有块，结合 LiveIn/LiveOut 和块内 Def/Use 计算区间
        for (IRBasicBlock bb : function.getBasicBlocks()) {
            int blockStart = -1;
            int blockEnd = -1;

            if (!bb.getAllInstructions().isEmpty()) {
                blockStart = instrIdMap.get(bb.getAllInstructions().getFirst());
                blockEnd = instrIdMap.get(bb.getAllInstructions().getLast());
            } else {
                continue;
            }

            // 对于该块 LiveIn 的变量，它们的生命周期至少覆盖到块开始
            // 注意：线性扫描通常处理的是整个函数的区间，所以 LiveIn 意味着变量从之前就活跃了
            // 这里我们通过遍历指令来修正具体的 start/end

            // 变量在该块 LiveOut，说明它活到了块尾
            for (IRValue liveOut : bb.getLiveOut()) {
                updateRange(startMap, endMap, liveOut, -1, blockEnd); // start暂不更新，end延展到blockEnd
            }

            // 遍历指令更新 Def/Use
            for (IRInstruction instr : bb.getAllInstructions()) {
                int id = instrIdMap.get(instr);

                // Def: 定义变量，这是区间的起点
                IRValue def = getDefValue(instr);
                if (def != null) {
                    updateRange(startMap, endMap, def, id, id);
                }

                // Use: 使用变量，这是区间的延伸
                for (IRValue operand : instr.getAllOperands()) {
                    if (isVariable(operand)) {
                        updateRange(startMap, endMap, operand, -1, id);
                    }
                }
            }
        }

        List<LiveInterval> result = new ArrayList<>();
        for (IRValue val : startMap.keySet()) {
            // 过滤掉未使用的变量或者不需要分配的
            if (endMap.containsKey(val)) {
                result.add(new LiveInterval(val, startMap.get(val), endMap.get(val)));
            }
        }
        Collections.sort(result); // 按起始位置排序
        return result;
    }

    private void updateRange(Map<IRValue, Integer> startMap, Map<IRValue, Integer> endMap, IRValue val, int start, int end) {
        if (!isVariable(val)) return;

        if (start != -1) {
            if (!startMap.containsKey(val) || start < startMap.get(val)) {
                startMap.put(val, start);
            }
        }

        if (end != -1) {
            if (!endMap.containsKey(val) || end > endMap.get(val)) {
                endMap.put(val, end);
            }
        }

        // 确保 map 都有 entry，避免空指针
        if (!startMap.containsKey(val)) startMap.put(val, 0); // 默认从头开始（参数等）
        if (!endMap.containsKey(val)) endMap.put(val, startMap.get(val));
    }

    /**
     * 核心算法：线性扫描
     */
    private void linearScanAllocate(List<LiveInterval> intervals) {
        List<LiveInterval> active = new ArrayList<>();
        // 寄存器状态池
        List<Reg> freeRegs = new ArrayList<>(ALLOCATABLE_REGS);

        for (LiveInterval current : intervals) {
            // 1. 移除过期的区间 (Expire Old Intervals)
            expireOldIntervals(current, active, freeRegs);

            // 2. 尝试分配
            if (active.size() == ALLOCATABLE_REGS.size()) {
                // 寄存器耗尽，溢出 (Spill)
                spillAtInterval(current, active, freeRegs);
            } else {
                // 分配寄存器
                Reg reg = freeRegs.remove(0); // 拿第一个空闲的
                current.assignedReg = reg;
                active.add(current);
                globalValueRegMap.put(current.value, reg);

                // 保持 active 按结束位置排序，方便 expire
                active.sort((a, b) -> Integer.compare(a.endPoint, b.endPoint));
            }
        }
    }

    private void expireOldIntervals(LiveInterval current, List<LiveInterval> active, List<Reg> freeRegs) {
        Iterator<LiveInterval> it = active.iterator();
        while (it.hasNext()) {
            LiveInterval interval = it.next();
            // 如果活跃区间的结束点 在 当前区间开始点 之前，说明已经结束了
            if (interval.endPoint < current.startPoint) {
                it.remove();
                // 释放寄存器 (归还到池中)
                // 简单起见加到末尾，或者为了复用率加到开头
                freeRegs.add(0, interval.assignedReg);
            }
        }
    }

    private void spillAtInterval(LiveInterval current, List<LiveInterval> active, List<Reg> freeRegs) {
        // 启发式：溢出那个结束得最晚的 (Spill the one that ends furthest in the future)
        LiveInterval spillCandidate = active.get(active.size() - 1); // active 是按 endPoint 排序的

        if (spillCandidate.endPoint > current.endPoint) {
            // 当前区间比候选区间结束得早，说明当前区间更有价值保留在寄存器中
            // 抢占寄存器
            current.assignedReg = spillCandidate.assignedReg;
            globalValueRegMap.put(current.value, current.assignedReg);
            active.add(current);
            active.sort((a, b) -> Integer.compare(a.endPoint, b.endPoint));

            // 溢出之前的候选者
            spillCandidate.assignedReg = null;
            globalValueRegMap.remove(spillCandidate.value);
            active.remove(spillCandidate);
        } else {
            // 当前区间结束得更晚，直接溢出当前区间
            // do nothing, current.assignedReg stays null
        }
    }

    /**
     * 更新 Call 指令的 LiveRegs (用于 Caller Save)
     * 后端会读取这个 Set，将里面包含的寄存器保存到栈上
     */
    private void updateCallLiveRegs(IRFunction function,
                                    List<LiveInterval> intervals,
                                    Map<IRInstruction, Integer> instrIdMap) {

        // 为了快速查询，将 active intervals 缓存或直接遍历
        // 由于 intervals 数量通常不多，直接遍历也尚可，或者可以优化为区间树
        // 这里采用遍历方式

        for (IRBasicBlock bb : function.getBasicBlocks()) {
            for (IRInstruction instr : bb.getAllInstructions()) {
                if (instr instanceof CallInstruction) {
                    CallInstruction call = (CallInstruction) instr;
                    int callId = instrIdMap.get(instr);
                    HashSet<Reg> liveRegs = new HashSet<>();

                    for (LiveInterval interval : intervals) {
                        // 如果变量被分配了寄存器
                        if (interval.assignedReg != null) {
                            // 核心判定： Start < Call < End
                            // 1. startPoint < callId: 变量在 Call 之前定义
                            // 2. endPoint > callId: 变量在 Call 之后还会被使用
                            // 注意：如果 endPoint == callId，说明 Call 是最后一次使用（作为参数），
                            // 这种情况下参数通过寄存器传递，不需要 Caller Save 跨越 Call。
                            if (interval.startPoint < callId && interval.endPoint > callId) {
                                liveRegs.add(interval.assignedReg);
                            }
                        }
                    }
                    call.liveRegSet = liveRegs;
                }
            }
        }
    }

    // ================= 辅助方法 =================
    private IRValue getDefValue(IRInstruction instr) {
        if (instr instanceof CopyInstruction) {
            return ((CopyInstruction) instr).getTargetValue();
        }
        if (hasReturnValue(instr)) {
            return instr;
        }
        return null;
    }

    private boolean isVariable(IRValue v) {
        return (v instanceof IRInstruction || v instanceof middle.llvm.value.IRFunctionParameter)
                && !(v instanceof middle.llvm.value.IRGlobalVariable)
                && !(v instanceof middle.llvm.value.constant.IRConstant)
                && !(v instanceof IRBasicBlock);
    }

    private boolean hasReturnValue(IRInstruction instr) {
        // Alloca 不分配寄存器 (地址在栈上)
        // Store, Branch, Jump, Return 等无返回值
        return instr.getName() != null && !(instr instanceof AllocaInstruction);
    }

    // ================= 活跃性分析器 =================
    private class LivenessAnalyzer {
        void analyze(IRFunction function) {
            // 1. 初始化 Def/Use 集合
            for (IRBasicBlock bb : function.getBasicBlocks()) {
                makeDefUse(bb);
                bb.setLiveIn(new HashSet<>());
                bb.setLiveOut(new HashSet<>());
            }

            // 2. 迭代计算 LiveIn / LiveOut
            boolean changed = true;
            while (changed) {
                changed = false;
                // 后序遍历基本块
                List<IRBasicBlock> bbs = function.getBasicBlocks();
                for (int i = bbs.size() - 1; i >= 0; i--) {
                    IRBasicBlock bb = bbs.get(i);

                    // OUT[B] = U (IN[S])
                    Set<IRValue> newOut = new HashSet<>();
                    for (IRBasicBlock succ : bb.getSuccessors()) {
                        newOut.addAll(succ.getLiveIn());

                        // 处理后继块中的 Phi 指令
                        for (IRInstruction instr : succ.getAllInstructions()) {
                            if (instr instanceof PhiInstruction) {
                                PhiInstruction phi = (PhiInstruction) instr;
                                // 使用 PhiInstruction 的 map 接口获取当前块 bb 对应的输入值
                                IRValue val = phi.getIncomingValue(bb);
                                if (val != null && isVariable(val)) {
                                    newOut.add(val);
                                }
                            } else {
                                // Phi 指令必定在块开头，遇到非 Phi 即可停止
                                break;
                            }
                        }
                    }

                    // IN[B] = USE[B] U (OUT[B] - DEF[B])
                    Set<IRValue> newIn = new HashSet<>(newOut);
                    newIn.removeAll(bb.getDefSet());
                    newIn.addAll(bb.getUseSet());

                    if (!newOut.equals(bb.getLiveOut()) || !newIn.equals(bb.getLiveIn())) {
                        bb.setLiveOut(newOut);
                        bb.setLiveIn(newIn);
                        changed = true;
                    }
                }
            }
        }

        private void makeDefUse(IRBasicBlock bb) {
            bb.getDefSet().clear();
            bb.getUseSet().clear();

            for (IRInstruction instr : bb.getAllInstructions()) {
                // 如果是 Phi 指令，跳过 Use 计算（已在 analyze 中作为 LiveOut 处理）
                if (instr instanceof PhiInstruction) {
                    IRValue def = getDefValue(instr);
                    if (def != null) bb.addToDef(def);
                    continue;
                }

                // 普通指令处理
                for (IRValue op : instr.getAllOperands()) {
                    if (isVariable(op) && !bb.getDefSet().contains(op)) {
                        bb.addToUse(op);
                    }
                }

                IRValue def = getDefValue(instr);
                if (def != null) {
                    bb.addToDef(def);
                }
            }
        }
    }
}