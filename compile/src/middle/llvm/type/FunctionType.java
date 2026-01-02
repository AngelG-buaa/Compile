package middle.llvm.type;

import java.util.ArrayList;
import java.util.List;

/**
 * LLVM IR函数类型实现
 * 
 * 表示LLVM IR中的函数类型，用于描述函数的签名（返回类型和参数类型）。
 * 
 * 在LLVM IR中的表示形式：
 * - i32 (i32, i32): 接受两个i32参数，返回i32的函数
 * - void (): 无参数无返回值的函数
 * - i32 (i32*, i32): 接受一个i32指针和一个i32参数，返回i32的函数
 * - void ([10 x i32]*): 接受一个数组指针参数，无返回值的函数
 * 
 * 对应SysY语言中的函数声明：
 * - int add(int a, int b)           -> i32 (i32, i32)
 * - void print()                    -> void ()
 * - int sum(int arr[], int n)       -> i32 (i32*, i32)
 * - void main()                     -> void ()
 * 
 * 函数类型在LLVM IR中的使用示例：
 * - define i32 @add(i32 %a, i32 %b) { ... }
 * - declare void @printf(i8*, ...)
 * - %result = call i32 @add(i32 %x, i32 %y)
 */
public class FunctionType extends IRType {
    
    /**
     * 函数的返回类型
     * 可以是：
     * - IntegerType: i32等整数类型
     * - VoidType: void类型（无返回值）
     */
    private final IntegerType returnType;
    
    /**
     * 函数的参数类型列表
     * 每个参数都是IntegerType（包括指针类型）
     */
    private final ArrayList<IntegerType> parameterTypes;
    
    /**
     * 构造函数类型
     * 
     * @param returnType 返回类型
     * @param parameterTypes 参数类型列表
     */
    public FunctionType(IntegerType returnType, List<IntegerType> parameterTypes) {
        this.returnType = returnType;
        this.parameterTypes = (ArrayList<IntegerType>) parameterTypes;
    }
    
    /**
     * 获取函数返回类型
     * 
     * @return 返回类型
     */
    public IntegerType getReturnType() {
        return returnType;
    }
    
    /**
     * 获取函数参数类型列表
     *
     * @return 参数类型列表
     */
    public ArrayList<IntegerType> getParameterTypes() {
        return (ArrayList)parameterTypes;
    }
    
    /**
     * 获取参数个数
     * 
     * @return 参数数量
     */
    public int getParameterCount() {
        return parameterTypes.size();
    }
    
    @Override
    public boolean isFunctionType() {
        return true;
    }
    
    @Override
    public int getByteSize() {
        // 函数类型本身不占用存储空间
        // 函数指针在32位系统中占用4字节，但这里返回0表示类型本身不占空间
        throw new UnsupportedOperationException("Function type does not have a byte size");
    }
    
    @Override
    public int getAlignment() {
        // 函数类型本身没有对齐要求
        throw new UnsupportedOperationException("Function type does not have alignment requirements");
    }
    
    /**
     * 返回LLVM IR格式的函数类型字符串
     * 
     * @return 格式为 "return_type (param1_type, param2_type, ...)" 的字符串
     *         例如: "i32 (i32, i32)", "void ()", "i32 (i32*, i32)"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType.toString()).append(" (");
        
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameterTypes.get(i).toString());
        }
        
        sb.append(")");
        return sb.toString();
    }
}