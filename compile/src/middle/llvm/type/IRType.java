package middle.llvm.type;

/**
 * LLVM IR类型系统的基类
 * 
 * 这是所有LLVM IR类型的抽象基类，定义了类型系统的通用接口。
 * LLVM IR类型系统包括：
 * - 基本整数类型：i1(布尔), i8(字节), i32(整数)
 * - 指针类型：指向其他类型的指针，如i32*, [10 x i32]*
 * - 数组类型：固定长度的数组，如[10 x i32], [256 x i8]
 * - 函数类型：函数签名，如i32 (i32, i32)
 * - void类型：表示无返回值
 * - label类型：基本块标签
 * 
 * 对应LLVM IR中的类型表示：
 * - i1: 1位整数（布尔值）
 * - i8: 8位整数（字符）
 * - i32: 32位整数
 * - i32*: 指向32位整数的指针
 * - [10 x i32]: 包含10个32位整数的数组
 * - void: 空类型
 * - label: 基本块标签类型
 */
public abstract class IRType {

    /**
     * 判断是否为基本整数类型(i1, i8, i32)
     * 
     * @return 如果是基本整数类型返回true，否则返回false
     */
    public boolean isBasicIntegerType() {
        return false;
    }

    public boolean isI() {
        return false;
    }
    
    /**
     * 判断是否为指针类型
     * 
     * 指针类型在LLVM IR中表示为 "type*"，如：
     * - i32*: 指向32位整数的指针
     * - [10 x i32]*: 指向数组的指针
     * 
     * @return 如果是指针类型返回true，否则返回false
     */
    public boolean isPointerType() {
        return false;
    }
    
    /**
     * 判断是否为数组类型
     * 
     * 数组类型在LLVM IR中表示为 "[length x element_type]"，如：
     * - [10 x i32]: 包含10个32位整数的数组
     * - [256 x i8]: 包含256个字节的数组（通常用于字符串）
     * 
     * @return 如果是数组类型返回true，否则返回false
     */
    public boolean isArrayType() {
        return false;
    }
    
    /**
     * 判断是否为函数类型
     * 
     * 函数类型在LLVM IR中表示函数签名，如：
     * - i32 (i32, i32): 接受两个32位整数参数，返回32位整数的函数
     * - void (): 无参数无返回值的函数
     * 
     * @return 如果是函数类型返回true，否则返回false
     */
    public boolean isFunctionType() {
        return false;
    }
    
    /**
     * 获取类型占用的字节数
     * 
     * 不同类型的字节大小：
     * - i1: 1字节（最小存储单位）
     * - i8: 1字节
     * - i32: 4字节
     * - 指针: 4字节（32位系统）
     * - 数组: 元素大小 × 数组长度
     * 
     * @return 类型占用的字节数
     */
    public abstract int getByteSize();
    
    /**
     * 获取类型的内存对齐要求
     * 
     * 内存对齐用于优化内存访问性能，通常等于类型的字节大小
     * 
     * @return 对齐字节数
     */
    public abstract int getAlignment();
    
    /**
     * 返回类型的LLVM IR字符串表示
     * 
     * @return LLVM IR格式的类型字符串
     */
    @Override
    public abstract String toString();
}