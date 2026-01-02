package middle.llvm.value.instruction;

import middle.llvm.type.IntegerType;
import middle.llvm.value.IRValue;

import java.util.ArrayList;
import java.util.List;

/**
 * LLVM IR整数比较指令
 * icmp <cond> <ty> <op1>, <op2>
 */
public class CompareInstruction extends IRInstruction {
    
    public enum CompareCondition {
        EQ("eq"),   // ==
        NE("ne"),   // !=
        SLT("slt"), // <  (signed less than)
        SLE("sle"), // <= (signed less than or equal)
        SGT("sgt"), // >  (signed greater than)
        SGE("sge"); // >= (signed greater than or equal)
        
        private final String conditionName;
        
        CompareCondition(String conditionName) {
            this.conditionName = conditionName;
        }
        
        public String getConditionName() {
            return conditionName;
        }
    }
    
    private final CompareCondition condition;

    /**
     * 构造整数比较指令
     *
     * @param parentBlock 所属基本块
     * @param condition 运算符类型
     * @param nameCounter 名称计数器，用于生成唯一的结果变量名
     * @param leftOperand 左操作数
     * @param rightOperand 右操作数
     */
    public CompareInstruction(IRValue parentBlock, CompareCondition condition, int nameCounter, 
                            IRValue leftOperand, IRValue rightOperand) {
        super(parentBlock, "%cmp" + nameCounter, IntegerType.I1); // 比较结果为i1类型
        this.condition = condition;
        
        List<IRValue> operands = new ArrayList<>();
        operands.add(leftOperand);
        operands.add(rightOperand);
        setOperands(operands);
    }
    
    /**
     * 获取比较条件
     */
    public CompareCondition getCondition() {
        return condition;
    }
    
    /**
     * 获取左操作数
     */
    public IRValue getLeftOperand() {
        return getOperand(0);
    }
    
    /**
     * 获取右操作数
     */
    public IRValue getRightOperand() {
        return getOperand(1);
    }
    
    @Override
    public String getOpcodeName() {
        return "icmp";
    }
    
    @Override
    public String toString() {
        IRValue left = getLeftOperand();
        IRValue right = getRightOperand();
        return getName() + " = icmp " + condition.getConditionName() + " " + 
               left.getType() + " " + left.getName() + ", " + right.getName();
    }
}