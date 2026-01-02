package middle.llvm.value.constant;

import middle.llvm.type.IntegerType;

import java.util.ArrayList;
import java.util.List;

/**
 * LLVM IR整数常量实现
 * 
 * 表示LLVM IR中的整数常量值，包括各种位宽的整数类型。
 * 整数常量是最基本的常量类型，广泛用于算术运算、比较操作等。
 * 
 * 在LLVM IR中的表示形式：
 * - i1类型：true (1), false (0)
 * - i8类型：42, -1, 255
 * - i32类型：1000, -2147483648, 2147483647
 * 
 * 使用示例：
 * - %result = add i32 %var, 42        ; 42是i32整数常量
 * - %cond = icmp eq i32 %x, 0         ; 0是i32整数常量
 * - store i8 65, i8* %ptr             ; 65是i8整数常量（字符'A'）
 * - br i1 true, label %then, label %else  ; true是i1整数常量
 * 
 * 对应SysY语言中的常量：
 * - const int x = 42;          -> i32 42
 * - const char c = 'A';        -> i8 65
 * - if (true) { ... }          -> i1 1 (true)
 * - while (x != 0) { ... }     -> 比较中的i32 0
 * 
 * 特殊值的含义：
 * - 0：通常表示false、null指针、数组/结构体的零初始化
 * - 1：通常表示true
 * - -1：在无符号上下文中表示最大值（如i8的255）
 */
public class IntegerConstant extends IRConstant {
    
    /**
     * 常量的整数值
     */
    private final int constantValue;
    
    /**
     * 是否仅为占位符标记
     * 占位符常量用于编译过程中的临时表示
     */
    private final boolean isPlaceholderOnly;
    
    /**
     * 构造整数常量
     * 
     * @param type 整数类型（i1, i8, i32等）
     * @param constantValue 常量值
     */
    public IntegerConstant(IntegerType type, int constantValue) {
        super(type);
        this.constantValue = constantValue;
        this.isPlaceholderOnly = false;
    }
    
    /**
     * 构造整数常量（可指定占位符标记）
     * 
     * @param type 整数类型
     * @param constantValue 常量值
     * @param isPlaceholderOnly 是否仅为占位符
     */
    public IntegerConstant(IntegerType type, int constantValue, boolean isPlaceholderOnly) {
        super(type);
        this.constantValue = constantValue;
        this.isPlaceholderOnly = isPlaceholderOnly;
    }
    
    /**
     * 获取常量值
     * 
     * @return 整数常量的值
     */
    public int getConstantValue() {
        return constantValue;
    }
    
    /**
     * 判断是否包含字符类型
     * 
     * 字符类型对应i8，用于字符串和字符处理
     * 
     * @return 如果是i8类型返回true
     */
    @Override
    public boolean containsCharacterType() {
        return ((IntegerType) getType()).isByteType();
    }
    
    /**
     * 获取常量的所有数值
     * 
     * 对于整数常量，返回包含单个值的列表
     * 
     * @return 包含常量值的列表
     */
    @Override
    public List<Integer> getAllNumbers() {
        List<Integer> result = new ArrayList<>();
        result.add(constantValue);
        return result;
    }
    
    /**
     * 判断是否为零值常量
     * 
     * 零值在LLVM IR中有特殊意义：
     * - i1 0 表示false
     * - i32 0 表示整数零
     * - 指针比较中的null值
     * 
     * @return 如果常量值为0返回true
     */
    @Override
    public boolean isZeroValue() {
        return constantValue == 0;
    }
    
    /**
     * 获取常量名称
     * 
     * 整数常量的名称就是其字符串表示
     * 
     * @return 常量值的字符串形式
     */
    @Override
    public String getName() {
        return toString();
    }
    
    /**
     * 判断是否仅为占位符
     * 
     * @return 占位符标记
     */
    @Override
    public boolean isPlaceholder() {
        return isPlaceholderOnly;
    }
    
    /**
     * 生成LLVM IR格式的整数常量字符串
     * 
     * 直接输出数值，不包含类型信息
     * 例如：42, 0, -1
     * 
     * @return 常量值的字符串表示
     */
    @Override
    public String toString() {
        return String.valueOf(constantValue);
    }
}