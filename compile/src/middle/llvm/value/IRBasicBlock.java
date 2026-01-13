package middle.llvm.value;

import middle.llvm.type.LabelType;
import middle.llvm.value.instruction.IRInstruction;
import middle.llvm.value.instruction.PhiInstruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * LLVM IR基本块实现
 * 
 * 表示LLVM IR中的基本块（Basic Block），是控制流图的基本单元。
 * 基本块是一个指令序列，具有以下特性：
 * - 只有一个入口点（第一条指令）
 * - 只有一个出口点（最后一条指令，通常是终结指令）
 * - 内部没有跳转指令（除了最后一条）
 * - 执行时要么全部执行，要么全部不执行
 * 
 * 在LLVM IR中的表示形式：
 * - entry:                    ; 入口基本块（通常省略标签）
 *     %1 = add i32 %a, %b     ; 普通指令
 *     br label %exit          ; 终结指令
 * 
 * - loop:                     ; 循环基本块
 *     %2 = phi i32 [0, %entry], [%3, %loop]  ; Phi指令
 *     %3 = add i32 %2, 1
 *     %4 = icmp slt i32 %3, 10
 *     br i1 %4, label %loop, label %exit
 * 
 * - exit:                     ; 出口基本块
 *     ret i32 %3
 * 
 * 对应SysY语言中的控制结构：
 * - 顺序执行语句块 -> 单个基本块
 * - if-else语句 -> 条件基本块 + 两个分支基本块 + 合并基本块
 * - while循环 -> 条件基本块 + 循环体基本块 + 出口基本块
 * - for循环 -> 初始化基本块 + 条件基本块 + 循环体基本块 + 更新基本块 + 出口基本块
 * 
 * 基本块的重要概念：
 * - 前驱块（Predecessors）：可能跳转到当前基本块的基本块
 * - 后继块（Successors）：当前基本块可能跳转到的基本块
 * - 支配关系（Dominance）：用于优化和分析的重要概念
 * - 活跃性分析（Liveness Analysis）：用于寄存器分配和优化
 */
public class IRBasicBlock extends IRValue {
    
    // ==================== 控制流图相关 ====================
    
    /**
     * 前驱基本块集合
     * 包含所有可能跳转到当前基本块的基本块
     */
    private final Set<IRBasicBlock> predecessors = new HashSet<>();
    
    /**
     * 后继基本块集合
     * 包含当前基本块可能跳转到的所有基本块
     */
    private final Set<IRBasicBlock> successors = new HashSet<>();
    
    // ==================== 支配关系相关 ====================
    
    /**
     * 支配当前基本块的所有基本块
     * 如果基本块A支配基本块B，则从入口到B的所有路径都必须经过A
     */
    private final Set<IRBasicBlock> dominatedBy = new HashSet<>();
    
    /**
     * 直接支配者
     * 在支配树中，当前基本块的直接父节点
     */
    private IRBasicBlock immediateDominator;
    
    /**
     * 直接被支配的基本块集合
     * 在支配树中，当前基本块的直接子节点
     */
    private final Set<IRBasicBlock> immediateDominated = new HashSet<>();
    
    /**
     * 支配边界
     * 用于SSA形式构造中确定Phi指令的插入位置
     */
    private final Set<IRBasicBlock> dominanceFrontier = new HashSet<>();
    
    // ==================== 活跃性分析相关 ====================
    
    /**
     * 定义集合（Def Set）
     * 在当前基本块中被定义（赋值）的变量集合
     */
    private final Set<IRValue> defSet = new HashSet<>();
    
    /**
     * 使用集合（Use Set）
     * 在当前基本块中被使用但未被定义的变量集合
     */
    private final Set<IRValue> useSet = new HashSet<>();
    
    /**
     * 活跃输入集合（Live In）
     * 在基本块入口处活跃的变量集合
     */
    private final Set<IRValue> liveIn = new HashSet<>();
    
