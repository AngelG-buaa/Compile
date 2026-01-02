package middle.llvm.value.instruction;

import middle.llvm.value.IRValue;
import back.mips.register.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * LLVM IR函数调用指令
 * 
 * 表示LLVM IR中的函数调用指令，用于调用函数并传递参数。
 * 
 * 特性：
 * - 支持有返回值和无返回值两种调用：根据函数签名确定
 * - 参数传递：按顺序传递实际参数给被调用函数
 * - 类型检查：参数类型必须与函数签名匹配
 * - 可能有副作用：函数调用可能修改全局状态或执行I/O操作
 * - 控制流转移：暂时将控制权转移给被调用函数
 * 
 * LLVM IR语法示例：
 * - %result = call i32 @func(i32 %a, i32 %b)     ; 有返回值的函数调用
 * - call void @print(i8* %str)                   ; 无返回值的函数调用
 * - %val = call i32 @getchar()                   ; 无参数的函数调用
 * - call void @exit(i32 0)                       ; 系统函数调用
 * 
 * 对应SysY语言场景：
 * - 函数调用表达式：result = func(a, b)
 * - 过程调用语句：print("Hello")
 * - 库函数调用：getint(), putint(x)
 * - 递归函数调用：factorial(n-1)
 * - 表达式中的函数调用：a + func(b) * c
 * 
 * 调用约定：
 * - 参数按从左到右的顺序传递
 * - 返回值通过寄存器或栈传递
 * - 调用者负责参数准备和结果接收
 * - 被调用者负责参数使用和返回值生成
 * 
 * 与其他指令的配合：
 * - 与load指令配合：加载参数值
 * - 与store指令配合：存储返回值
 * - 与alloca指令配合：为参数分配临时空间
 * - 与分支指令配合：根据返回值进行条件跳转
 * 
 * 编译器优化相关：
 * - 内联优化：将小函数直接展开到调用点
 * - 尾调用优化：将尾递归转换为循环
 * - 死代码消除：移除未使用返回值的纯函数调用
 * - 常量传播：传播常量参数到函数内部
 */
public class CallInstruction extends IRInstruction {
    /**
     * 是否有返回值标志
     * 标识此函数调用是否产生返回值
     */
    private final boolean hasReturnValue;

    public HashSet<Reg> liveRegSet = new HashSet<>();
    
    /**
     * 创建有返回值的函数调用指令
     * 
     * @param parentBlock 所属基本块
     * @param nameCounter 名称计数器，用于生成唯一的结果变量名
     * @param function 被调用的函数
     * @param arguments 函数参数列表
     */
    public CallInstruction(IRValue parentBlock, int nameCounter, IRValue function, List<IRValue> arguments) {
        super(parentBlock, "%call" + nameCounter, ((middle.llvm.type.FunctionType) function.getType()).getReturnType());
        addOperand(function);
        if (arguments != null && !arguments.isEmpty()) {
            for (IRValue argument : arguments) {
                addOperand(argument);
            }
        }
        hasReturnValue = true;
    }
    
    /**
     * 创建无返回值的函数调用指令
     * 
     * @param parentBlock 所属基本块
     * @param function 被调用的函数
     * @param arguments 函数参数列表
     */
    public CallInstruction(IRValue parentBlock, IRValue function, List<IRValue> arguments) {
        super(parentBlock, ((middle.llvm.type.FunctionType) function.getType()).getReturnType());
        addOperand(function);
        if (arguments != null && !arguments.isEmpty()) {
            for (IRValue argument : arguments) {
                addOperand(argument);
            }
        }
        hasReturnValue = false;
    }
    
    /**
     * 获取被调用的函数
     * 
     * @return 被调用的函数对象
     */
    public IRValue getCalledFunction() {
        return getOperand(0);
    }
    
    /**
     * 判断是否有返回值
     * 
     * @return 如果函数调用有返回值则返回true，否则返回false
     */
    public boolean hasReturnValue() {
        return hasReturnValue;
    }
    
    /**
     * 获取函数参数列表
     * 
     * @return 传递给函数的参数列表
     */
    public List<IRValue> getArguments() {
        List<IRValue> arguments = new ArrayList<>();
        for (int i = 1; i < getOperandCount(); i++) {
            arguments.add(getOperand(i));
        }
        return arguments;
    }
    
    /**
     * 检查是否有副作用
     * 函数调用通常被认为有副作用，因为可能修改全局状态
     * 
     * @return 总是返回true，表示函数调用可能有副作用
     */
    @Override
    public boolean hasSideEffects() {
        return true; // 函数调用可能有副作用
    }
    
    /**
     * 获取指令操作码名称
     * 
     * @return 返回"call"，表示这是一个函数调用指令
     */
    @Override
    public String getOpcodeName() {
        return "call";
    }
    
    /**
     * 生成LLVM IR字符串表示
     * 
     * @return 完整的LLVM IR函数调用指令字符串
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        IRValue function = getCalledFunction();
        
        if (hasReturnValue) {
            builder.append(getName()).append(" = ");
        }
        
        // 修复：使用函数的返回类型而不是函数类型
        middle.llvm.type.FunctionType funcType = (middle.llvm.type.FunctionType) function.getType();
        builder.append("call ").append(funcType.getReturnType()).append(" ").append(function.getName()).append("(");
        
        List<IRValue> arguments = getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            IRValue arg = arguments.get(i);
            builder.append(arg.getType()).append(" ").append(arg.getName());
            if (i < arguments.size() - 1) {
                builder.append(", ");
            }
        }
        
        builder.append(")");
        return builder.toString();
    }
}