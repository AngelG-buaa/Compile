package utils;

import front.parser.syntax.AstNode;
import front.parser.syntax.exp.AddExp;
import front.parser.syntax.exp.Exp;
import front.parser.syntax.exp.FuncCallUnaryExp;
import front.parser.syntax.exp.LVal;
import front.parser.syntax.exp.MulExp;
import front.parser.syntax.exp.PrimaryExp;
import front.parser.syntax.exp.PrimaryUnaryExp;
import front.parser.syntax.exp.UnaryExp;
import middle.checker.symbol.FuncSymbol;
import middle.checker.symbol.Symbol;
import middle.checker.symbol.SymbolManager;
import middle.checker.symbol.SymbolType;

import java.util.List;

public class GetExpType {
    public static SymbolType getExpType(Exp exp) {
        if (exp == null) {
            System.out.println("exp is null --by getExpType");
            return SymbolType.INT; // 默认类型
        }

        // Exp → AddExp
        AddExp addExp = exp.getAddExp();
        if (addExp != null) {
            return getAddExpType(addExp);
        }

        System.out.println("something strange happened --by getExpType");
        return SymbolType.INT; // 默认类型
    }

    /**
     * 获取AddExp的类型
     */
    private static SymbolType getAddExpType(AddExp addExp) {
        if (addExp == null) {
            return SymbolType.INT;
        }

        // 如果是简单的MulExp（没有加减运算）
        if (addExp.getAddExp() == null) {
            return getMulExpType(addExp.getMulExp());
        }

        // 如果有加减运算，结果类型取决于操作数类型
        // AddExp ('+' | '−') MulExp
        SymbolType leftType = getAddExpType(addExp.getAddExp());
        SymbolType rightType = getMulExpType(addExp.getMulExp());

        // 算术运算的结果类型推断
        return getArithmeticResultType(leftType, rightType);
    }

    /**
     * 获取MulExp的类型
     */
    private static SymbolType getMulExpType(MulExp mulExp) {
        if (mulExp == null) {
            return SymbolType.INT;
        }

        // 如果是简单的UnaryExp（没有乘除运算）
        if (mulExp.getMulExp() == null) {
            return getUnaryExpType(mulExp.getUnaryExp());
        }

        // 如果有乘除运算，结果类型取决于操作数类型
        // MulExp ('*' | '/' | '%') UnaryExp
        SymbolType leftType = getMulExpType(mulExp.getMulExp());
        SymbolType rightType = getUnaryExpType(mulExp.getUnaryExp());

        // 算术运算的结果类型推断
        return getArithmeticResultType(leftType, rightType);
    }

    /**
     * 获取UnaryExp的类型
     */
    private static SymbolType getUnaryExpType(UnaryExp unaryExp) {
        if (unaryExp == null) {
            return SymbolType.INT;
        }

        // 判断UnaryExp的具体类型
        if (unaryExp instanceof PrimaryUnaryExp) {
            PrimaryUnaryExp primaryUnaryExp = (PrimaryUnaryExp) unaryExp;
            return getPrimaryExpType(primaryUnaryExp.getPrimaryExp());
        } else if (unaryExp instanceof FuncCallUnaryExp) {
            FuncCallUnaryExp funcCallUnaryExp = (FuncCallUnaryExp) unaryExp;
            return getFuncCallType(funcCallUnaryExp);
        } else {
            // UnaryOp UnaryExp 的情况
            // 一元运算符不改变基本类型，只是改变值
            List<AstNode> children = unaryExp.getChildren();
            for (AstNode child : children) {
                if (child instanceof UnaryExp) {
                    return getUnaryExpType((UnaryExp) child);
                }
            }
        }

        return SymbolType.INT;
    }

    /**
     * 获取PrimaryExp的类型
     */
    private static SymbolType getPrimaryExpType(PrimaryExp primaryExp) {
        if (primaryExp == null) {
            return SymbolType.INT;
        }

        // PrimaryExp → '(' Exp ')' | LVal | Number
        if (primaryExp.isExp()) {
            // 括号表达式，递归获取内部表达式类型
            return getExpType(primaryExp.getExp());
        } else if (primaryExp.isLVal()) {
            // 左值表达式，需要查找符号表
            return getLValType(primaryExp.getLVal());
        } else if (primaryExp.isNumber()) {
            // 数字常量，类型为int
            return SymbolType.INT;
        }

        return SymbolType.INT;
    }

    /**
     * 获取LVal的类型
     */
    private static SymbolType getLValType(LVal lVal) {
        if (lVal == null) {
            return SymbolType.INT;
        }

        String identifierName = lVal.getIdentifierName();
        if (identifierName == null) {
            return SymbolType.INT;
        }

        // 从符号表中查找符号
        Symbol symbol = SymbolManager.getSymbol(identifierName);
        if (symbol == null) {
            return SymbolType.INT; // 未定义的符号，返回默认类型
        }

        SymbolType symbolType = symbol.getSymbolType();

        // 如果是数组访问
        if (lVal.isArray()) {
            // 数组访问返回元素类型
            switch (symbolType) {
                case INT_ARRAY:
                case STATIC_INT_ARRAY:
                    return SymbolType.INT;
                case CONST_INT_ARRAY:
                    return SymbolType.CONST_INT;
                default:
                    return SymbolType.INT;
            }
        } else {
            // 直接返回符号类型
            return symbolType;
        }
    }

    /**
     * 获取函数调用的返回类型
     */
    private static SymbolType getFuncCallType(FuncCallUnaryExp funcCallUnaryExp) {
        if (funcCallUnaryExp == null) {
            return SymbolType.INT;
        }

        String funcName = funcCallUnaryExp.getFunctionName();
        if (funcName == null) {
            return SymbolType.INT;
        }

        // 从符号表中查找函数符号
        Symbol symbol = SymbolManager.getSymbol(funcName);
        if (symbol == null || !(symbol instanceof FuncSymbol)) {
            return SymbolType.INT; // 未定义的函数，返回默认类型
        }

        SymbolType funcType = symbol.getSymbolType();

        // 根据函数类型返回相应的返回值类型
        switch (funcType) {
            case INT_FUNC:
                return SymbolType.INT;
            case VOID_FUNC:
                return SymbolType.INT; // void函数在表达式中使用时，可能需要特殊处理
            default:
                return SymbolType.INT;
        }
    }

    /**
     * 获取算术运算的结果类型
     */
    private static SymbolType getArithmeticResultType(SymbolType leftType, SymbolType rightType) {
        // 如果任一操作数是常量，结果可能是常量（但这里简化处理）
        // 实际编译器中可能需要更复杂的常量传播分析

        // 简化规则：算术运算结果通常是int类型
        // 除非两个操作数都是常量且可以在编译时计算

        if (leftType == SymbolType.CONST_INT && rightType == SymbolType.CONST_INT) {
            return SymbolType.CONST_INT; // 两个常量的运算结果仍是常量
        }

        return SymbolType.INT; // 其他情况返回int类型
    }
}
