package middle.llvm.value.instruction;

import middle.llvm.type.IRType;
import middle.llvm.value.IRUser;
import middle.llvm.value.IRValue;

import java.util.List;

/**
 * LLVM IR指令基类实现
 * 
 * <p>表示LLVM IR中所有指令的抽象基类，继承自IRUser，具有使用其他值作为操作数的能力。
 * IRInstruction类在LLVM IR中具有以下特征：
 * <ul>
 *   <li>位于基本块（BasicBlock）内部，按顺序执行</li>
 *   <li>可能产生结果值，也可能不产生（void类型）</li>
 *   <li>使用其他值作为操作数，维护Use-Def关系</li>
 *   <li>具有特定的操作码（opcode）标识指令类型</li>
 *   <li>可能具有副作用（如内存访问、函数调用）</li>
 * </ul>
 * 
 * <p>LLVM IR指令分类：
 * <ul>
 *   <li><strong>算术指令</strong>：add, sub, mul, div, rem等</li>
 *   <li><strong>比较指令</strong>：icmp, fcmp等</li>
 *   <li><strong>内存指令</strong>：alloca, load, store, getelementptr等</li>
 *   <li><strong>控制流指令</strong>：br, ret, call等</li>
 *   <li><strong>类型转换指令</strong>：trunc, zext, sext, bitcast等</li>
 *   <li><strong>其他指令</strong>：phi, select等</li>
 * </ul>
 * 
 * <p>LLVM IR指令示例：
 * <pre>
 * ; 算术指令
 * %3 = add i32 %1, %2
 * 
 * ; 内存分配指令
 * %ptr = alloca i32
 * 
 * ; 内存访问指令
 * %4 = load i32, i32* %ptr
 * store i32 %3, i32* %ptr
 * 
 * ; 控制流指令
 * br i1 %cond, label %true, label %false
 * ret i32 %4
 * 
 * ; 函数调用指令
 * %5 = call i32 @func(i32 %3)
 * </pre>
 * 
 * <p>对应SysY语言场景：
 * <ul>
 *   <li>表达式计算：{@code a + b} 对应算术指令</li>
 *   <li>变量声明：{@code int x;} 对应alloca指令</li>
 *   <li>赋值操作：{@code x = y;} 对应load+store指令</li>
 *   <li>条件分支：{@code if (cond)} 对应icmp+br指令</li>
 *   <li>函数调用：{@code func(x)} 对应call指令</li>
 *   <li>函数返回：{@code return x;} 对应ret指令</li>
 * </ul>
 * 
 * <p>指令特性：
 * <ul>
 *   <li><strong>终结指令</strong>：结束基本块的指令（br, ret, unreachable）</li>
 *   <li><strong>副作用指令</strong>：可能影响程序状态的指令（store, call, volatile load）</li>
 *   <li><strong>纯指令</strong>：无副作用的指令，可以被优化器重排或消除</li>
 * </ul>
 * 
 * <p>编译器优化相关：
 * <ul>
 *   <li>指令调度：根据数据依赖关系重排指令顺序</li>
 *   <li>死代码消除：删除无用的指令</li>
 *   <li>常量传播：将常量操作数传播到使用处</li>
 *   <li>公共子表达式消除：合并相同的计算</li>
 * </ul>
 * 
 * @see IRUser 用户值基类
 * @see IRValue 值基类
 * @see IRType 类型系统
 */
public abstract class IRInstruction extends IRUser {
    
    /**
     * 构造指令（带操作数列表）
     * 
     * @param parentBlock 所属基本块
     * @param instructionName 指令名称（结果值名称）
     * @param resultType 结果类型
     * @param operands 操作数列表
     */
    protected IRInstruction(IRValue parentBlock, String instructionName, IRType resultType, List<IRValue> operands) {
        super(parentBlock, instructionName, resultType, operands);
    }
    
    /**
     * 构造指令（无初始操作数）
     * 
     * @param parentBlock 所属基本块
     * @param instructionName 指令名称（结果值名称）
     * @param resultType 结果类型
     */
    protected IRInstruction(IRValue parentBlock, String instructionName, IRType resultType) {
        super(parentBlock, instructionName, resultType);
    }
    
    /**
     * 构造指令（自动生成名称）
     * 
     * @param parentBlock 所属基本块
     * @param resultType 结果类型
     */
    protected IRInstruction(IRValue parentBlock, IRType resultType) {
        super(parentBlock, resultType);
    }
    
//    /**
//     * 获取指令所属的基本块
//     */
//    public IRValue getParentBlock() {
//        return getParent();
//    }
    
    /**
     * 判断指令是否为终结指令
     * 
     * <p>终结指令是结束基本块执行的指令，包括：
     * <ul>
     *   <li>{@code ret}：函数返回指令</li>
     *   <li>{@code br}：分支指令（条件分支和无条件分支）</li>
     *   <li>{@code unreachable}：不可达指令</li>
     * </ul>
     * 
     * <p>每个基本块必须且只能有一个终结指令，且必须是最后一条指令。
     * 
     * @return 如果是终结指令返回true，否则返回false
     */
    public boolean isTerminatorInstruction() {
        return false;
    }
    
    /**
     * 判断指令是否有副作用
     * 
     * <p>有副作用的指令是指可能影响程序状态或外部环境的指令，包括：
     * <ul>
     *   <li>内存写入：{@code store}指令</li>
     *   <li>函数调用：{@code call}指令（可能修改全局状态）</li>
     *   <li>易失性内存访问：{@code volatile load/store}</li>
     *   <li>原子操作：{@code atomicrmw, cmpxchg}等</li>
     * </ul>
     * 
     * <p>有副作用的指令不能被随意删除或重排，即使其结果未被使用。
     * 
     * @return 如果有副作用返回true，否则返回false
     */
    public boolean hasSideEffects() {
        return false;
    }
    
    /**
     * 获取指令的操作码名称
     * 
     * <p>操作码是LLVM IR中标识指令类型的字符串，如：
     * <ul>
     *   <li>"add" - 加法指令</li>
     *   <li>"load" - 内存加载指令</li>
     *   <li>"store" - 内存存储指令</li>
     *   <li>"br" - 分支指令</li>
     *   <li>"call" - 函数调用指令</li>
     * </ul>
     * 
     * @return 指令的操作码名称
     */
    public abstract String getOpcodeName();
}