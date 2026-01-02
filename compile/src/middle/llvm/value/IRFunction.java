package middle.llvm.value;

import middle.llvm.type.FunctionType;
import middle.llvm.type.IRType;
import middle.llvm.type.IntegerType;
import middle.llvm.type.PointerType;
import middle.llvm.type.VoidType;
import middle.llvm.value.instruction.IRInstruction;
import back.mips.register.Reg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * LLVM IR函数值实现
 * 
 * 表示LLVM IR中的函数定义，是程序的基本执行单元。
 * 
 * 在LLVM IR中的表示形式：
 * - define i32 @main() { ... }              ; 主函数定义
 * - define i32 @add(i32 %a, i32 %b) { ... } ; 带参数的函数
 * - declare i32 @getint()                    ; 外部函数声明
 * - declare void @putint(i32)                ; 库函数声明
 * 
 * 函数的组成部分：
 * - 函数名：以@开头的全局标识符，如@main, @add
 * - 返回类型：函数返回值的类型，如i32, void
 * - 参数列表：形式参数及其类型，如(i32 %a, i32 %b)
 * - 基本块序列：函数体由多个基本块组成
 * 
 * 对应SysY语言中的函数：
 * - int main() { ... }                -> define i32 @main() { ... }
 * - int add(int a, int b) { ... }     -> define i32 @add(i32 %a, i32 %b) { ... }
 * - void print(int x) { ... }         -> define void @print(i32 %x) { ... }
 * 
 * 函数的类型：
 * - 用户定义函数：包含函数体（基本块序列）
 * - 库函数/外部函数：只有声明，没有函数体
 * 
 * 函数调用示例：
 * - %result = call i32 @add(i32 %x, i32 %y)
 * - call void @putint(i32 %value)
 */
public class IRFunction extends IRValue {
    
    /**
     * 函数包含的基本块列表
     * 按执行顺序排列，第一个基本块是入口块
     */
    private final LinkedList<IRBasicBlock> basicBlocks = new LinkedList<>();
    
    /**
     * 函数的形式参数列表
     * 每个参数都是IRFunctionParameter类型
     */
    private final List<IRFunctionParameter> parameters = new ArrayList<>();
    
