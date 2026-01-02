package middle.llvm.value;

import middle.llvm.type.IRType;
import middle.llvm.type.PointerType;
import middle.llvm.value.constant.IRConstant;

/**
 * LLVM IR全局变量实现
 * 
 * <p>表示LLVM IR中的全局变量和全局常量，存储在程序的全局数据段中。
 * 全局变量在LLVM IR中具有以下特征：
 * <ul>
 *   <li>具有全局作用域，在整个程序中可见</li>
 *   <li>在程序启动时分配内存并初始化</li>
 *   <li>可以是可变的全局变量或不可变的全局常量</li>
 *   <li>支持各种数据类型：基本类型、数组、结构体等</li>
 *   <li>具有明确的初始值或零初始化</li>
 * </ul>
 * 
 * <p>LLVM IR表示示例：
 * <pre>
 * ; 全局整数变量
 * @g_a = dso_local global i32 97
 * 
 * ; 全局数组变量（部分初始化）
 * @g_arr = dso_local global [10 x i32] [i32 1, i32 2, i32 3, i32 0, i32 0, i32 0, i32 0, i32 0, i32 0, i32 0]
 * 
 * ; 全局数组变量（零初始化）
 * @g_zeros = dso_local global [20 x i32] zeroinitializer
 * 
 * ; 全局字符串常量
 * @g_str = dso_local constant [8 x i8] c"foobar\00\00", align 1
 * 
 * ; 全局常量
 * @g_const = dso_local constant i32 42
 * </pre>
 * 
 * <p>对应SysY语言场景：
 * <ul>
 *   <li>全局变量声明：{@code int a = 97;} 对应 {@code @g_a = dso_local global i32 97}</li>
 *   <li>全局数组声明：{@code int arr[10] = {1, 2, 3};} 对应部分初始化的全局数组</li>
 *   <li>全局常量声明：{@code const int MAX = 100;} 对应 {@code @g_MAX = dso_local constant i32 100}</li>
 *   <li>字符串字面量：{@code "hello"} 对应全局字符数组常量</li>
 * </ul>
 * 
 * <p>内存布局特点：
 * <ul>
 *   <li>存储在程序的数据段或只读数据段</li>
 *   <li>程序启动时自动分配和初始化</li>
 *   <li>生命周期贯穿整个程序执行过程</li>
 *   <li>通过指针类型进行访问</li>
 * </ul>
 * 
 * @see IRConstant 常量值
 * @see PointerType 指针类型
 * @see IRValue 值基类
 */
public class IRGlobalVariable extends IRValue {
    /**
     * 全局变量的初始值
     * 必须是编译时常量，用于在程序启动时初始化变量
     */
    private final IRConstant initializer;
    
    /**
     * 标识是否为全局常量
     * true表示constant（不可修改），false表示global（可修改）
     */
    private final boolean isConstant;
    
    /**
     * 构造全局变量
     * 
     * <p>全局变量的类型总是指针类型，指向实际存储的数据类型。
     * 这是因为在LLVM IR中，全局变量名实际上是指向全局数据的指针。
     * 
     * @param name 变量名（不包含@前缀）
     * @param initializer 初始值常量
     * @param isConstant 是否为常量（true=constant, false=global）
     */
    public IRGlobalVariable(String name, IRConstant initializer, boolean isConstant) {
        super(null, "@g_" + name, new PointerType(initializer.getType()));
        this.initializer = initializer;
        this.isConstant = isConstant;
    }
    
    /**
     * 获取全局变量的初始值
     * 
     * @return 初始值常量，用于在程序启动时初始化变量
     */
    public IRConstant getInit() {
        return initializer;
    }
    
    /**
     * 判断是否为全局常量
     * 
     * @return true表示是constant（不可修改），false表示是global（可修改）
     */
    public boolean isConstant() {
        return isConstant;
    }
    
    /**
     * 获取全局变量指向的实际数据类型
     * 
     * <p>由于全局变量的类型是指针类型，此方法返回指针指向的实际类型。
     * 例如：{@code @g_a = global i32 0} 中，变量类型是 {@code i32*}，
     * 指向的类型是 {@code i32}。
     * 
     * @return 指向的实际数据类型
     */
    public IRType getPointeeType() {
        return ((PointerType) getType()).getPointeeType();
    }
    
    /**
     * 判断全局变量是否为数组类型
     * 
     * @return 如果指向的类型是数组类型则返回true，否则返回false
     */
    public boolean isArrayType() {
        return getPointeeType() instanceof middle.llvm.type.ArrayType;
    }
    
    /**
     * 生成全局变量的LLVM IR声明字符串
     * 
     * <p>格式：{@code @name = dso_local [constant|global] <type> <initializer>}
     * 
     * <p>生成示例：
     * <ul>
     *   <li>{@code @g_a = dso_local global i32 97} - 全局整数变量</li>
     *   <li>{@code @g_const = dso_local constant i32 42} - 全局整数常量</li>
     *   <li>{@code @g_arr = dso_local global [10 x i32] zeroinitializer} - 零初始化数组</li>
     * </ul>
     * 
     * @return 全局变量的LLVM IR声明字符串
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append(getName()).append(" = dso_local ");
        
        if (isConstant) {
            builder.append("constant ");
        } else {
            builder.append("global ");
        }
        
        IRType pointeeType = getPointeeType();
        builder.append(pointeeType.toString()).append(" ");
        
        // 初始值
        if (initializer != null) {
            builder.append(initializer.toString());
        } else {
            // 零初始化
            if (pointeeType instanceof middle.llvm.type.ArrayType) {
                builder.append("zeroinitializer");
            } else {
                builder.append("0");
            }
        }
        
        return builder.toString();
    }
}