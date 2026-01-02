package middle.llvm.value;

import middle.llvm.type.IRType;
import middle.llvm.type.PointerType;
import middle.llvm.value.constant.IRConstant;

public class IRStaticVariable extends IRValue {
    /**
     * 全局变量的初始值
     * 必须是编译时常量，用于在程序启动时初始化变量
     */
    private final IRConstant initializer;

    /**
     * 构造全局变量
     *
     * <p>全局变量的类型总是指针类型，指向实际存储的数据类型。
     * 这是因为在LLVM IR中，全局变量名实际上是指向全局数据的指针。
     *
     * @param name 变量名（不包含@前缀）
     * @param initializer 初始值常量
     */
    public IRStaticVariable(String name, IRConstant initializer) {
        super(null, name, new PointerType(initializer.getType()));
        this.initializer = initializer;
    }

    /**
     * 获取全局变量的初始值
     *
     * @return 初始值常量，用于在程序启动时初始化变量
     */
    public IRConstant getInit() {
        return initializer;
    }

    /**
     * 获取全局变量指向的实际数据类型
     *
     * <p>由于全局变量的类型是指针类型，此方法返回指针指向的实际类型。
     * 例如：{@code @g_a = global i32 0} 中，变量类型是 {@code i32*}，
     * 指向的类型是 {@code i32}。
     *
     * @return 指向的实际数据类型
     */
    public IRType getPointeeType() {
        return ((PointerType) getType()).getPointeeType();
    }

    /**
     * 生成全局变量的LLVM IR声明字符串
     *
     * <p>格式：{@code @name = dso_local [constant|global] <type> <initializer>}
     *
     * <p>生成示例：
     * <ul>
     *   <li>{@code @g_a = dso_local global i32 97} - 全局整数变量</li>
     *   <li>{@code @g_const = dso_local constant i32 42} - 全局整数常量</li>
     *   <li>{@code @g_arr = dso_local global [10 x i32] zeroinitializer} - 零初始化数组</li>
     * </ul>
     *
     * @return 全局变量的LLVM IR声明字符串
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(getName()).append(" = internal ");

        builder.append("global ");

        IRType pointeeType = getPointeeType();
        builder.append(pointeeType.toString()).append(" ");

        // 初始值
        if (initializer != null) {
            builder.append(initializer.toString());
        } else {
            // 零初始化
            if (pointeeType instanceof middle.llvm.type.ArrayType) {
                builder.append("zeroinitializer");
            } else {
                builder.append("0");
            }
        }

        return builder.toString();
    }
}