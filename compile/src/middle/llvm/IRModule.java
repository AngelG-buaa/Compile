package middle.llvm;

import middle.llvm.value.IRFunction;
import middle.llvm.value.IRGlobalVariable;
import middle.llvm.value.IRStaticVariable;
import middle.llvm.value.IRStringLiteral;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLVM IR模块管理器
 * 
 * <p>负责管理整个LLVM IR模块的顶层结构，包括：
 * <ul>
 *   <li>全局变量和常量的集合管理</li>
 *   <li>函数定义和声明的组织</li>
 *   <li>字符串字面量的统一存储</li>
 *   <li>外部库函数的声明管理</li>
 *   <li>完整IR代码的生成和输出</li>
 * </ul>
 * 
 * <p>LLVM IR模块的标准结构：
 * <pre>
 * ; 外部函数声明
 * declare i32 @getint()
 * declare void @putint(i32)
 * 
 * ; 全局字符串字面量
 * @str.0 = private unnamed_addr constant [6 x i8] c"hello\00", align 1
 * 
 * ; 全局变量定义
 * @g_var = dso_local global i32 0, align 4
 * 
 * ; 函数定义
 * define i32 @main() {
 *   ; 函数体
 * }
 * </pre>
 * 
 * <p>模块管理的核心职责：
 * <ul>
 *   <li>维护全局符号表，避免名称冲突</li>
 *   <li>确保声明和定义的正确顺序</li>
 *   <li>提供统一的IR代码生成接口</li>
 *   <li>支持增量式的模块构建</li>
 * </ul>
 * 
 * <p>与SysY编译器的集成：
 * <ul>
 *   <li>全局变量声明对应SysY的全局变量定义</li>
 *   <li>函数定义对应SysY的函数实现</li>
 *   <li>字符串字面量来自SysY的字符串常量</li>
 *   <li>库函数声明支持SysY的内置函数调用</li>
 * </ul>
 * 
 * @see IRGlobalVariable 全局变量实现
 * @see IRFunction 函数实现
 * @see IRStringLiteral 字符串字面量实现
 */
public class IRModule {
    
    /**
     * 字符串字面量存储容器
     * 使用有序列表保证输出的确定性
     */
    private final List<IRStringLiteral> stringLiterals;
    
    /**
     * 全局变量存储容器
     * 包括全局变量、全局常量
     */
    private final List<IRGlobalVariable> globalVariables;

    private final List<IRStaticVariable> staticVariables;
    
    /**
     * 函数定义存储容器
     * 包括用户定义函数和主函数，主函数通常位于最后
     */
    private final List<IRFunction> functionDefinitions;
    
    /**
     * 全局符号名称映射表
     * 用于快速查找和避免重复定义
     */
    private final Map<String, Object> symbolTable;
    
    /**
     * 字符串字面量计数器
     * 用于生成唯一的字符串标识符
     */
    private int stringLiteralCounter;
    
    /**
     * 构造函数，初始化空的IR模块
     */
    public IRModule() {
        this.stringLiterals = new ArrayList<>();
        this.globalVariables = new ArrayList<>();
        this.staticVariables = new ArrayList<>();
        this.functionDefinitions = new ArrayList<>();
        this.symbolTable = new LinkedHashMap<>();
        this.stringLiteralCounter = 0;
    }
    
    /**
     * 向模块中添加字符串字面量
     * 
     * @param stringLiteral 要添加的字符串字面量
     * @throws IllegalArgumentException 如果字符串字面量为null
     */
    public void registerStringLiteral(IRStringLiteral stringLiteral) {
        if (stringLiteral == null) {
            throw new IllegalArgumentException("字符串字面量不能为null");
        }
        
        stringLiterals.add(stringLiteral);
        symbolTable.put(stringLiteral.getName(), stringLiteral);
    }
    
    /**
     * 向模块中添加全局变量
     * 
     * @param globalVariable 要添加的全局变量
     * @throws IllegalArgumentException 如果全局变量为null或名称已存在
     */
    public void registerGlobalVariable(IRGlobalVariable globalVariable) {
        if (globalVariable == null) {
            throw new IllegalArgumentException("全局变量不能为null");
        }
        
        String name = globalVariable.getName();
        if (symbolTable.containsKey(name)) {
            throw new IllegalArgumentException("全局符号名称冲突: " + name);
        }
        
        globalVariables.add(globalVariable);
        symbolTable.put(name, globalVariable);
    }

    public void registerStaticVariable(IRStaticVariable staticVariable) {
        if (staticVariable == null) {
            throw new IllegalArgumentException("STATIC变量不能为null");
        }

        String name = staticVariable.getName();
        if (symbolTable.containsKey(name)) {
            throw new IllegalArgumentException("STATIC变量名称符号冲突: " + name);
        }

        staticVariables.add(staticVariable);
        symbolTable.put(name, staticVariable);
    }

