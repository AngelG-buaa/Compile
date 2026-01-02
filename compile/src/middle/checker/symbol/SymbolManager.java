package middle.checker.symbol;

/**
 * 符号管理器 - 全局符号表管理和作用域控制
 * 采用单例模式和状态机设计，提供符号表的层次化管理
 */
public class SymbolManager {
    // 符号表层次结构
    private static SymbolTable globalScope;
    private static SymbolTable activeScope;
    private static int scopeDepth;

    // 循环嵌套控制
    private static int loopNestingLevel;
    
    // 函数上下文管理
    private static String currentFunctionReturnType = "";

    /**
     * 初始化符号管理器
     * 创建全局作用域并重置所有状态
     */
    public static void init() {
        scopeDepth = 1;
        globalScope = new SymbolTable(scopeDepth, null);
        activeScope = globalScope;

        loopNestingLevel = 0;
        currentFunctionReturnType = "";
    }

    /**
     * 回到全局作用域
     * 将当前活动作用域重置为全局作用域
     */
    public static void goBackToRootSymbolTable() {
        activeScope = globalScope;
    }

    /**
     * 检查当前是否在全局作用域
     * @return true表示在全局作用域，false表示在局部作用域
     */
    public static boolean isGlobal() {
        return activeScope == globalScope;
    }

    /**
     * 向当前作用域添加符号
     * @param symbol 要添加的符号
     * @param line 符号定义所在行号
     */
    public static void addSymbol(Symbol symbol, int line) {
        activeScope.addSymbol(symbol, line);
        
        // 如果是值符号，设置其全局性标记
        if (symbol instanceof ValueSymbol valueSymbol) {
            valueSymbol.setIsGlobal(isGlobal());
        }
    }

    /**
     * 在作用域链中查找符号
     * 从当前作用域开始，向上逐级查找直到全局作用域
     * @param name 符号名称
     * @return 找到的符号，不存在返回null
     */
    public static Symbol getSymbol(String name) {
        SymbolTable searchScope = activeScope;
        
        while (searchScope != null) {
            Symbol foundSymbol = searchScope.getSymbol(name);
            if (foundSymbol != null) {
                return foundSymbol;
            }
            searchScope = searchScope.getFatherTable();
        }
        
        return null;
    }

    /**
     * 在父级作用域中查找符号
     * 跳过当前作用域，从父级开始查找
     * @param name 符号名称
     * @return 找到的符号，不存在返回null
     */
    public static Symbol getSymbolFromFather(String name) {
        SymbolTable searchScope = activeScope.getFatherTable();
        
        while (searchScope != null) {
            Symbol foundSymbol = searchScope.getSymbol(name);
            if (foundSymbol != null) {
                return foundSymbol;
            }
            searchScope = searchScope.getFatherTable();
        }
        
        return null;
    }

    /**
     * 获取根符号表
     * @return 全局符号表实例
     */
    public static SymbolTable getSymbolTable() {
        return globalScope;
    }

    /**
     * 获取当前活动符号表
     * @return 当前作用域符号表
     */
    public static SymbolTable getCurrentSymbolTable() {
        return activeScope;
    }

    /**
     * 返回到父级作用域
     * 将当前活动作用域切换到其父级作用域
     */
    public static void goToFatherSymbolTable() {
        SymbolTable parentScope = activeScope.getFatherTable();
        if (parentScope != null) {
            activeScope = parentScope;
        }
    }

    /**
     * 创建新的子作用域
     * 在当前作用域下创建新的子作用域并切换到该作用域
     */
    public static void createSonSymbolTable() {
        SymbolTable childScope = new SymbolTable(++scopeDepth, activeScope);
        activeScope.addSonTable(childScope);
        activeScope = childScope;
    }

    /**
     * 切换到下一个子作用域
     * 用于遍历已存在的子作用域
     */
    public static void goToSonSymbolTable() {
        activeScope = activeScope.getNextSonTable();
    }

    /**
     * 进入循环块
     * 增加循环嵌套层级计数
     */
    public static void enterForBlock() {
        loopNestingLevel++;
    }

    /**
     * 离开循环块
     * 减少循环嵌套层级计数
     */
    public static void leaveForBlock() {
        loopNestingLevel--;
    }

    /**
     * 检查是否不在循环块中
     * @return true表示不在任何循环中，false表示在循环中
     */
    public static boolean notInForBlock() {
        return loopNestingLevel <= 0;
    }

    /**
     * 进入函数定义
     * 设置当前函数的返回类型
     * @param type 函数返回类型
     */
    public static void enterFunc(String type) {
        currentFunctionReturnType = type;
    }

    /**
     * 离开函数定义
     * 清空当前函数返回类型
     */
    public static void leaveFunc() {
        currentFunctionReturnType = "";
    }

    /**
     * 获取当前函数返回类型
     * @return 当前函数的返回类型字符串
     */
    public static String getFuncType() {
        return currentFunctionReturnType;
    }
}