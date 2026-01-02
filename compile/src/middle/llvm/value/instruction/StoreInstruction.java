package middle.llvm.value.instruction;

import middle.llvm.type.VoidType;
import middle.llvm.value.IRValue;

/**
 * LLVM IR内存存储指令实现
 * 
 * <p>表示LLVM IR中的store指令，用于将值存储到指针指向的内存位置。
 * StoreInstruction在LLVM IR中具有以下特征：
 * <ul>
 *   <li>将值写入指针指向的内存位置</li>
 *   <li>不产生结果值（void类型）</li>
 *   <li>具有副作用，会修改程序状态</li>
 *   <li>是内存访问的基本操作之一</li>
 *   <li>与load指令配对使用实现内存读写</li>
 * </ul>
 * 
 * <p>LLVM IR中的store指令语法：
 * <pre>
 * ; 基本语法
 * store &lt;type&gt; &lt;value&gt;, &lt;type&gt;* &lt;pointer&gt;
 * 
 * ; 存储整数
 * store i32 42, i32* %ptr
 * 
 * ; 存储变量值
 * store i32 %value, i32* %ptr
 * 
 * ; 存储到数组元素
 * store i32 %value, i32* %arrayElementPtr
 * </pre>
 * 
 * <p>对应SysY语言场景：
 * <ul>
 *   <li>变量赋值：{@code x = 42;} 对应 {@code store i32 42, i32* %x}</li>
 *   <li>数组元素赋值：{@code arr[i] = value;} 对应 GEP + store 指令序列</li>
 *   <li>函数参数传递：将实参值存储到形参的本地副本</li>
 *   <li>表达式结果保存：将计算结果存储到临时变量</li>
 * </ul>
 * 
 * <p>内存访问特性：
 * <ul>
 *   <li><strong>类型安全</strong>：存储的值类型必须与指针指向的类型匹配</li>
 *   <li><strong>原子性</strong>：单个store操作是原子的</li>
 *   <li><strong>对齐要求</strong>：访问地址应满足类型的对齐要求</li>
 *   <li><strong>有副作用</strong>：修改内存状态，不能被随意删除或重排</li>
 * </ul>
 * 
 * <p>与其他指令的配合使用：
 * <ul>
 *   <li>{@code alloca}指令：分配要存储的目标内存</li>
 *   <li>{@code load}指令：从内存读取要存储的值</li>
 *   <li>{@code getelementptr}指令：计算要存储的目标地址</li>
 *   <li>算术指令：计算要存储的值</li>
 * </ul>
 * 
 * <p>编译器优化相关：
 * <ul>
 *   <li>存储消除：消除被覆盖的store操作</li>
 *   <li>存储转发：将store-load序列优化为直接值传递</li>
 *   <li>内存到寄存器：将简单的内存访问转换为寄存器操作</li>
 *   <li>死存储消除：删除永远不会被读取的store操作</li>
 * </ul>
 * 
 * @see IRInstruction 指令基类
 * @see LoadInstruction 内存加载指令
 * @see AllocaInstruction 内存分配指令
 * @see GetElementPtrInstruction 地址计算指令
 */
public class StoreInstruction extends IRInstruction {
    
    /**
     * 创建store指令
     * 
     * <p>将指定的值存储到指定的指针指向的内存位置。
     * store指令不产生结果值，因此类型为void。
     * 
     * @param parentBlock 所属基本块
     * @param valueOperand 要存储的值操作数
     * @param pointerOperand 目标指针操作数，指向要存储的内存位置
     */
    public StoreInstruction(IRValue parentBlock, IRValue valueOperand, IRValue pointerOperand) {
        super(parentBlock, new VoidType());
        addOperand(valueOperand);
        addOperand(pointerOperand);
    }
    
    /**
     * 获取要存储的值操作数
     * 
     * <p>返回store指令的第一个操作数，即要写入内存的值。
     * 
     * @return 要存储的值操作数
     */
    public IRValue getValueOperand() {
        return getOperand(0);
    }
    
    /**
     * 获取目标指针操作数
     * 
     * <p>返回store指令的第二个操作数，即要写入的目标内存地址。
     * 
     * @return 目标指针操作数
     */
    public IRValue getPointerOperand() {
        return getOperand(1);
    }
    
    /**
     * 判断指令是否有副作用
     * 
     * <p>store指令总是有副作用，因为它会修改内存状态。
     * 有副作用的指令不能被随意删除或重排，即使其结果未被使用。
     * 
     * @return 总是返回true，表示有副作用
     */
    @Override
    public boolean hasSideEffects() {
        return true; // store指令有副作用，会修改内存
    }
    
    @Override
    public String getOpcodeName() {
        return "store";
    }
    
    /**
     * 生成LLVM IR字符串表示
     * 
     * <p>格式：{@code store <value_type> <value_name>, <pointer_type> <pointer_name>}
     * 
     * @return LLVM IR字符串表示
     */
    @Override
    public String toString() {
        IRValue value = getValueOperand();
        IRValue pointer = getPointerOperand();
        return "store " + value.getType() + " " + value.getName() + 
               ", " + pointer.getType() + " " + pointer.getName();
    }
}