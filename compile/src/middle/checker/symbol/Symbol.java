package middle.checker.symbol;

import java.util.Objects;

/**
 * 符号基类 - 表示编译器中的符号实体
 * 采用不可变设计模式，确保符号信息的稳定性
 */
public class Symbol {
    // 使用私有字段和访问器模式
    private final String identifier;
    private final SymbolType category;
    
    /**
     * 构造符号实例
     * @param identifier 符号标识符
     * @param category 符号类别
     */
    public Symbol(String identifier, SymbolType category) {
        this.identifier = Objects.requireNonNull(identifier, "符号标识符不能为空");
        this.category = Objects.requireNonNull(category, "符号类别不能为空");
    }

    /**
     * 获取符号名称
     * @return 符号标识符字符串
     */
    public String getSymbolName() {
        return this.identifier;
    }

    /**
     * 获取符号类型
     * @return 符号类别枚举
     */
    public SymbolType getSymbolType() {
        return this.category;
    }

    /**
     * 生成符号的字符串表示
     * 格式: "标识符 类型"
     */
    @Override
    public String toString() {
        return String.format("%s %s", this.identifier, this.category.toString());
    }

    /**
     * 符号相等性判断 - 基于标识符比较
     * 两个符号如果标识符相同则认为相等
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Symbol)) {
            return false;
        }
        Symbol otherSymbol = (Symbol) other;
        return Objects.equals(this.identifier, otherSymbol.identifier);
    }

    /**
     * 计算符号哈希值 - 基于标识符
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.identifier);
    }
}