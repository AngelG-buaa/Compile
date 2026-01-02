package middle.llvm.type;

/**
 * LLVM IR指针类型实现
 * 
 * 表示LLVM IR中的指针类型，用于指向其他类型的内存地址。
 * 
 * 在LLVM IR中的表示形式：
 * - i32*: 指向32位整数的指针
 * - i8*: 指向字节的指针（通常用于字符串或通用指针）
 * - [10 x i32]*: 指向包含10个32位整数数组的指针
 * 
 * 对应SysY语言中的使用场景：
 * - 函数参数传递数组时：void func(int arr[]) -> void func(i32*)
 * - 动态分配的内存：malloc返回i8*
 * - 变量的地址：&variable 得到该变量类型的指针
 * 
 * 指针操作的LLVM IR示例：
 * - %ptr = alloca i32              ; 分配i32*类型的栈空间
 * - %val = load i32, i32* %ptr     ; 从指针加载值
 * - store i32 %val, i32* %ptr      ; 向指针存储值
 * - %elem_ptr = getelementptr [10 x i32], [10 x i32]* %arr, i32 0, i32 %idx
 */
public class PointerType extends IntegerType {
    
    /**
     * 指针指向的类型（被解引用后的类型）
     * 可以是：
     * - IntegerType: i8, i32等基本类型
     * - ArrayType: 数组类型
     * - 其他复合类型
     */
    private final IRType pointeeType;
    
    /**
     * 构造指针类型
     * 
     * @param pointeeType 指针指向的类型
     */
    public PointerType(IRType pointeeType) {
        super(32); // 指针本身在32位系统中占用32位
        this.pointeeType = pointeeType;
    }
    
    /**
     * 获取指针指向的类型
     * 
     * @return 被指向的类型
     */
    public IRType getPointeeType() {
        return pointeeType;
    }
    
    @Override
    public boolean isPointerType() {
        return true;
    }
    
    @Override
    public boolean isBasicIntegerType() {
        // 指针类型不是基本整数类型
        return false;
    }
    
    /**
     * 判断是否为数组指针
     * 
     * 数组指针用于：
     * - 函数参数中的数组传递
     * - 多维数组的访问
     * 
     * 例如：[10 x i32]* 表示指向包含10个i32元素数组的指针
     * 
     * @return 如果指向数组类型返回true
     */
    public boolean isArrayPointer() {
        return pointeeType.isArrayType();
    }
    
    /**
     * 判断是否为整数指针
     * 
     * 整数指针用于：
     * - 指向单个整数变量
     * - 作为函数参数传递整数的引用
     * 
     * 例如：i32* 表示指向32位整数的指针
     * 
     * @return 如果指向整数类型返回true
     */
    public boolean isIntegerPointer() {
        return pointeeType.isBasicIntegerType();
    }
    
    @Override
    public int getByteSize() {
        // 在32位系统中，所有指针都占用4字节
        return 4;
    }
    
    @Override
    public int getAlignment() {
        // 指针的对齐要求为4字节
        return 4;
    }
    
    /**
     * 返回LLVM IR格式的指针类型字符串
     * 
     * @return 格式为 "pointee_type*" 的字符串
     *         例如: "i32*", "i8*", "[10 x i32]*"
     */
    @Override
    public String toString() {
        return pointeeType.toString() + "*";
    }
}