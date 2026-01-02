package middle.llvm.type;

/**
 * LLVM IR void类型实现
 * 
 * 表示LLVM IR中的void类型，用于表示"无类型"或"无返回值"。
 * 
 * 在LLVM IR中的使用场景：
 * - 函数返回类型：void表示函数不返回任何值
 * - 不能用于变量声明：void类型不能作为变量的类型
 * - 不能进行算术运算：void类型没有具体的值
 * 
 * 在LLVM IR中的表示形式：
 * - define void @func() { ... }     ; void返回类型的函数
 * - call void @func()               ; 调用void函数
 * 
 * 对应SysY语言中的使用：
 * - void main() { ... }             -> define void @main() { ... }
 * - void print(int x) { ... }       -> define void @print(i32 %x) { ... }
 * 
 * 注意事项：
 * - void类型不能用于计算字节大小或内存对齐
 * - void类型不能作为指针的目标类型（在某些上下文中）
 * - void类型主要用于类型检查和函数签名定义
 */
public class VoidType extends IntegerType {
    /**
     * 构造void类型
     *
     * 继承IntegerType是为了类型系统的统一性，
     * 但void类型不是真正的整数类型
     */
    public VoidType() {
        super(0); // void类型没有位宽概念，设为0
    }
    
    @Override
    public boolean isBasicIntegerType() {
        // void类型不是基本整数类型
        return false;
    }
    
    /**
     * 判断是否为void类型
     * 
     * @return 始终返回true
     */
    public boolean isVoidType() {
        return true;
    }
    
    @Override
    public int getByteSize() {
        // void类型不应该被用于计算字节大小
        throw new UnsupportedOperationException("Void type should not be used for byte size calculation: " + getClass());
    }
    
    @Override
    public int getAlignment() {
        // void类型不应该被用于计算内存对齐
        throw new UnsupportedOperationException("Void type should not be used for alignment calculation: " + getClass());
    }
    
    /**
     * 返回LLVM IR格式的void类型字符串
     * 
     * @return "void"
     */
    @Override
    public String toString() {
        return "void";
    }
}