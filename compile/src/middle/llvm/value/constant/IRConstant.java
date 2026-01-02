package middle.llvm.value.constant;

import middle.llvm.type.IRType;
import middle.llvm.value.IRValue;

import java.util.List;

/**
 * LLVM IR常量值基类
 * 
 * 表示LLVM IR中的常量值，这些值在编译时就已经确定，不会在运行时改变。
 * 常量值是LLVM IR中的重要概念，用于表示字面量、全局变量初始值等。
 * 
 * 在LLVM IR中的表示形式：
 * - 整数常量：42, 0, -1
 * - 数组常量：[i32 1, i32 2, i32 3]
 * - 零初始化：zeroinitializer
 * - 字符串常量：c"hello\00"
 * 
 * 常量的使用场景：
 * - 全局变量初始化：@global_var = global i32 42
 * - 数组初始化：@array = global [3 x i32] [i32 1, i32 2, i32 3]
 * - 指令操作数：%result = add i32 %var, 10
 * - 函数调用参数：call void @func(i32 42)
 * 
 * 对应SysY语言中的常量：
 * - const int x = 42;        -> i32 42
 * - const int arr[] = {1,2}; -> [i32 1, i32 2]
 * - 字符串字面量 "hello"     -> c"hello\00"
 * 
 * 常量的特性：
 * - 编译时确定：值在编译阶段就已知
 * - 不可变性：运行时不能修改常量的值
 * - 类型安全：每个常量都有明确的类型
 * - 内存效率：相同的常量值可以共享存储
 */
public abstract class IRConstant extends IRValue {
    
    /**
     * 构造常量值
     * 
     * 常量值没有父容器，因为它们是全局可见的
     * 
     * @param type 常量的类型
     */
    protected IRConstant(IRType type) {
        super(null, type);  // 常量没有父容器
    }
    
    /**
     * 判断常量是否包含字符类型
     * 
     * 用于区分字符数据和普通整数数据，影响代码生成和优化策略。
     * 字符类型通常对应i8类型，用于字符串处理。
     * 
     * @return 对于整数常量，判断是否为i8类型；对于数组常量，判断元素是否为i8类型
     */
    public boolean containsCharacterType() {
        return false;
    }
    
    /**
     * 获取常量的所有数值
     * 
     * 将常量值展开为整数列表，用于数据分析和优化。
     * - 对于整数常量：返回包含单个值的列表
     * - 对于数组常量：返回所有元素值的列表
     * 
     * @return 常量包含的所有整数值列表
     */
    public abstract List<Integer> getAllNumbers();
    
    /**
     * 判断是否为零值常量
     * 
     * 零值常量在LLVM IR中有特殊意义：
     * - 可以使用zeroinitializer表示
     * - 在内存分配时可以优化为清零操作
     * - 在条件判断中表示false
     * 
     * @return 如果是零值常量返回true
     */
    public abstract boolean isZeroValue();
    
    /**
     * 判断是否仅为占位符
     * 
     * 占位符常量用于编译过程中的临时表示，
     * 不会出现在最终的LLVM IR代码中。
     * 
     * @return 如果仅为占位符返回true
     */
    public boolean isPlaceholder() {
        return false;
    }
}