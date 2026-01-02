package middle.llvm.value;

import middle.llvm.type.IRType;

import java.util.ArrayList;
import java.util.List;

/**
 * LLVM IR用户类实现
 * 
 * <p>表示LLVM IR中使用其他值作为操作数的IR值，是LLVM IR中Use-Def链的核心组件。
 * IRUser类在LLVM IR中具有以下特征：
 * <ul>
 *   <li>维护一个操作数列表，记录该值使用的其他值</li>
 *   <li>自动管理Use-Def关系，确保数据流分析的正确性</li>
 *   <li>支持操作数的动态添加、替换和删除</li>
 *   <li>是大多数LLVM IR指令的基类</li>
 * </ul>
 * 
 * <p>Use-Def链概念：
 * <ul>
 *   <li><strong>Use</strong>：一个值被另一个值使用的关系</li>
 *   <li><strong>Def</strong>：一个值定义另一个值的关系</li>
 *   <li><strong>Use-Def链</strong>：连接值的定义和使用的双向关系链</li>
 * </ul>
 * 
 * <p>LLVM IR中的用户示例：
 * <pre>
 * ; 算术指令使用两个操作数
 * %3 = add i32 %1, %2    ; IRUser: %3, 操作数: [%1, %2]
 * 
 * ; 函数调用使用函数和参数作为操作数
 * %4 = call i32 @func(i32 %3)    ; IRUser: %4, 操作数: [@func, %3]
 * 
 * ; 内存访问指令使用地址作为操作数
 * %5 = load i32, i32* %ptr    ; IRUser: %5, 操作数: [%ptr]
 * 
 * ; 分支指令使用条件和标签作为操作数
 * br i1 %cond, label %true, label %false    ; IRUser: br, 操作数: [%cond, %true, %false]
 * </pre>
 * 
 * <p>对应SysY语言场景：
 * <ul>
 *   <li>表达式计算：{@code a + b} 对应加法指令，使用a和b作为操作数</li>
 *   <li>函数调用：{@code func(x, y)} 对应调用指令，使用func、x、y作为操作数</li>
 *   <li>数组访问：{@code arr[i]} 对应GEP指令，使用arr和i作为操作数</li>
 *   <li>条件分支：{@code if (cond)} 对应分支指令，使用cond作为操作数</li>
 * </ul>
 * 
 * <p>Use-Def关系管理：
 * <ul>
 *   <li>当添加操作数时，自动在被使用的值上记录使用关系</li>
 *   <li>当替换操作数时，自动更新相关的Use-Def关系</li>
 *   <li>当删除操作数时，自动清理相关的Use-Def关系</li>
 *   <li>支持编译器优化中的数据流分析和变换</li>
 * </ul>
 * 
 * @see IRValue 值基类
 * @see IRInstruction 指令基类
 * @see IRType 类型系统
 */
public class IRUser extends IRValue {
    /**
     * 操作数列表
     * 存储该用户值使用的所有操作数，维护Use-Def关系的Use端
     */
    private List<IRValue> operandList; // 操作数列表
    
    /**
     * 构造用户值（带操作数列表）
     * 
     * @param containerValue 容器值（父级值）
     * @param valueName 值名称
     * @param valueType 值类型
     * @param operandList 初始操作数列表
     */
    public IRUser(IRValue containerValue, String valueName, IRType valueType, List<IRValue> operandList) {
        super(containerValue, valueName, valueType);
        this.operandList = new ArrayList<>();
        if (operandList != null) {
            setOperands(operandList);
        }
    }
    
    /**
     * 构造用户值（无初始操作数）
     * 
     * @param containerValue 容器值（父级值）
     * @param valueName 值名称
     * @param valueType 值类型
     */
    public IRUser(IRValue containerValue, String valueName, IRType valueType) {
        super(containerValue, valueName, valueType);
        this.operandList = new ArrayList<>();
    }
    
    /**
     * 构造用户值（自动生成名称）
     * 
     * @param containerValue 容器值（父级值）
     * @param valueType 值类型
     */
    public IRUser(IRValue containerValue, IRType valueType) {
        super(containerValue, valueType);
        this.operandList = new ArrayList<>();
    }
    
