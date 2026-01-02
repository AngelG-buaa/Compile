package middle.llvm.value.instruction;

import middle.llvm.type.IRType;
import middle.llvm.type.PointerType;
import middle.llvm.value.IRValue;
import middle.llvm.value.constant.IRConstant;

/**
 * LLVM IR内存分配指令实现
 * 
 * <p>表示LLVM IR中的alloca指令，用于在函数的栈帧中分配内存空间。
 * AllocaInstruction在LLVM IR中具有以下特征：
 * <ul>
 *   <li>在栈上分配指定类型的内存空间</li>
 *   <li>返回指向分配内存的指针</li>
 *   <li>内存在函数返回时自动释放</li>
 *   <li>支持可选的初始值设置</li>
 *   <li>分配的内存大小在编译时确定</li>
 * </ul>
 * 
 * <p>LLVM IR中的alloca指令语法：
 * <pre>
 * ; 基本语法
 * %ptr = alloca &lt;type&gt;
 * 
 * ; 分配单个整数
 * %1 = alloca i32
 * 
 * ; 分配数组
 * %2 = alloca [10 x i32]
 * 
 * ; 分配结构体
 * %3 = alloca %struct.Point
 * </pre>
 * 
 * <p>对应SysY语言场景：
 * <ul>
 *   <li>局部变量声明：{@code int x;} 对应 {@code %x = alloca i32}</li>
 *   <li>数组声明：{@code int arr[10];} 对应 {@code %arr = alloca [10 x i32]}</li>
 *   <li>临时变量：表达式计算中的中间结果存储</li>
 *   <li>函数参数：值传递参数的本地副本</li>
 * </ul>
 * 
 * <p>内存管理特性：
 * <ul>
 *   <li><strong>栈分配</strong>：内存在栈上分配，访问速度快</li>
 *   <li><strong>自动释放</strong>：函数返回时自动释放，无需手动管理</li>
 *   <li><strong>局部作用域</strong>：只在当前函数内有效</li>
 *   <li><strong>固定大小</strong>：分配大小在编译时确定</li>
 * </ul>
 * 
 * <p>与其他指令的配合使用：
 * <ul>
 *   <li>{@code store}指令：向分配的内存写入值</li>
 *   <li>{@code load}指令：从分配的内存读取值</li>
 *   <li>{@code getelementptr}指令：计算数组元素地址</li>
 * </ul>
 * 
 * <p>编译器优化相关：
 * <ul>
 *   <li>寄存器提升：将频繁访问的alloca提升为SSA值</li>
 *   <li>栈合并：合并生命周期不重叠的alloca</li>
 *   <li>死代码消除：删除未使用的alloca</li>
 * </ul>
 * 
 * @see IRInstruction 指令基类
 * @see PointerType 指针类型
 * @see LoadInstruction 内存加载指令
 * @see StoreInstruction 内存存储指令
 */
public class AllocaInstruction extends IRInstruction {
    /**
     * 初始值常量
     * 可能为null表示未初始化，用于支持带初始值的变量声明
     */
    private final IRConstant initialValue; // 初始值，可能为null表示未初始化
    
    /**
     * 创建未初始化的alloca指令
     * 
     * <p>用于声明未初始化的局部变量，如：
     * <pre>
     * int x;  // 对应 %x = alloca i32
     * </pre>
     * 
     * @param parentBlock 所属基本块
     * @param nameCounter 变量名计数器，用于生成唯一的变量名
     * @param allocatedType 要分配的数据类型
     */
    public AllocaInstruction(IRValue parentBlock, int nameCounter, IRType allocatedType) {
        super(parentBlock, "%var" + nameCounter, new PointerType(allocatedType));
        this.initialValue = null;
    }
    
    /**
     * 创建带初始值的alloca指令
     * 
     * <p>用于声明带初始值的局部变量，如：
     * <pre>
     * int x = 42;  // 对应 %x = alloca i32 (然后store初始值)
     * </pre>
     * 
     * @param parentBlock 所属基本块
     * @param nameCounter 变量名计数器，用于生成唯一的变量名
     * @param allocatedType 要分配的数据类型
     * @param initialValue 初始值常量
     */
    public AllocaInstruction(IRValue parentBlock, int nameCounter, IRType allocatedType, IRConstant initialValue) {
        super(parentBlock, "%var" + nameCounter, new PointerType(allocatedType));
        this.initialValue = initialValue;
    }
    
    /**
     * 获取分配的数据类型
     * 
     * <p>返回alloca指令分配的实际数据类型，而不是指针类型。
     * 例如，对于 {@code %x = alloca i32}，返回 {@code i32} 而不是 {@code i32*}。
     * 
     * @return 分配的数据类型
     */
    public IRType getAllocatedType() {
        return ((PointerType) getType()).getPointeeType();
    }
    
    /**
     * 获取初始值常量
     * 
     * @return 初始值常量，如果未初始化则返回null
     */
    public IRConstant getInitialValue() {
        return initialValue;
    }
    
    /**
     * 判断是否有初始值
     * 
     * @return 如果有初始值返回true，否则返回false
     */
    public boolean hasInitialValue() {
        return initialValue != null;
    }
    
    @Override
    public String getOpcodeName() {
        return "alloca";
    }
    
    /**
     * 生成LLVM IR字符串表示
     * 
     * <p>格式：{@code %varN = alloca <type>}
     * 
     * @return LLVM IR字符串表示
     */
    @Override
    public String toString() {
        return getName() + " = alloca " + getAllocatedType().toString();
    }
}