    /**
     * 构造函数值
     * 
     * @param functionName 函数名（不包含@前缀）
     * @param returnType 返回类型
     * @param parameterTypes 参数类型列表
     */
    public IRFunction(String functionName, IntegerType returnType, ArrayList<IntegerType> parameterTypes) {
        super(null,
                "@" + functionName,  // 函数名以@开头
                new FunctionType(returnType, parameterTypes));
        
        // 创建函数参数对象
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.size(); i++) {
                parameters.add(new IRFunctionParameter(this, i, parameterTypes.get(i)));
            }
        }
    }
    
    /**
     * 无参数函数的构造函数
     * 
     * @param functionName 函数名
     * @param returnType 返回类型
     */
    public IRFunction(String functionName, IntegerType returnType) {
        this(functionName, returnType, new ArrayList<>());
    }
    
    /**
     * 获取函数中的所有指令
     * 
     * 遍历所有基本块，收集其中的所有指令
     * 用于指令级别的分析和优化
     * 
     * @return 所有指令的列表
     */
    public List<IRInstruction> getAllInstructions() {
        List<IRInstruction> instructions = new ArrayList<>();
        for (IRBasicBlock block : basicBlocks) {
            instructions.addAll(block.getAllInstructions());
        }
        return instructions;
    }
    
    /**
     * 获取函数返回类型
     * 
     * @return 返回类型（i32, void等）
     */
    public IRType getReturnType() {
        return ((FunctionType) getType()).getReturnType();
    }
    
    /**
     * 获取函数参数类型列表
     *
     * @return 参数类型列表
     */
    public ArrayList<IntegerType> getParameterTypes() {
        return ((FunctionType) getType()).getParameterTypes();
    }
    
    /**
     * 获取函数形式参数列表
     * 
     * @return 参数对象列表
     */
    public List<IRFunctionParameter> getParameters() {
        return new ArrayList<>(parameters);
    }
    
    /**
     * 获取入口基本块
     * 
     * 入口基本块是函数执行的起始点，
     * 函数调用时控制流从这里开始
     * 
     * @return 第一个基本块，如果函数为空则返回null
     */
    public IRBasicBlock getEntryBlock() {
        return basicBlocks.isEmpty() ? null : basicBlocks.getFirst();
    }
    
    /**
     * 获取所有基本块
     * 
     * @return 基本块列表
     */
    public LinkedList<IRBasicBlock> getBasicBlocks() {
        return basicBlocks;
    }
    
    /**
     * 在函数末尾添加基本块
     * 
     * @param basicBlock 要添加的基本块
     */
    public void addBasicBlock(IRBasicBlock basicBlock) {
        basicBlocks.addLast(basicBlock);
    }
    
    /**
     * 在指定基本块前插入新基本块
     * 
     * 用于控制流变换和优化
     * 
     * @param existingBlock 已存在的基本块
     * @param newBlock 要插入的新基本块
     */
    public void insertBasicBlockBefore(IRBasicBlock existingBlock, IRBasicBlock newBlock) {
        int index = basicBlocks.indexOf(existingBlock);
        if (index != -1) {
            basicBlocks.add(index, newBlock);
        } else {
            basicBlocks.addLast(newBlock);
        }
    }
    
    /**
     * 删除指定索引的基本块
     * 
     * @param index 要删除的基本块索引
     */
    public void removeBasicBlock(int index) {
        if (index >= 0 && index < basicBlocks.size()) {
            basicBlocks.remove(index);
        }
    }
    
    /**
     * 判断是否为库函数
     * 
     * 库函数只有声明没有定义（没有基本块）
     * 如：declare i32 @getint()
     * 
     * @return 如果是库函数返回true
     */
    public boolean isLibraryFunction() {
        return basicBlocks.isEmpty();
    }

    private HashMap<IRValue, Reg> value2reg = new HashMap<>();

    public void setValue2reg(HashMap<IRValue, Reg> value2reg) {
        this.value2reg = value2reg;
    }

    public HashMap<IRValue, Reg> getValue2reg() {
        return value2reg;
    }
    
    /**
     * 生成LLVM IR格式的函数字符串
     * 
     * @return LLVM IR格式的函数定义或声明
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        // 函数声明或定义关键字
        if (isLibraryFunction()) {
            builder.append("declare ");  // 外部函数声明
        } else {
            builder.append("define ");   // 函数定义
        }
        
        // 返回类型和函数名
        builder.append(getReturnType()).append(" ").append(getName()).append("(");
        
        // 参数列表
        for (int i = 0; i < parameters.size(); i++) {
            IRFunctionParameter param = parameters.get(i);
            builder.append(param.getType()).append(" ").append(param.getName());
            if (i < parameters.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(")");
        
        // 如果是库函数，只输出声明
        if (isLibraryFunction()) {
            return builder.toString();
        }
        
        // 函数体
        builder.append(" {\n");
        
        // 输出所有基本块
        for (IRBasicBlock block : basicBlocks) {
            builder.append(block.toString()).append("\n");
        }
        
        builder.append("}");
        return builder.toString();
    }

    /**
     * declare i32 @getint()
     * declare i32 @getchar()
     * declare void @putint(i32)
     * declare void @putch(i32)
     * declare void @putstr(i8*)
     */
    public static final IRFunction GETINT = new IRFunction("getint", IntegerType.I32);
    public static final IRFunction GETCHAR = new IRFunction("getchar", IntegerType.I8);
    public static final IRFunction PUTINT = new IRFunction("putint", new VoidType(), new ArrayList<IntegerType>(){{add(IntegerType.I32);}});
    public static final IRFunction PUTCH = new IRFunction("putch", new VoidType(), new ArrayList<IntegerType>(){{add(IntegerType.I32);}});
    public static final IRFunction PUTSTR = new IRFunction("putstr", new VoidType(), new ArrayList<>(){
        {
            add(new PointerType(IntegerType.I8));
        }
    });
}