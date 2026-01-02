package middle.llvm.type;

import java.util.Objects;

/**
 * LLVM IR整数类型实现
 *
 * 表示LLVM IR中的整数类型，包括：
 * - i1: 1位整数，通常用于布尔值（true/false）
 * - i8: 8位整数，通常用于字符或字节
 * - i32: 32位整数，通常用于普通整数
 *
 * 在LLVM IR中的表示形式：
 * - i1 %var = icmp eq i32 %a, %b    ; 比较结果为i1类型
 * - i8 %char = load i8, i8* %ptr    ; 加载字符
 * - i32 %num = add i32 %a, %b       ; 32位整数运算
 *
 * 对应SysY语言中的类型：
 * - i1: 条件表达式的结果
 * - i8: 字符常量和字符数组元素
 * - i32: int类型变量和数组元素
 */
public class IntegerType extends IRType {
    public static final IntegerType I1 = new IntegerType(1);
    public static final IntegerType I8 = new IntegerType(8);
    public static final IntegerType I32 = new IntegerType(32);

    /**
     * 整数类型的位宽
     * - 1: 布尔类型 (i1)
     * - 8: 字节类型 (i8)
     * - 32: 整数类型 (i32)
     */
    private final int bitWidth;

    /**
     * 构造整数类型
     *
     * @param bitWidth 位宽，支持1、8、32
     */
    IntegerType(int bitWidth) {
        this.bitWidth = bitWidth;
    }

    /**
     * 获取位宽
     *
     * @return 整数类型的位宽
     */
    public int getBitWidth() {
        return bitWidth;
    }

    @Override
    public boolean isBasicIntegerType() {
        return true;
    }

    /**
     * 判断是否为布尔类型 (i1)
     *
     * i1类型主要用于：
     * - 条件判断的结果
     * - icmp指令的返回值
     * - br指令的条件
     *
     * @return 如果是i1类型返回true
     */
    public boolean isBoolType() {
        return this.bitWidth == 1;
    }

    /**
     * 判断是否为字节类型 (i8)
     *
     * i8类型主要用于：
     * - 字符常量
     * - 字符串中的字符
     * - 字节数组
     *
     * @return 如果是i8类型返回true
     */
    public boolean isByteType() {
        return this.bitWidth == 8;
    }

    /**
     * 判断是否为32位整数类型 (i32)
     *
     * i32类型主要用于：
     * - SysY中的int变量
     * - 数组索引
     * - 算术运算结果
     *
     * @return 如果是i32类型返回true
     */
    public boolean isInt32Type() {
        return this.bitWidth == 32;
    }

    /**
     * return this == I1 || this == I8 || this == I32;
     */
    public boolean isI() {
        return this == I1 || this == I8 || this == I32;
    }

    @Override
    public int getByteSize() {
        // i1类型虽然只有1位，但最小存储单位是1字节
        if (bitWidth == 1) {
            return 1;
        }
        // i8类型占用1字节
        else if (bitWidth == 8) {
            return 1;
        }
        // i32类型占用4字节
        else if (bitWidth == 32) {
            return 4;
        }
        // 对于其他位宽，可以抛出异常或进行相应计算
        throw new IllegalArgumentException("Unsupported bit width for byte size calculation: " + bitWidth);
    }

    @Override
    public int getAlignment() {
        // 对齐要求通常等于字节大小
        return getByteSize();
    }

    /**
     * 返回LLVM IR格式的类型字符串
     *
     * @return "i1", "i8", 或 "i32"
     */
    @Override
    public String toString() {
        return "i" + bitWidth;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IntegerType that = (IntegerType) obj;
        return bitWidth == that.bitWidth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitWidth);
    }
}