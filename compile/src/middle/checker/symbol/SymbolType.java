package middle.checker.symbol;

import java.util.HashMap;
import java.util.Map;

/**
 * 符号类型枚举 - 定义编译器中所有可能的符号类型
 * 使用策略模式和缓存机制优化类型解析性能
 */
public enum SymbolType {
    // 基本变量类型
    INT("Int"),
    CONST_INT("ConstInt"),
    
    // 数组类型
    INT_ARRAY("IntArray"),
    CONST_INT_ARRAY("ConstIntArray"),
    
    // 静态类型
    STATIC_INT("StaticInt"),
    STATIC_INT_ARRAY("StaticIntArray"),
    
    // 函数类型
    VOID_FUNC("VoidFunc"),
    INT_FUNC("IntFunc"),
    
    // 错误类型
    ERROR("Error");

    // 类型名称标识
    private final String typeIdentifier;
    
    // 静态缓存映射，提高查找效率
    private static final Map<String, SymbolType> VARIABLE_TYPE_CACHE = new HashMap<>();
    private static final Map<String, SymbolType> CONSTANT_TYPE_CACHE = new HashMap<>();
    private static final Map<String, SymbolType> FUNCTION_TYPE_CACHE = new HashMap<>();
    
    // 静态初始化缓存
    static {
        initializeTypeCaches();
    }

    /**
     * 构造符号类型
     * @param typeIdentifier 类型标识符
     */
    SymbolType(String typeIdentifier) {
        this.typeIdentifier = typeIdentifier;
    }

    /**
     * 获取类型的字符串表示
     */
    @Override
    public String toString() {
        return this.typeIdentifier;
    }

    /**
     * 初始化类型缓存映射
     */
    private static void initializeTypeCaches() {
        // 变量类型缓存
        VARIABLE_TYPE_CACHE.put("int", INT);
        
        // 常量类型缓存
        CONSTANT_TYPE_CACHE.put("int", CONST_INT);
        
        // 函数类型缓存
        FUNCTION_TYPE_CACHE.put("void", VOID_FUNC);
        FUNCTION_TYPE_CACHE.put("int", INT_FUNC);
    }

    /**
     * 根据类型字符串获取变量类型
     * @param typeString 类型字符串
     * @return 对应的符号类型，未找到返回ERROR
     */
    public static SymbolType getVarType(String typeString) {
        return VARIABLE_TYPE_CACHE.getOrDefault(typeString, ERROR);
    }

    /**
     * 根据类型字符串和维度获取变量类型
     * @param typeString 类型字符串
     * @param dimension 数组维度
     * @return 对应的符号类型，未找到返回ERROR
     */
    public static SymbolType getVarType(String typeString, int dimension) {
        if (dimension == 0) {
            return getVarType(typeString);
        }
        
        // 处理数组类型
        if ("int".equals(typeString)) {
            return INT_ARRAY;
        }
        
        return ERROR;
    }

    /**
     * 根据类型字符串获取常量类型
     * @param typeString 类型字符串
     * @return 对应的常量符号类型，未找到返回ERROR
     */
    public static SymbolType getConstType(String typeString) {
        return CONSTANT_TYPE_CACHE.getOrDefault(typeString, ERROR);
    }

    /**
     * 根据类型字符串和维度获取常量类型
     * @param typeString 类型字符串
     * @param dimension 数组维度
     * @return 对应的常量符号类型，未找到返回ERROR
     */
    public static SymbolType getConstType(String typeString, int dimension) {
        if (dimension == 0) {
            return getConstType(typeString);
        }
        
        // 处理常量数组类型
        if ("int".equals(typeString)) {
            return CONST_INT_ARRAY;
        }
        
        return ERROR;
    }

    /**
     * 根据类型字符串获取函数类型
     * @param typeString 函数返回类型字符串
     * @return 对应的函数符号类型，未找到返回ERROR
     */
    public static SymbolType getFuncType(String typeString) {
        return FUNCTION_TYPE_CACHE.getOrDefault(typeString, ERROR);
    }
}