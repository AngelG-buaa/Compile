package middle.llvm;

import middle.llvm.value.IRBasicBlock;

/**
 * 循环结构管理类
 * 
 * 用于管理循环语句（for循环、while循环）在LLVM IR中的基本块结构。
 * 循环在LLVM IR中通常被分解为多个基本块来实现控制流：
 * 
 * 典型的for循环结构：
 * for (init; condition; update) {
 *     body;
 * }
 * 
 * 对应的基本块结构：
 * - initBlock: 初始化基本块，执行循环变量的初始化
 * - conditionBlock: 条件判断基本块，检查循环条件
 * - bodyBlock: 循环体基本块，执行循环内的语句
 * - updateBlock: 更新基本块，执行循环变量的更新操作
 * - exitBlock: 退出基本块，循环结束后的代码
 * 
 * 对应SysY语言中的循环语句：
 * - for语句：完整的五个基本块结构
 * - while语句：省略初始化和更新基本块
 * - break语句：直接跳转到exitBlock
 * - continue语句：跳转到updateBlock（for循环）或conditionBlock（while循环）
 */
public class LoopStructure {
    
    /**
     * 初始化基本块
     * 用于执行循环开始前的初始化操作，如for循环中的初始化语句
     */
    private final IRBasicBlock initializationBlock;
    
    /**
     * 条件判断基本块
     * 用于检查循环继续条件，决定是进入循环体还是退出循环
     */
    private final IRBasicBlock conditionBlock;
    
    /**
     * 循环体基本块
     * 包含循环内部的所有语句
     */
    private final IRBasicBlock bodyBlock;
    
    /**
     * 更新基本块
     * 用于执行循环变量的更新操作，如for循环中的增量表达式
     */
    private final IRBasicBlock updateBlock;
    
    /**
     * 退出基本块
     * 循环结束后继续执行的代码块
     */
    private final IRBasicBlock exitBlock;
    
    /**
     * 构造循环结构
     * 
     * @param initializationBlock 初始化基本块，执行循环前的准备工作
     * @param conditionBlock 条件判断基本块，检查循环条件
     * @param bodyBlock 循环体基本块，包含循环内的语句
     * @param updateBlock 更新基本块，执行循环变量更新
     * @param exitBlock 退出基本块，循环结束后的代码
     */
    public LoopStructure(IRBasicBlock initializationBlock, 
                        IRBasicBlock conditionBlock,
                        IRBasicBlock bodyBlock, 
                        IRBasicBlock updateBlock, 
                        IRBasicBlock exitBlock) {
        this.initializationBlock = initializationBlock;
        this.conditionBlock = conditionBlock;
        this.bodyBlock = bodyBlock;
        this.updateBlock = updateBlock;
        this.exitBlock = exitBlock;
    }
    
    /**
     * 获取初始化基本块
     * 
     * @return 初始化基本块，用于循环开始前的准备工作
     */
    public IRBasicBlock getInitializationBlock() {
        return initializationBlock;
    }
    
    /**
     * 获取条件判断基本块
     * 
     * @return 条件判断基本块，用于检查循环继续条件
     */
    public IRBasicBlock getConditionBlock() {
        return conditionBlock;
    }
    
    /**
     * 获取循环体基本块
     * 
     * @return 循环体基本块，包含循环内部的语句
     */
    public IRBasicBlock getBodyBlock() {
        return bodyBlock;
    }
    
    /**
     * 获取更新基本块
     * 
     * @return 更新基本块，用于循环变量的更新操作
     */
    public IRBasicBlock getUpdateBlock() {
        return updateBlock;
    }
    
    /**
     * 获取退出基本块
     * 
     * @return 退出基本块，循环结束后继续执行的代码
     */
    public IRBasicBlock getExitBlock() {
        return exitBlock;
    }
    
    /**
     * 获取break语句应该跳转的目标基本块
     * break语句会直接跳出循环，因此目标是退出基本块
     * 
     * @return 退出基本块
     */
    public IRBasicBlock getBreakTarget() {
        return exitBlock;
    }
    
    /**
     * 获取continue语句应该跳转的目标基本块
     * continue语句会跳过当前迭代的剩余部分，直接进入下一次迭代
     * 对于for循环，跳转到更新基本块；对于while循环，跳转到条件基本块
     * 
     * @return 更新基本块（如果存在）或条件基本块
     */
    public IRBasicBlock getContinueTarget() {
        // 如果有更新基本块（for循环），continue跳转到更新基本块
        // 如果没有更新基本块（while循环），continue跳转到条件基本块
        return updateBlock != null ? updateBlock : conditionBlock;
    }
    
    /**
     * 检查是否为完整的for循环结构
     * 完整的for循环包含所有五个基本块
     * 
     * @return 如果包含更新基本块则为完整for循环，否则为while循环
     */
    public boolean isCompleteForLoop() {
        return updateBlock != null;
    }
    
    /**
     * 获取循环结构的字符串表示，用于调试
     * 
     * @return 循环结构的描述字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LoopStructure{");
        sb.append("init=").append(initializationBlock != null ? initializationBlock.getName() : "null");
        sb.append(", cond=").append(conditionBlock != null ? conditionBlock.getName() : "null");
        sb.append(", body=").append(bodyBlock != null ? bodyBlock.getName() : "null");
        sb.append(", update=").append(updateBlock != null ? updateBlock.getName() : "null");
        sb.append(", exit=").append(exitBlock != null ? exitBlock.getName() : "null");
        sb.append("}");
        return sb.toString();
    }
}