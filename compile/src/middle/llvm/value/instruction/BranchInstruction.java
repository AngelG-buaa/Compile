package middle.llvm.value.instruction;

import middle.llvm.type.VoidType;
import middle.llvm.value.IRValue;

/**
 * LLVM IR条件分支指令
 * 
 * 表示LLVM IR中的条件分支指令，根据条件值决定程序的执行路径。
 * 
 * 特性：
 * - 终结指令：必须是基本块的最后一条指令
 * - 条件判断：根据i1类型的条件值进行分支
 * - 双向分支：提供真分支和假分支两个目标
 * - 无返回值：分支指令不产生值，类型为void
 * - 控制流转移：将程序执行权转移到目标基本块
 * 
 * LLVM IR语法示例：
 * - br i1 %cond, label %true_bb, label %false_bb    ; 条件分支
 * - br i1 true, label %always_true, label %never    ; 常量条件分支
 * - br i1 %result, label %then, label %else         ; 比较结果分支
 * 
 * 对应SysY语言场景：
 * - if语句：if (condition) { ... } else { ... }
 * - while循环：while (condition) { ... }
 * - for循环：for (...; condition; ...) { ... }
 * - 条件表达式：condition ? value1 : value2
 * - 逻辑运算的短路求值：a && b, a || b
 * 
 * 分支类型：
 * - 条件分支：根据运行时条件值选择路径
 * - 常量分支：编译时已知条件的分支（可优化）
 * - 比较分支：基于比较指令结果的分支
 * 
 * 与其他指令的配合：
 * - 与比较指令配合：icmp指令产生条件值
 * - 与load指令配合：加载条件变量
 * - 与phi指令配合：在汇合点合并不同路径的值
 * - 与基本块配合：连接不同的代码块
 * 
 * 编译器优化相关：
 * - 分支预测：预测分支方向以提高性能
 * - 条件移动：将简单分支转换为条件移动指令
 * - 分支消除：消除永远不会执行的分支
 * - 基本块合并：合并只有一个前驱的基本块
 */
public class BranchInstruction extends IRInstruction {
    
    /**
     * 构造条件分支指令
     * 
     * @param parentBlock 所属基本块
     * @param condition 分支条件，必须是i1类型
     * @param trueBranch 条件为真时的目标基本块
     * @param falseBranch 条件为假时的目标基本块
     */
    public BranchInstruction(IRValue parentBlock, IRValue condition, IRValue trueBranch, IRValue falseBranch) {
        super(parentBlock, new VoidType());
        addOperand(condition);
        addOperand(trueBranch);
        addOperand(falseBranch);
    }
    
    /**
     * 获取分支条件
     * 
     * @return 用于判断分支方向的条件值（i1类型）
     */
    public IRValue getCondition() {
        return getOperand(0);
    }
    
    /**
     * 获取真分支目标
     * 
     * @return 条件为真时跳转的目标基本块
     */
    public IRValue getTrueBranch() {
        return getOperand(1);
    }
    
    /**
     * 设置真分支目标
     * 
     * @param trueBranch 条件为真时跳转的目标基本块
     */
    public void setTrueBranch(IRValue trueBranch) {
        replaceOperand(1, trueBranch);
    }

    /**
     * 获取假分支目标
     * 
     * @return 条件为假时跳转的目标基本块
     */
    public IRValue getFalseBranch() {
        return getOperand(2);
    }

    /**
     * 设置假分支目标
     * 
     * @param falseBranch 条件为假时跳转的目标基本块
     */
    public void setFalseBranch(IRValue falseBranch) {
        replaceOperand(2, falseBranch);
    }
    
    /**
     * 检查是否为终结指令
     * 分支指令是终结指令，必须是基本块的最后一条指令
     * 
     * @return 总是返回true，表示这是一个终结指令
     */
    @Override
    public boolean isTerminatorInstruction() {
        return true; // 分支指令是终结指令
    }
    
    /**
     * 获取指令操作码名称
     * 
     * @return 返回"br"，表示这是一个分支指令
     */
    @Override
    public String getOpcodeName() {
        return "br";
    }
    
    /**
     * 生成LLVM IR字符串表示
     * 
     * @return 完整的LLVM IR条件分支指令字符串
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("br i1 ");
        builder.append(getCondition().getName());
        builder.append(", label ");
        builder.append(getTrueBranch().getName());
        builder.append(", label ");
        builder.append(getFalseBranch().getName());
        return builder.toString();
    }
}