    /**
     * 批量设置操作数列表
     * 
     * <p>该方法会为每个操作数建立Use-Def关系，确保数据流分析的正确性。
     * 
     * @param operandList 要设置的操作数列表
     */
    protected void setOperands(List<IRValue> operandList) {
        for (IRValue operand : operandList) {
            addOperand(operand);
        }
    }
    
    /**
     * 添加单个操作数
     * 
     * <p>添加操作数时会自动建立Use-Def关系：
     * <ul>
     *   <li>将操作数添加到当前用户的操作数列表中</li>
     *   <li>在操作数的使用列表中记录当前用户</li>
     * </ul>
     * 
     * @param operand 要添加的操作数
     */
    public void addOperand(IRValue operand) {
        operandList.add(operand);
        if (operand != null) {
            operand.addUseRelation(this);
        }
    }
    
    /**
     * 获取指定位置的操作数
     * 
     * @param index 操作数索引
     * @return 指定位置的操作数，如果索引无效则返回null
     */
    public IRValue getOperand(int index) {
        if (index >= 0 && index < operandList.size()) {
            return operandList.get(index);
        }
        return null;
    }
    
    /**
     * 获取所有操作数的副本
     * 
     * <p>返回操作数列表的副本，避免外部直接修改内部列表。
     * 
     * @return 操作数列表的副本
     */
    public List<IRValue> getAllOperands() {
        return new ArrayList<>(operandList);
    }
    
    /**
     * 获取操作数的个数
     * 
     * @return 操作数个数
     */
    public int getOperandCount() {
        return operandList.size();
    }
    
    /**
     * 替换指定的操作数
     * 
     * <p>该方法会自动维护Use-Def关系：
     * <ul>
     *   <li>从旧操作数的使用列表中移除当前用户</li>
     *   <li>在新操作数的使用列表中添加当前用户</li>
     *   <li>更新当前用户的操作数列表</li>
     * </ul>
     * 
     * <p>这个方法在编译器优化中非常重要，用于：
     * <ul>
     *   <li>常量传播：将变量替换为常量</li>
     *   <li>死代码消除：替换无用的操作数</li>
     *   <li>代码重构：更新指令的操作数</li>
     * </ul>
     * 
     * @param oldOperand 要被替换的旧操作数
     * @param newOperand 新的操作数
     */
    public void replaceOperand(IRValue oldOperand, IRValue newOperand) {
        for (int i = 0; i < operandList.size(); i++) {
            if (operandList.get(i) == oldOperand) {
                operandList.set(i, newOperand);
                // 更新Use-Def关系
                if (oldOperand != null) {
                    oldOperand.removeUseBy(this);
                }
                if (newOperand != null) {
                    newOperand.addUseRelation(this);
                }
            }
        }
    }
    
    /**
     * 按索引替换操作数
     * 
     * <p>与{@link #replaceOperand(IRValue, IRValue)}类似，但通过索引定位要替换的操作数。
     * 
     * @param index 要替换的操作数索引
     * @param newOperand 新的操作数
     */
    public void replaceOperand(int index, IRValue newOperand) {
        if (index >= 0 && index < operandList.size()) {
            IRValue oldOperand = operandList.get(index);
            operandList.set(index, newOperand);
            // 更新Use-Def关系
            if (oldOperand != null) {
                oldOperand.removeUseBy(this);
            }
            if (newOperand != null) {
                newOperand.addUseRelation(this);
            }
        }
    }
    
    /**
     * 清除所有操作数
     * 
     * <p>该方法会：
     * <ul>
     *   <li>从所有操作数的使用列表中移除当前用户</li>
     *   <li>清空当前用户的操作数列表</li>
     * </ul>
     * 
     * <p>通常在删除指令或重构代码时使用。
     */
    public void clearAllOperands() {
        for (IRValue operand : operandList) {
            if (operand != null) {
                operand.removeUseBy(this);
            }
        }
        operandList.clear();
    }
}