package middle.checker.symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 值符号类，继承自Symbol，用于表示变量和常量的符号信息
 * 包含维度、深度列表、初始值列表等信息
 */
public class ValueSymbol extends Symbol {

    
    /**
     * 是否为全局变量
     */
    private boolean globalFlag;
    
    /**
     * 是否为常量
     */
    private boolean constantFlag;
    
    /**
     * 运行时值列表
     */
    private List<Integer> runtimeValues;

    /**
     * 构造函数，创建一个基本的值符号
     * 
     * @param symbolName 符号名称
     * @param symbolType 符号类型
     */
    public ValueSymbol(String symbolName, SymbolType symbolType) {
        super(symbolName, symbolType);
        this.globalFlag = false;
        this.constantFlag = false;
        this.runtimeValues = new ArrayList<>();
    }

    /**
     * 设置全局变量标志
     * 
     * @param isGlobal 是否为全局变量
     */
    public void setIsGlobal(boolean isGlobal) {
        this.globalFlag = isGlobal;
    }

    /**
     * 检查是否为常量
     * 
     * @return 如果是常量返回true，否则返回false
     */
    public boolean isConst() {
        return this.constantFlag;
    }

}