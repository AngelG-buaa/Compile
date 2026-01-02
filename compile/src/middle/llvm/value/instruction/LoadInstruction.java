package middle.llvm.value.instruction;

import middle.llvm.type.PointerType;
import middle.llvm.value.IRValue;

/**
 * LLVM IR内存加载指令实现
 * 
 * <p>表示LLVM IR中的load指令，用于从指针指向的内存位置读取值。
 * LoadInstruction在LLVM IR中具有以下特征：
 * <ul>
 *   <li>从指针指向的内存位置加载数据</li>
 *   <li>产生加载的值作为结果</li>
 *   <li>支持各种数据类型的加载</li>
 *   <li>是内存访问的基本操作之一</li>
 *   <li>与store指令配对使用实现内存读写</li>
 * </ul>
 * 
 * <p>LLVM IR中的load指令语法：
 * <pre>
 * ; 基本语法
 * %result = load &lt;type&gt;, &lt;type&gt;* &lt;pointer&gt;
 * 
 * ; 加载整数
 * %1 = load i32, i32* %ptr
 * 
 * ; 加载数组元素
 * %2 = load i32, i32* %arrayPtr
 * 
 * ; 加载结构体字段
 * %3 = load %struct.Point, %struct.Point* %structPtr
 * </pre>
 * 
 * <p>对应SysY语言场景：
 * <ul>
 *   <li>变量读取：{@code x} 对应 {@code %1 = load i32, i32* %x}</li>
 *   <li>数组元素访问：{@code arr[i]} 对应 GEP + load 指令序列</li>
 *   <li>函数参数使用：读取参数的本地副本</li>
 *   <li>表达式计算：读取变量值参与运算</li>
 * </ul>
 * 
 * <p>内存访问特性：
 * <ul>
 *   <li><strong>类型安全</strong>：加载的类型必须与指针指向的类型匹配</li>
 *   <li><strong>原子性</strong>：单个load操作是原子的</li>
 *   <li><strong>对齐要求</strong>：访问地址应满足类型的对齐要求</li>
 *   <li><strong>无副作用</strong>：纯读操作，不修改程序状态</li>
 * </ul>
 * 
 * <p>与其他指令的配合使用：
 * <ul>
 *   <li>{@code alloca}指令：分配要加载的内存</li>
 *   <li>{@code store}指令：向内存写入要加载的值</li>
 *   <li>{@code getelementptr}指令：计算要加载的地址</li>
 *   <li>算术指令：使用加载的值进行计算</li>
 * </ul>
 * 
 * <p>编译器优化相关：
 * <ul>
 *   <li>加载消除：消除冗余的load操作</li>
 *   <li>寄存器提升：将频繁加载的值提升为SSA值</li>
 *   <li>内存到寄存器：将简单的内存访问转换为寄存器操作</li>
 *   <li>公共子表达式消除：合并相同地址的load操作</li>
 * </ul>
 * 
 * @see IRInstruction 指令基类
 * @see StoreInstruction 内存存储指令
 * @see AllocaInstruction 内存分配指令
 * @see GetElementPtrInstruction 地址计算指令
 */
public class LoadInstruction extends IRInstruction {
    
    /**
     * 创建load指令
     * 
     * <p>从指定的指针操作数加载值，结果类型为指针指向的类型。
     * 
     * @param parentBlock 所属基本块
     * @param nameCounter 结果值名称计数器，用于生成唯一的结果名
     * @param pointerOperand 指针操作数，指向要加载的内存位置
     */
    public LoadInstruction(IRValue parentBlock, int nameCounter, IRValue pointerOperand) {
        super(parentBlock, "%load" + nameCounter, ((PointerType) pointerOperand.getType()).getPointeeType());
        addOperand(pointerOperand);
    }
    
    /**
     * 获取被加载的指针操作数
     * 
     * <p>返回load指令的指针操作数，即要从中加载值的内存地址。
     * 
     * @return 指针操作数
     */
    public IRValue getPointerOperand() {
        return getOperand(0);
    }
    
    /**
     * 获取加载的数据类型
     * 
     * <p>返回从内存中加载的值的类型，也是load指令的结果类型。
     * 
     * @return 加载的数据类型
     */
    public middle.llvm.type.IRType getLoadedType() {
        return getType();
    }
    
    @Override
    public String getOpcodeName() {
        return "load";
    }
    
    /**
     * 生成LLVM IR字符串表示
     * 
     * <p>格式：{@code %loadN = load <type>, <pointer_type> <pointer_name>}
     * 
     * @return LLVM IR字符串表示
     */
    @Override
    public String toString() {
        IRValue pointer = getPointerOperand();
        return getName() + " = load " + getLoadedType() + ", " + 
               pointer.getType() + " " + pointer.getName();
    }
}