package middle.optimize;

import middle.llvm.value.IRFunction;
import middle.llvm.value.IRBasicBlock;
import middle.llvm.value.IRValue;
import middle.llvm.value.constant.IntegerConstant;
import middle.llvm.value.instruction.*;
import middle.llvm.type.IntegerType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * 局部值编号（LVN / GVN 的简化形态）
 *
 * 目标：
 * - 常量折叠（Constant Folding）：在编译期计算纯常量表达式。
 * - 公共子表达式消除（CSE）：在同一支配范围内，用已有等价表达式的结果替换重复计算。
 * - 代数简化（Algebraic Simplification）：利用恒等律与零元/幺元规则简化表达式。
 *
 * 原理与遍历：
 * - 在支配树上 DFS 遍历基本块；块内对可 GVN 的指令建立哈希（语义签名），形成“表达式→值”的映射。
 * - 若同一哈希已出现，则用旧值替换当前指令并删除当前指令；否则将其加入映射，在被支配块可复用。
 * - 在返回父块前清理当前块加入的映射项，保持兄弟块独立。
 *
 * 支持指令类型：
 * - `BinaryOperationInstruction`，`CompareInstruction`，`GetElementPtrInstruction`。
 *
 * 示例（常量折叠与代数简化）：
 * ```llvm
 * ; 优化前
 * %1 = add i32 2, 3      ; → 常量折叠为 5
 * %2 = add i32 %x, 0     ; → 代数简化为 %x
 * %3 = sub i32 %y, %y    ; → 代数简化为 0
 * %4 = mul i32 %z, 1     ; → 代数简化为 %z（若实现 1 规则）
 *
 * ; 优化后
 * ; %1 被删除，其所有使用者替换为 5
 * ; %2 被删除，其所有使用者替换为 %x
 * ; %3 被删除，其所有使用者替换为 0
 * ```
 *
 * 示例（CSE）：
 * ```llvm
 * ; 优化前
 * %a1 = add i32 %p, %q
 * %a2 = add i32 %p, %q   ; 与 %a1 相同表达式
 *
 * ; 优化后
 * %a1 = add i32 %p, %q
 * ; %a2 被删除，其所有使用者替换为 %a1
 * ```
 */
public class LocalValueNumbering extends Optimizer {
    
    /**
     * 全局值编号映射表
     * Key: 表达式的哈希值
     * Value: 对应的指令对象
     */
    private final HashMap<String, IRInstruction> gvnHashMap;

    public LocalValueNumbering() {
        this.gvnHashMap = new HashMap<>();
    }

    @Override
    public void optimize() {
        for (IRFunction function : irModule.getFunctionDefinitions()) {
            // 在每个函数开始前初始化
            this.gvnHashMap.clear();
            // 开始遍历支配树
            this.gvnVisit(function.getBasicBlocks().get(0));
        }
    }

    /**
     * 基于支配树的GVN遍历
     * 
     * @param basicBlock 当前基本块
     */
    private void gvnVisit(IRBasicBlock basicBlock) {
        // 常量折叠
        this.foldValue(basicBlock);

        // 当前block插入map的指令：在支配块中可使用
        HashSet<IRInstruction> gvnAddInstrSet = new HashSet<>();
        this.foldInstruction(basicBlock, gvnAddInstrSet);

        // 对支配块遍历：支配块依然可折叠
        for (IRBasicBlock dominateBlock : basicBlock.getImmediateDominated()) {
            this.gvnVisit(dominateBlock);
        }

        // 恢复对当前的gvn-map，变量兄弟结点
        for (IRInstruction addedInstr : gvnAddInstrSet) {
            this.gvnHashMap.remove(addedInstr.toString());
        }
    }

    /**
     * 判断指令是否可以进行GVN优化
     * 
     * @param instruction 待检查的指令
     * @return 如果可以进行GVN优化返回true
     */
    private boolean canGvnInstruction(IRInstruction instruction) {
        return instruction instanceof BinaryOperationInstruction || 
               instruction instanceof CompareInstruction ||
               instruction instanceof GetElementPtrInstruction;
    }

    /**
     * 进行常量折叠
     * 
     * @param basicBlock 当前基本块
     */
    private void foldValue(IRBasicBlock basicBlock) {
        Iterator<IRInstruction> iterator = basicBlock.getAllInstructions().iterator();
        while (iterator.hasNext()) {
            IRInstruction instruction = iterator.next();
            boolean folded = false;
            
            if (instruction instanceof BinaryOperationInstruction binOp) {
                folded = this.foldBinaryOperation(binOp);
            } else if (instruction instanceof CompareInstruction cmpInst) {
                folded = this.foldCompare(cmpInst);
            }
            
            if (folded) {
                iterator.remove();
            }
        }
    }

