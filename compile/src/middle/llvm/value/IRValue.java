package middle.llvm.value;

import middle.llvm.type.IRType;
import middle.llvm.UseDefChain;

import java.util.ArrayList;
import java.util.List;

/**
 * LLVM IR值系统的基类
 * 
 * 这是所有LLVM IR值的抽象基类，表示可以在LLVM IR中被使用和操作的实体。
 * 
 * LLVM IR值系统包括：
 * - 常量值：整数常量、字符串常量等编译时确定的值
 * - 指令值：指令执行后产生的临时值，如算术运算结果
 * - 函数参数：函数的形式参数
 * - 全局变量：程序中的全局变量和函数
 * - 基本块：控制流的基本单元
 * 
 * 在LLVM IR中的表示形式：
 * - %1 = add i32 %a, %b        ; %1是add指令产生的值
 * - %ptr = alloca i32          ; %ptr是alloca指令产生的指针值
 * - @global_var = global i32 0 ; @global_var是全局变量值
 * - define i32 @func(i32 %arg) ; %arg是函数参数值
 * 
 * 值的使用-定义链（Use-Def Chain）：
 * - 每个值都维护一个使用列表，记录哪些指令使用了这个值
 * - 支持值的替换操作，用于优化和代码变换
 * - 提供使用关系的管理，便于进行数据流分析
 * 
 * 值的命名规则：
 * - 局部值：以%开头，如%1, %temp, %result
 * - 全局值：以@开头，如@main, @global_var
 * - 匿名值：系统自动分配数字编号
 */
public class IRValue {
    
    /**
     * 全局唯一ID计数器
     * 用于为每个值分配唯一标识符
     */
    private static int globalIdCounter = 0;
    
    /**
     * 值的唯一标识符
     * 用于区分不同的值实例
     */
    private final int uniqueId;
    
    /**
     * 值的名称
     * 在LLVM IR中显示的名称，如%temp, @main等
     * 可以为null（匿名值）
     */
    private String valueName;
    
    /**
     * 值的类型
     * 描述这个值的LLVM IR类型，如i32, i32*, [10 x i32]等
     */
    private final IRType valueType;
    
    /**
     * 使用该值的Use关系列表
     * 记录所有使用这个值的指令或其他值
     * 用于实现Use-Def链，支持值替换和优化
     */
    private final List<UseDefChain> useList;
    
    /**
     * 容器值
     * 表示包含这个值的上级容器，如函数、基本块等
     */
    private IRValue containerValue;
    
    /**
     * 构造具名IR值
     * 
     * @param containerValue 容器值
     * @param valueName 值的名称
     * @param valueType 值的类型
     */
    public IRValue(IRValue containerValue, String valueName, IRType valueType) {
        this.containerValue = containerValue;
        this.uniqueId = globalIdCounter++;
        this.valueName = valueName;
        this.valueType = valueType;
        this.useList = new ArrayList<>();
    }
    
    /**
     * 构造匿名IR值
     * 
     * @param containerValue 容器值
     * @param valueType 值的类型
     */
    public IRValue(IRValue containerValue, IRType valueType) {
        this.containerValue = containerValue;
        this.uniqueId = globalIdCounter++;
        this.valueName = null;
        this.valueType = valueType;
        this.useList = new ArrayList<>();
    }
    
    /**
     * 添加使用关系
     * 
     * 当某个指令或值使用了这个值时，调用此方法建立Use-Def关系
     * 
     * @param user 使用这个值的用户（指令或其他值）
     */
    public void addUseRelation(IRUser user) {
        UseDefChain useChain = new UseDefChain(user, this);
        useList.add(useChain);
    }
    
    /**
     * 获取所有使用关系
     * 
     * @return 使用关系列表的副本
     */
    public List<UseDefChain> getUseList() {
        return new ArrayList<>(useList);
    }
    
    /**
     * 将所有使用该值的地方替换为新值
     * 
     * 这是LLVM IR优化中的重要操作，用于：
     * - 常量传播：将变量替换为常量
     * - 死代码消除：移除未使用的值
     * - 指令合并：用更简单的指令替换复杂指令
     * 
     * @param newValue 新的替换值
     */
    public void replaceAllUsesWith(IRValue newValue) {
        // 创建useList的副本，避免在迭代过程中修改原集合导致ConcurrentModificationException
        List<UseDefChain> useListCopy = new ArrayList<>(useList);
        for (UseDefChain useChain : useListCopy) {
            IRUser user = useChain.user();
            user.replaceOperand(this, newValue);
        }
        useList.clear();
    }
    
    /**
     * 清除指定用户的使用关系
     * 
     * 当某个指令被删除或不再使用这个值时调用
     * 
     * @param user 不再使用这个值的用户
     */
    public void removeUseBy(IRUser user) {
        useList.removeIf(useChain -> {
            if (useChain.user() == user) {
                return true;
            }
            return false;
        });
    }
    
    /**
     * 获取值的类型
     * type描述这个值的LLVM IR类型，如i32, i32*, [10 x i32]等
     * @return 值的LLVM IR类型
     */
    public IRType getType() {
        return valueType;
    }
    
    /**
     * 设置值的名称
     * 
     * @param valueName 新的值名称
     */
    public void setName(String valueName) {
        this.valueName = valueName;
    }
    
    /**
     * 获取值的名称
     * 
     * @return 值的名称，可能为null（匿名值）
     */
    public String getName() {
        return valueName;
    }
    
    /**
     * 获取容器值
     * 
     * @return 包含这个值的容器
     */
    public IRValue getContainer() {
        return containerValue;
    }

    /**
     * 设置容器值
     *
     * @param containerValue 新的容器值
     */
    public void setContainer(IRValue containerValue) {
        this.containerValue = containerValue;
    }
    
    /**
     * 获取唯一标识符
     * 
     * @return 值的唯一ID
     */
    public int getUniqueId() {
        return uniqueId;
    }
    
    /**
     * 判断是否为占位符
     * 
     * 占位符是临时创建的值，通常用于：
     * - 构建过程中的临时值
     * - 尚未完全初始化的值
     * 
     * @return 如果是占位符返回true
     */
    public boolean isPlaceholder() {
        return valueName == null && useList.isEmpty();
    }
}