    /**
     * 向模块中添加函数定义
     * 
     * @param function 要添加的函数
     * @throws IllegalArgumentException 如果函数为null或名称已存在
     */
    public void registerFunction(IRFunction function) {
        if (function == null) {
            throw new IllegalArgumentException("函数不能为null");
        }
        
        String name = function.getName();
        if (symbolTable.containsKey(name)) {
            throw new IllegalArgumentException("函数名称冲突: " + name);
        }
        
        functionDefinitions.add(function);
        symbolTable.put(name, function);
    }
    
    /**
     * 获取所有字符串字面量的只读视图
     * 
     * @return 字符串字面量列表
     */
    public List<IRStringLiteral> getStringLiterals() {
        return new ArrayList<>(stringLiterals);
    }
    
    /**
     * 获取所有全局变量的只读视图
     * 
     * @return 全局变量列表
     */
    public List<IRGlobalVariable> getGlobalVariables() {
        return new ArrayList<>(globalVariables);
    }
    
    /**
     * 获取所有函数定义的只读视图
     * 
     * @return 函数定义列表
     */
    public ArrayList<IRFunction> getFunctionDefinitions() {
        return new ArrayList<>(functionDefinitions);
    }

    public List<IRStaticVariable> getStaticVariables() {
        return new ArrayList<>(staticVariables);
    }
    
    /**
     * 检查指定名称的符号是否已存在
     * 
     * @param symbolName 符号名称
     * @return 如果符号存在返回true，否则返回false
     */
    public boolean hasSymbol(String symbolName) {
        return symbolTable.containsKey(symbolName);
    }
    
    /**
     * 获取下一个字符串字面量的计数器值
     * 
     * @return 字符串字面量计数器
     */
    public int getNextStringLiteralId() {
        return stringLiteralCounter++;
    }
    
    /**
     * 获取模块中的符号总数
     * 
     * @return 符号总数
     */
    public int getSymbolCount() {
        return symbolTable.size();
    }
    
    /**
     * 清空模块中的所有内容
     * 用于重新构建或测试场景
     */
    public void clear() {
        stringLiterals.clear();
        globalVariables.clear();
        functionDefinitions.clear();
        symbolTable.clear();
        stringLiteralCounter = 0;
    }
    
    /**
     * 生成完整的LLVM IR模块代码
     * 
     * <p>按照LLVM IR的标准格式输出：
     * <ol>
     *   <li>外部函数声明</li>
     *   <li>全局字符串字面量</li>
     *   <li>全局变量定义</li>
     *   <li>函数定义</li>
     * </ol>
     * 
     * @return 完整的LLVM IR代码字符串
     */
    @Override
    public String toString() {
        StringBuilder moduleBuilder = new StringBuilder();
        
        // 1. 输出标准库函数声明
        appendLibraryDeclarations(moduleBuilder);
        
        // 2. 输出全局字符串字面量
        if (!stringLiterals.isEmpty()) {
            moduleBuilder.append("; 全局字符串字面量\n");
            for (IRStringLiteral literal : stringLiterals) {
                moduleBuilder.append(literal.toString()).append("\n");
            }
            moduleBuilder.append("\n");
        }
        
        // 3. 输出全局变量定义
        if (!globalVariables.isEmpty()) {
            moduleBuilder.append("; 全局变量定义\n");
            for (IRGlobalVariable variable : globalVariables) {
                moduleBuilder.append(variable.toString()).append("\n");
            }
            moduleBuilder.append("\n");
        }

        // 4. 输出静态局部变量定义
        if (!staticVariables.isEmpty()) {
            moduleBuilder.append(";静态局部变量定义\n");
            for (IRStaticVariable variable : staticVariables) {
                moduleBuilder.append(variable.toString()).append("\n");
            }
            moduleBuilder.append("\n");
        }
        
        // 5. 输出函数定义
        if (!functionDefinitions.isEmpty()) {
            moduleBuilder.append("; 函数定义\n");
            for (IRFunction function : functionDefinitions) {
                moduleBuilder.append(function.toString()).append("\n\n");
            }
        }
        
        return moduleBuilder.toString();
    }
    
    /**
     * 添加标准库函数声明到模块输出
     * 
     * @param builder 字符串构建器
     */
    private void appendLibraryDeclarations(StringBuilder builder) {
        builder.append("; 标准库函数声明\n");
        builder.append("declare i32 @getint()\n");
        builder.append("declare i32 @getchar()\n");
        builder.append("declare void @putint(i32)\n");
        builder.append("declare void @putch(i8)\n");
        builder.append("declare void @putstr(i8*)\n");
        builder.append("\n");
    }
    
    /**
     * 获取主函数
     * 
     * @return 主函数，如果不存在则返回null
     */
    public IRFunction getMainFunction() {
        for (IRFunction function : functionDefinitions) {
            if ("@main".equals(function.getName())) {
                return function;
            }
        }
        return null;
    }

    public void setFunctionDefinitions(List<IRFunction> functionDefinitions) {
        this.functionDefinitions.clear();
        this.functionDefinitions.addAll(functionDefinitions);
    }
}
