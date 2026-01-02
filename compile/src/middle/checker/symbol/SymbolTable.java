package middle.checker.symbol;

import error.Error;
import error.ErrorManager;
import error.ErrorType;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 符号表实现 - 管理作用域内的符号信息
 * 采用链式结构和双重存储策略，支持快速查找和有序遍历
 */
public class SymbolTable {
    // 作用域层级深度
    private final int scopeLevel;
    
    // 子表访问索引，用于遍历子作用域
    private int childTableIndex;

    // 符号存储 - 使用LinkedHashMap保持插入顺序
    private final Map<String, Symbol> symbolRegistry;
    
    // 符号列表 - 用于有序访问和输出
    private final List<Symbol> symbolSequence;

    // 父级符号表引用
    private final SymbolTable parentScope;
    
    // 子级符号表集合
    private final List<SymbolTable> childScopes;

    /**
     * 构造符号表
     * @param scopeLevel 作用域层级
     * @param parentScope 父级符号表
     */
    public SymbolTable(int scopeLevel, SymbolTable parentScope) {
        this.scopeLevel = scopeLevel;
        this.childTableIndex = -1;

        // 使用LinkedHashMap保持符号插入顺序
        this.symbolRegistry = new LinkedHashMap<>();
        this.symbolSequence = new LinkedList<>();

        this.parentScope = parentScope;
        this.childScopes = new LinkedList<>();
    }

    /**
     * 获取作用域深度
     * @return 作用域层级数值
     */
    public int getDepth() {
        return this.scopeLevel;
    }

    /**
     * 在当前作用域查找符号
     * @param symbolName 符号名称
     * @return 找到的符号，不存在返回null
     */
    public Symbol getSymbol(String symbolName) {
        return this.symbolRegistry.get(symbolName);
    }

    /**
     * 获取父级符号表
     * @return 父级符号表引用
     */
    public SymbolTable getFatherTable() {
        return this.parentScope;
    }

    /**
     * 添加子符号表
     * @param symbolTable 子符号表实例
     */
    public void addSonTable(SymbolTable symbolTable) {
        this.childScopes.add(symbolTable);
    }

    /**
     * 向当前作用域添加符号
     * @param symbol 要添加的符号
     * @param line 符号定义的行号
     */
    public void addSymbol(Symbol symbol, int line) {
        String symbolName = symbol.getSymbolName();
        
        // 检查当前作用域是否已存在同名符号
        if (!this.symbolRegistry.containsKey(symbolName)) {
            // 添加到有序列表和映射表
            this.symbolSequence.add(symbol);
            this.symbolRegistry.put(symbolName, symbol);
        } else {
            // 符号重定义错误
            ErrorManager.AddError(Error.createError(ErrorType.NAME_REDEFINE, line));
        }
    }

    /**
     * 获取下一个子符号表
     * @return 下一个子符号表实例
     */
    public SymbolTable getNextSonTable() {
        return this.childScopes.get(++childTableIndex);
    }

    /**
     * 生成符号表的字符串表示
     * 递归输出当前表及所有子表的符号信息
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        
        // 输出当前作用域的所有符号
        for (Symbol symbol : this.symbolSequence) {
            output.append(String.format("%d %s%n", this.scopeLevel, symbol));
        }

        // 递归输出所有子作用域
        for (SymbolTable childScope : this.childScopes) {
            output.append(childScope.toString());
        }

        return output.toString();
    }
}