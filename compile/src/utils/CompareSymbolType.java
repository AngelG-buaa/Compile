package utils;

import middle.checker.symbol.SymbolType;

public class CompareSymbolType {
    /**
     * 检查类型兼容性
     */
    public static boolean isTypeCompatible(SymbolType actualType, SymbolType formalType) {
        // 将actualType映射到基本类别
        // 将formalType映射到基本类别
        // 只要映射后的类别相同即可
        return mergeSymbolType(actualType) == mergeSymbolType(formalType);
    }

    private static int mergeSymbolType(SymbolType type) {
        int mergedType;
        switch (type) {
            case INT:
            case CONST_INT:
            case STATIC_INT:
                mergedType = 1; // INT类别
                break;
            case INT_ARRAY:
            case CONST_INT_ARRAY:
            case STATIC_INT_ARRAY:
                mergedType = 2; // ARRAY类别
                break;
            case INT_FUNC:
            case VOID_FUNC:
                mergedType = 3; // FUNC类别
                break;
            default:
                mergedType = 0; // OTHER类别
                break;
        }

        return mergedType;
    }
}