    /**
     * 进行表达式替换
     * 
     * @param basicBlock 当前基本块
     * @param addedInstr 新添加的指令集合
     */
    private void foldInstruction(IRBasicBlock basicBlock, HashSet<IRInstruction> addedInstr) {
        Iterator<IRInstruction> iterator = basicBlock.getAllInstructions().iterator();
        while (iterator.hasNext()) {
            IRInstruction instruction = iterator.next();
            if (instruction instanceof CopyInstruction) {
                CopyInstruction c = (CopyInstruction) instruction;
                IRValue src = c.getSourceValue();
                c.getTargetValue().replaceAllUsesWith(src);
                iterator.remove();
                continue;
            }
            if (instruction instanceof TruncateInstruction) {
                TruncateInstruction t = (TruncateInstruction) instruction;
                if (t.getSourceType().toString().equals(t.getTargetType().toString())) {
                    instruction.replaceAllUsesWith(t.getOriginalValue());
                    iterator.remove();
                    continue;
                }
            }
            if (instruction instanceof ZeroExtendInstruction) {
                ZeroExtendInstruction z = (ZeroExtendInstruction) instruction;
                if (z.getSourceType().toString().equals(z.getTargetType().toString())) {
                    instruction.replaceAllUsesWith(z.getOriginalValue());
                    iterator.remove();
                    continue;
                }
            }
            if (this.canGvnInstruction(instruction)) {
                String hash = instruction.toString();
                // 如果存在，则替换值
                if (this.gvnHashMap.containsKey(hash)) {
                    instruction.replaceAllUsesWith(this.gvnHashMap.get(hash));
                    iterator.remove();
                }
                // else，插入map
                else {
                    this.gvnHashMap.put(hash, instruction);
                    addedInstr.add(instruction);
                }
            }
        }
    }

    /**
     * 折叠二元运算指令
     * 
     * @param binOp 二元运算指令
     * @return 如果成功折叠返回true
     */
    private boolean foldBinaryOperation(BinaryOperationInstruction binOp) {
        IRValue leftOperand = binOp.getLeftOperand();
        IRValue rightOperand = binOp.getRightOperand();
        
        // 两个常量
        if (leftOperand instanceof IntegerConstant && rightOperand instanceof IntegerConstant) {
            return this.foldBinaryTwoConstants(leftOperand, rightOperand, binOp);
        }
        // 一个常量或代数简化
        else {
            return this.foldBinaryElseConstant(leftOperand, rightOperand, binOp);
        }
    }

    /**
     * 折叠两个常量的二元运算
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param binOp 二元运算指令
     * @return 如果成功折叠返回true
     */
    private boolean foldBinaryTwoConstants(IRValue leftValue, IRValue rightValue, BinaryOperationInstruction binOp) {
        int leftNum = ((IntegerConstant) leftValue).getConstantValue();
        int rightNum = ((IntegerConstant) rightValue).getConstantValue();
        
        int result;
        if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.ADD) {
            result = leftNum + rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.SUB) {
            result = leftNum - rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.MUL) {
            result = leftNum * rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.SDIV) {
            result = leftNum / rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.SREM) {
            result = leftNum % rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.BITAND) {
            result = leftNum & rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.BITOR) {
            result = leftNum | rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.BITXOR) {
            result = leftNum ^ rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.SHL) {
            result = leftNum << rightNum;
        } else if (binOp.getOperator() == BinaryOperationInstruction.BinaryOperator.ASHR) {
            result = leftNum >> rightNum;
        } else {
            System.err.println("Unknown operator: " + binOp.getOperator());
            return false;
        }

