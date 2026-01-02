package middle.llvm.value;

import middle.llvm.type.IRType;

/**
 * LLVM IR函数参数实现
 * 
 * <p>表示LLVM IR中函数的形式参数，是函数定义中的参数声明。
 * 函数参数在LLVM IR中具有以下特征：
 * <ul>
 *   <li>具有明确的类型和索引位置</li>
 *   <li>在函数体内可以被引用和使用</li>
 *   <li>按照调用约定传递实际参数值</li>
 *   <li>可以是基本类型、指针类型或复合类型</li>
 * </ul>
 * 
 * <p>LLVM IR表示示例：
 * <pre>
 * define i32 @func(i32 %a0, i32* %a1, [10 x i32] %a2) {
 *   ; %a0: 第0个参数，i32类型
 *   ; %a1: 第1个参数，i32指针类型  
 *   ; %a2: 第2个参数，数组类型
 *   ...
 * }
 * </pre>
 * 
 * <p>对应SysY语言场景：
 * <ul>
 *   <li>函数形参声明：{@code int func(int a, int b[])} 对应 {@code i32 %a0, i32* %a1}</li>
 *   <li>数组参数传递：数组作为指针传递</li>
 *   <li>值传递：基本类型按值传递</li>
 *   <li>引用传递：数组和指针按引用传递</li>
 * </ul>
 * 
 * @see IRFunction 函数定义
 * @see IRValue 值基类
 * @see IRType 类型系统
 */
public class IRFunctionParameter extends IRValue {
    /**
     * 参数在函数参数列表中的索引位置
     * 从0开始计数，用于生成参数名称和标识参数位置
     */
    private final int index;
    
    /**
     * 构造函数参数
     * 
     * @param parent 父容器（通常是IRFunction）
     * @param index 参数索引，从0开始
     * @param type 参数类型
     */
    public IRFunctionParameter(IRValue parent, int index, IRType type) {
        super(parent, "%a" + index, type);
        this.index = index;
    }
    
    /**
     * 获取参数在函数参数列表中的索引位置
     * 
     * @return 参数索引，从0开始
     */
    public int getIndex() {
        return index;
    }
    
    /**
     * 判断参数是否为指针类型
     * 
     * <p>指针类型参数通常对应：
     * <ul>
     *   <li>SysY中的数组参数</li>
     *   <li>需要修改的变量参数（模拟引用传递）</li>
     *   <li>复合数据结构的传递</li>
     * </ul>
     * 
     * @return 如果参数类型是指针类型则返回true，否则返回false
     */
    public boolean isPointerType() {
        return getType() instanceof middle.llvm.type.PointerType;
    }
    
    /**
     * 生成参数的LLVM IR字符串表示
     * 
     * <p>格式：{@code <type> <name>}
     * <p>示例：
     * <ul>
     *   <li>{@code i32 %a0} - 整数参数</li>
     *   <li>{@code i32* %a1} - 指针参数</li>
     *   <li>{@code [10 x i32] %a2} - 数组参数</li>
     * </ul>
     * 
     * @return 参数的LLVM IR表示字符串
     */
    @Override
    public String toString() {
        return getType() + " " + getName();
    }
}