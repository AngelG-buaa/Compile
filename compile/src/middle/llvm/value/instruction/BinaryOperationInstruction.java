package middle.llvm.value.instruction;

import middle.llvm.type.IntegerType;
import middle.llvm.value.IRValue;

import java.util.ArrayList;
import java.util.List;

/**
 * LLVM IR二元运算指令
 * 
 * 表示LLVM IR中的二元算术运算指令，用于执行两个操作数之间的数学运算。
 * 
 * 特性：
 * - 接受两个操作数：左操作数和右操作数，类型必须相同
 * - 产生一个结果值：运算结果，类型与操作数类型相同
 * - 支持多种运算：加法、减法、乘法、除法、取模等
 * - 类型安全：操作数类型必须匹配，通常为整数类型
 * - 无副作用：纯计算指令，不修改内存或全局状态
 * 
 * LLVM IR语法示例：
 * - %result = add i32 %a, %b        ; 整数加法
 * - %result = sub i32 %x, 5         ; 整数减法
 * - %result = mul i32 %i, %j        ; 整数乘法
 * - %result = sdiv i32 %m, %n       ; 有符号整数除法
 * - %result = srem i32 %p, %q       ; 有符号整数取模
 * 
 * 对应SysY语言场景：
 * - 算术表达式：a + b, x - y, i * j
 * - 复合表达式：(a + b) * (c - d)
 * - 数组索引计算：arr[i * 2 + 1]
 * - 循环计数器更新：i = i + 1
 * - 条件表达式的数值计算部分
 * 
 * 运算类型：
 * - ADD：整数加法运算
 * - SUB：整数减法运算  
 * - MUL：整数乘法运算
 * - SDIV：有符号整数除法运算
 * - SREM：有符号整数取模运算
 * 
 * 与其他指令的配合：
 * - 与load指令配合：先加载操作数，再进行运算
 * - 与store指令配合：将运算结果存储到内存
 * - 与比较指令配合：运算结果用于条件判断
 * - 与分支指令配合：根据运算结果进行控制流转移
 * 
 * 编译器优化相关：
 * - 常量折叠：编译时计算常量表达式
 * - 强度削减：将乘法转换为移位操作
 * - 代数简化：利用数学恒等式简化表达式
 * - 公共子表达式消除：避免重复计算相同表达式
 */
public class BinaryOperationInstruction extends IRInstruction {
    
    /**
     * 二元运算符枚举
     * 定义支持的所有二元算术运算类型
     */
    public enum BinaryOperator {
        ADD("add"),      // 加法运算
        SUB("sub"),      // 减法运算
        MUL("mul"),      // 乘法运算
        SDIV("sdiv"),    // 有符号除法运算
        SREM("srem");    // 有符号取模运算
        
        private final String opName;
        
        BinaryOperator(String opName) {
            this.opName = opName;
        }
        
        public String getOpName() {
            return opName;
        }
    }
    
    /**
     * 运算符类型
     * 存储此二元运算指令的具体运算类型
     */
    private final BinaryOperator operator;
    
    /**
     * 构造二元运算指令
     * 
     * @param parentBlock 所属基本块
     * @param operator 运算符类型
     * @param nameCounter 名称计数器，用于生成唯一的结果变量名
     * @param leftOperand 左操作数
     * @param rightOperand 右操作数
     */
    public BinaryOperationInstruction(IRValue parentBlock, BinaryOperator operator, int nameCounter, 
                                    IRValue leftOperand, IRValue rightOperand) {
        super(parentBlock, "%calc" + nameCounter, IntegerType.I32); // 运算结果一定为i32
        this.operator = operator;
        
        List<IRValue> operands = new ArrayList<>();
        operands.add(leftOperand);
        operands.add(rightOperand);
        setOperands(operands);
    }
    
    /**
     * 获取运算符类型
     * 
     * @return 此指令使用的二元运算符
     */
    public BinaryOperator getOperator() {
        return operator;
    }
    
    /**
     * 获取左操作数
     * 
     * @return 运算的左操作数
     */
    public IRValue getLeftOperand() {
        return getOperand(0);
    }
    
    /**
     * 获取右操作数
     * 
     * @return 运算的右操作数
     */
    public IRValue getRightOperand() {
        return getOperand(1);
    }
    
    /**
     * 获取指令操作码名称
     * 
     * @return 运算符对应的LLVM IR操作码字符串
     */
    @Override
    public String getOpcodeName() {
        return operator.getOpName();
    }
    
    /**
     * 生成LLVM IR字符串表示
     * 
     * @return 完整的LLVM IR二元运算指令字符串
     */
    @Override
    public String toString() {
        return getName() + " = " + operator.getOpName() + " i32 " +
               getLeftOperand().getName() + ", " + getRightOperand().getName();
    }
}