        IntegerConstant resultConstant = new IntegerConstant(IntegerType.I32, result);
        binOp.replaceAllUsesWith(resultConstant);
        return true;
    }

    /**
     * 折叠一个或没有常量的二元运算
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param binOp 二元运算指令
     * @return 如果成功折叠返回true
     */
    private boolean foldBinaryElseConstant(IRValue leftValue, IRValue rightValue, BinaryOperationInstruction binOp) {
        return switch (binOp.getOperator()) {
            case ADD -> this.foldElseAdd(leftValue, rightValue, binOp);
            case SUB -> this.foldElseSub(leftValue, rightValue, binOp);
            case MUL -> this.foldElseMul(leftValue, rightValue, binOp);
            case SDIV -> this.foldElseDiv(leftValue, rightValue, binOp);
            case SREM -> this.foldElseRem(leftValue, rightValue, binOp);
            default -> false;
        };
    }

    /**
     * 折叠加法运算的特殊情况
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param binOp 二元运算指令
     * @return 如果成功折叠返回true
     */
    private boolean foldElseAdd(IRValue leftValue, IRValue rightValue, BinaryOperationInstruction binOp) {
        // x + 0 = x
        if (leftValue instanceof IntegerConstant && ((IntegerConstant) leftValue).getConstantValue() == 0) {
            binOp.replaceAllUsesWith(rightValue);
            return true;
        } else if (rightValue instanceof IntegerConstant && ((IntegerConstant) rightValue).getConstantValue() == 0) {
            binOp.replaceAllUsesWith(leftValue);
            return true;
        }
        return false;
    }

    /**
     * 折叠减法运算的特殊情况
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param binOp 二元运算指令
     * @return 如果成功折叠返回true
     */
    private boolean foldElseSub(IRValue leftValue, IRValue rightValue, BinaryOperationInstruction binOp) {
        // x - 0 = x
        if (rightValue instanceof IntegerConstant && ((IntegerConstant) rightValue).getConstantValue() == 0) {
            binOp.replaceAllUsesWith(leftValue);
            return true;
        } 
        // x - x = 0
        else if (rightValue == leftValue) {
            binOp.replaceAllUsesWith(new IntegerConstant(IntegerType.I32, 0));
            return true;
        }
        return false;
    }

    /**
     * 折叠乘法运算的特殊情况
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param binOp 二元运算指令
     * @return 如果成功折叠返回true
     */
    private boolean foldElseMul(IRValue leftValue, IRValue rightValue, BinaryOperationInstruction binOp) {
        if (leftValue instanceof IntegerConstant) {
            int constant = ((IntegerConstant) leftValue).getConstantValue();
            if (constant == 0) {
                // 0 * x = 0
                binOp.replaceAllUsesWith(new IntegerConstant(IntegerType.I32, 0));
                return true;
            } else if (constant == 1) {
                // 1 * x = x
                binOp.replaceAllUsesWith(rightValue);
                return true;
            }
        } else if (rightValue instanceof IntegerConstant) {
            int constant = ((IntegerConstant) rightValue).getConstantValue();
            if (constant == 0) {
                // x * 0 = 0
                binOp.replaceAllUsesWith(new IntegerConstant(IntegerType.I32, 0));
                return true;
            } else if (constant == 1) {
                // x * 1 = x
                binOp.replaceAllUsesWith(leftValue);
                return true;
            }
        }
        return false;
    }

    /**
     * 折叠除法运算的特殊情况
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param binOp 二元运算指令
     * @return 如果成功折叠返回true
     */
    private boolean foldElseDiv(IRValue leftValue, IRValue rightValue, BinaryOperationInstruction binOp) {
        // x / 1 = x
        if (rightValue instanceof IntegerConstant) {
            if (((IntegerConstant) rightValue).getConstantValue() == 1) {
                binOp.replaceAllUsesWith(leftValue);
                return true;
            }
        }
        // x / x = 1 (假设x != 0)
        if (rightValue == leftValue) {
            binOp.replaceAllUsesWith(new IntegerConstant(IntegerType.I32, 1));
            return true;
        }
        return false;
    }

    /**
     * 折叠取模运算的特殊情况
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param binOp 二元运算指令
     * @return 如果成功折叠返回true
     */
    private boolean foldElseRem(IRValue leftValue, IRValue rightValue, BinaryOperationInstruction binOp) {
        // x % 1 = 0
        if (rightValue instanceof IntegerConstant) {
            if (((IntegerConstant) rightValue).getConstantValue() == 1) {
                binOp.replaceAllUsesWith(new IntegerConstant(IntegerType.I32, 0));
                return true;
            }
        }
        // x % x = 0 (假设x != 0)
        if (rightValue == leftValue) {
            binOp.replaceAllUsesWith(new IntegerConstant(IntegerType.I32, 0));
            return true;
        }
        return false;
    }

    /**
     * 折叠比较指令
     * 
     * @param cmpInst 比较指令
     * @return 如果成功折叠返回true
     */
    private boolean foldCompare(CompareInstruction cmpInst) {
        IRValue leftOperand = cmpInst.getLeftOperand();
        IRValue rightOperand = cmpInst.getRightOperand();
        
        // 两个常量
        if (leftOperand instanceof IntegerConstant && rightOperand instanceof IntegerConstant) {
            return this.foldCompareTwoConstants(leftOperand, rightOperand, cmpInst);
        }
        // 一个常量或特殊情况
        else {
            return this.foldCompareElseConstant(leftOperand, rightOperand, cmpInst);
        }
    }

    /**
     * 折叠两个常量的比较
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param cmpInst 比较指令
     * @return 如果成功折叠返回true
     */
    private boolean foldCompareTwoConstants(IRValue leftValue, IRValue rightValue, CompareInstruction cmpInst) {
        int leftNum = ((IntegerConstant) leftValue).getConstantValue();
        int rightNum = ((IntegerConstant) rightValue).getConstantValue();
        
        boolean result = switch (cmpInst.getCondition()) {
            case EQ -> leftNum == rightNum;
            case NE -> leftNum != rightNum;
            case SGT -> leftNum > rightNum;
            case SGE -> leftNum >= rightNum;
            case SLT -> leftNum < rightNum;
            case SLE -> leftNum <= rightNum;
        };

        IntegerConstant resultConstant = new IntegerConstant(IntegerType.I1, result ? 1 : 0);
        cmpInst.replaceAllUsesWith(resultConstant);
        return true;
    }

    /**
     * 折叠一个或没有常量的比较
     * 
     * @param leftValue 左操作数
     * @param rightValue 右操作数
     * @param cmpInst 比较指令
     * @return 如果成功折叠返回true
     */
    private boolean foldCompareElseConstant(IRValue leftValue, IRValue rightValue, CompareInstruction cmpInst) {
        return switch (cmpInst.getCondition()) {
            case EQ -> this.foldElseEq(leftValue, rightValue, cmpInst);
            case NE -> this.foldElseNe(leftValue, rightValue, cmpInst);
            case SGT -> this.foldElseSgt(leftValue, rightValue, cmpInst);
            case SGE -> this.foldElseSge(leftValue, rightValue, cmpInst);
            case SLT -> this.foldElseSlt(leftValue, rightValue, cmpInst);
            case SLE -> this.foldElseSle(leftValue, rightValue, cmpInst);
        };
    }

    /**
     * 折叠等于比较的特殊情况
     */
    private boolean foldElseEq(IRValue leftValue, IRValue rightValue, CompareInstruction cmpInst) {
        // x == x = true
        if (leftValue == rightValue) {
            cmpInst.replaceAllUsesWith(new IntegerConstant(IntegerType.I1, 1));
            return true;
        }
        return false;
    }

    /**
     * 折叠不等于比较的特殊情况
     */
    private boolean foldElseNe(IRValue leftValue, IRValue rightValue, CompareInstruction cmpInst) {
        // x != x = false
        if (leftValue == rightValue) {
            cmpInst.replaceAllUsesWith(new IntegerConstant(IntegerType.I1, 0));
            return true;
        }
        return false;
    }

    /**
     * 折叠大于比较的特殊情况
     */
    private boolean foldElseSgt(IRValue leftValue, IRValue rightValue, CompareInstruction cmpInst) {
        // x > x = false
        if (leftValue == rightValue) {
            cmpInst.replaceAllUsesWith(new IntegerConstant(IntegerType.I1, 0));
            return true;
        }
        return false;
    }

    /**
     * 折叠大于等于比较的特殊情况
     */
    private boolean foldElseSge(IRValue leftValue, IRValue rightValue, CompareInstruction cmpInst) {
        // x >= x = true
        if (leftValue == rightValue) {
            cmpInst.replaceAllUsesWith(new IntegerConstant(IntegerType.I1, 1));
            return true;
        }
        return false;
    }

    /**
     * 折叠小于比较的特殊情况
     */
    private boolean foldElseSlt(IRValue leftValue, IRValue rightValue, CompareInstruction cmpInst) {
        // x < x = false
        if (leftValue == rightValue) {
            cmpInst.replaceAllUsesWith(new IntegerConstant(IntegerType.I1, 0));
            return true;
        }
        return false;
    }

    /**
     * 折叠小于等于比较的特殊情况
     */
    private boolean foldElseSle(IRValue leftValue, IRValue rightValue, CompareInstruction cmpInst) {
        // x <= x = true
        if (leftValue == rightValue) {
            cmpInst.replaceAllUsesWith(new IntegerConstant(IntegerType.I1, 1));
            return true;
        }
        return false;
    }

    @Override
    public String OptimizerName() {
        return "LocalValueNumbering";
    }
}