    /**
     * 活跃输出集合（Live Out）
     * 在基本块出口处活跃的变量集合
     */
    private final Set<IRValue> liveOut = new HashSet<>();
    
    // ==================== 指令和状态 ====================
    
    /**
     * 基本块包含的指令列表
     * 按执行顺序排列，最后一条通常是终结指令
     */
    private final LinkedList<IRInstruction> instructions = new LinkedList<>();
    
    /**
     * 访问标记，用于图遍历算法
     */
    private boolean isVisited = false;
    
    /**
     * 活跃标记，用于死代码消除
     */
    private boolean isLive = true;
    
    /**
     * 构造基本块
     * 
     * @param parent 父容器（通常是IRFunction）
     * @param nameCounter 基本块编号，用于生成唯一标签名
     */
    public IRBasicBlock(IRValue parent, int nameCounter) {
        super(parent, "%b" + nameCounter, new LabelType());
    }
    
    // ==================== 指令管理方法 ====================
    
    /**
     * 在基本块末尾添加指令
     * 
     * @param instruction 要添加的指令
     */
    public void addInstructionToTail(IRInstruction instruction) {
        instructions.addLast(instruction);
    }
    
    /**
     * 在基本块开头添加指令
     * 
     * 通常用于插入Phi指令，因为Phi指令必须在基本块开头
     * 
     * @param instruction 要添加的指令
     */
    public void addInstructionToHead(IRInstruction instruction) {
        instructions.addFirst(instruction);
    }
    
    /**
     * 获取所有指令
     * 
     * @return 指令列表
     */
    public LinkedList<IRInstruction> getAllInstructions() {
        return instructions;
    }
    
    /**
     * 获取最后一条指令
     * 
     * 通常是终结指令（br, ret等）
     * 
     * @return 最后一条指令，如果基本块为空则返回null
     */
    public IRInstruction getLastInstruction() {
        return instructions.isEmpty() ? null : instructions.getLast();
    }

    /**
     * 判断基本块是否已终止（以终结指令结尾）
     * 
     * @return 如果最后一条指令是终结指令则返回true，否则（包括空块）返回false
     */
    public boolean isTerminated() {
        IRInstruction last = getLastInstruction();
        return last != null && last.isTerminatorInstruction();
    }
    
    /**
     * 获取第一条指令
     * 
     * @return 第一条指令，如果基本块为空则返回null
     */
    public IRInstruction getFirstInstruction() {
        return instructions.isEmpty() ? null : instructions.getFirst();
    }
    
    /**
     * 获取所有Phi指令
     * 
     * Phi指令用于SSA形式中合并来自不同前驱块的值
     * 例如：%result = phi i32 [%val1, %block1], [%val2, %block2]
     * 
     * @return Phi指令列表
     */
    public List<PhiInstruction> getPhiInstructions() {
        List<PhiInstruction> phiInstructions = new ArrayList<>();
        for (IRInstruction instruction : instructions) {
            if (instruction instanceof PhiInstruction) {
                phiInstructions.add((PhiInstruction) instruction);
            }
        }
        return phiInstructions;
    }
    
    // ==================== 控制流图方法 ====================
    
    /**
     * 获取前驱基本块集合
     */
    public Set<IRBasicBlock> getPredecessors() {
        return predecessors;
    }
    
    /**
     * 获取后继基本块集合
     */
    public Set<IRBasicBlock> getSuccessors() {
        return successors;
    }
    
    /**
     * 添加前驱基本块
     */
    public void addPredecessor(IRBasicBlock predecessor) {
        predecessors.add(predecessor);
    }
    
    /**
     * 添加后继基本块
     */
    public void addSuccessor(IRBasicBlock successor) {
        successors.add(successor);
    }
    
    /**
     * 移除前驱基本块
     */
    public void removePredecessor(IRBasicBlock predecessor) {
        predecessors.remove(predecessor);
    }
    
