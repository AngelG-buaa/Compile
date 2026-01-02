package middle.checker.symbol;

import front.parser.syntax.func.FuncFParam;
import front.parser.syntax.func.FuncFParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 函数符号类，继承自Symbol，用于表示函数定义的符号信息
 * 包含函数的形式参数列表等信息
 */
public class FuncSymbol extends Symbol {
    /**
     * 函数的形式参数列表
     */
    private List<SymbolType> parameterList;

    /**
     * 构造函数，创建一个空参数列表的函数符号
     * 
     * @param symbolName 符号名称
     * @param symbolType 符号类型
     */
    public FuncSymbol(String symbolName, SymbolType symbolType) {
        super(symbolName, symbolType);
        this.parameterList = new ArrayList<>();
    }

    /**
     * 构造函数，创建一个带参数列表的函数符号
     * 
     * @param symbolName 符号名称
     * @param symbolType 符号类型
     * @param formalParamList 形式参数列表
     */
    public FuncSymbol(String symbolName, SymbolType symbolType,
                      FuncFParams formalParamList) {
        super(symbolName, symbolType);
        // this.parameterList = Objects.requireNonNull(formalParamList, "Parameter list cannot be null");
        this.parameterList = new ArrayList<>();
        if (formalParamList != null) {
            for (FuncFParam param : formalParamList.getParams()) {
                if (param.isArray()) {
                    parameterList.add(SymbolType.INT_ARRAY);
                } else {
                    parameterList.add(SymbolType.INT);
                }
            }
        }
    }

    /**
     * 获取函数的形式参数的SymbolType列表
     * 
     * @return 形式参数列表的副本，防止外部修改
     */
    public ArrayList<SymbolType> getFormalParamList() {
        return new ArrayList<>(this.parameterList);
    }

    /**
     * 获取参数数量
     * 
     * @return 参数数量
     */
    public int getParameterCount() {
        return this.parameterList.size();
    }

    /**
     * 检查是否有参数
     * 
     * @return 如果有参数返回true，否则返回false
     */
    public boolean hasParameters() {
        return !this.parameterList.isEmpty();
    }
}