package middle.llvm.IR_Symbol;

import middle.llvm.value.IRValue;
import java.util.HashMap;
import java.util.Map;

/**
 * LLVM IR符号表实现
 * 用于管理中间代码生成过程中的符号信息
 */
public class IRSymTable {
    private IRSymTable parentScope;
    private final Map<String, IRValue> symbolMap;
    
    public IRSymTable(IRSymTable parentScope) {
        this.parentScope = parentScope;
        this.symbolMap = new HashMap<>();
    }
    
    /**
     * 获取父作用域
     */
    public IRSymTable getParentScope() {
        return this.parentScope;
    }
    
    /**
     * 向当前作用域添加符号
     */
    public void insertSymbol(String symbolName, IRValue value) {
        symbolMap.put(symbolName, value);
    }
    
    /**
     * 查找符号，支持作用域链查找
     */
    public IRValue lookupSymbol(String symbolName) {
        IRSymTable currentTable = this;
        while(currentTable != null) {
            if (currentTable.symbolMap.containsKey(symbolName)) {
                return currentTable.symbolMap.get(symbolName);
            }
            currentTable = currentTable.getParentScope();
        }
        return null;
    }
    
    /**
     * 检查当前作用域是否包含指定符号
     */
    public boolean containsInCurrentScope(String symbolName) {
        return symbolMap.containsKey(symbolName);
    }
    
    /**
     * 获取当前作用域的所有符号
     */
    public Map<String, IRValue> getCurrentScopeSymbols() {
        return new HashMap<>(symbolMap);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Current Scope Symbols: ");
        for (String symbol : symbolMap.keySet()) {
            builder.append(symbol).append(" ");
        }
        builder.append("\n");
        if (parentScope != null) {
            builder.append("Parent: ").append(parentScope.toString());
        }
        return builder.toString();
    }
}