    /**
     * 移除后继基本块
     */
    public void removeSuccessor(IRBasicBlock successor) {
        successors.remove(successor);
    }


    public void clearCFG() {
        // 可达
        this.predecessors.clear();
        this.successors.clear();
        // 支配
        this.dominatedBy.clear();
        this.immediateDominator = null;
        this.immediateDominated.clear();
        this.dominanceFrontier.clear();
    }
    // ==================== 支配关系方法 ====================
    
    /**
     * 获取支配当前基本块的所有基本块
     */
    public Set<IRBasicBlock> getDominatedBy() {
        return dominatedBy;
    }
    
    /**
     * 获取直接支配者
     */
    public IRBasicBlock getImmediateDominator() {
        return immediateDominator;
    }
    
    /**
     * 设置直接支配者
     */
    public void setImmediateDominator(IRBasicBlock dominator) {
        this.immediateDominator = dominator;
    }
    
    /**
     * 获取直接被支配的基本块集合
     */
    public Set<IRBasicBlock> getImmediateDominated() {
        return immediateDominated;
    }
    
    /**
     * 添加直接被支配的基本块
     */
    public void addImmediateDominated(IRBasicBlock dominated) {
        immediateDominated.add(dominated);
    }
    
    /**
     * 获取支配边界
     */
    public Set<IRBasicBlock> getDominanceFrontier() {
        return dominanceFrontier;
    }
    
    /**
     * 添加到支配边界
     */
    public void addToDominanceFrontier(IRBasicBlock block) {
        dominanceFrontier.add(block);
    }
    
    // ==================== 活跃性分析方法 ====================
    
    /**
     * 获取定义集合
     */
    public Set<IRValue> getDefSet() {
        return defSet;
    }
    
    /**
     * 获取使用集合
     */
    public Set<IRValue> getUseSet() {
        return useSet;
    }
    
    /**
     * 获取活跃输入集合
     */
    public Set<IRValue> getLiveIn() {
        return liveIn;
    }
    
    /**
     * 获取活跃输出集合
     */
    public Set<IRValue> getLiveOut() {
        return liveOut;
    }
    
    /**
     * 添加到定义集合
     */
    public void addToDef(IRValue value) {
        defSet.add(value);
    }
    
    /**
     * 添加到使用集合
     */
    public void addToUse(IRValue value) {
        useSet.add(value);
    }
    
    /**
     * 设置活跃输入集合
     */
    public void setLiveIn(Set<IRValue> liveIn) {
        this.liveIn.clear();
        this.liveIn.addAll(liveIn);
    }
    
    /**
     * 设置活跃输出集合
     */
    public void setLiveOut(Set<IRValue> liveOut) {
        this.liveOut.clear();
        this.liveOut.addAll(liveOut);
    }
    
    // ==================== 状态管理方法 ====================
    
    /**
     * 获取访问标记
     */
    public boolean isVisited() {
        return isVisited;
    }
    
    /**
     * 设置访问标记
     */
    public void setVisited(boolean visited) {
        isVisited = visited;
    }
    
    /**
     * 获取活跃标记
     */
    public boolean isLive() {
        return isLive;
    }
    
    /**
     * 设置活跃标记
     */
    public void setLive(boolean live) {
        this.isLive = live;
    }
    
    /**
     * 生成LLVM IR格式的基本块字符串
     * 
     * 格式：
     * label:
     *   instruction1
     *   instruction2
     *   ...
     * 
     * @return LLVM IR格式的基本块定义
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        // 基本块标签：始终打印，包括入口块（%b0）。
        // 因为Phi等指令可能引用入口块标签，若不打印将导致“use of undefined value”。
        builder.append(getName().substring(1)).append(":\n");
        
        // 输出所有指令，每条指令前加两个空格缩进
        for (IRInstruction instruction : instructions) {
            builder.append("  ").append(instruction.toString()).append("\n");
        }
        
        return builder.toString();
    }
}