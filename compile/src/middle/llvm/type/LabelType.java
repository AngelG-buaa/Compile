package middle.llvm.type;

/**
 * LLVM IR标签类型实现
 * 
 * 表示LLVM IR中的标签类型，用于标识基本块（Basic Block）。
 * 
 * 在LLVM IR中的使用场景：
 * - 基本块标签：每个基本块都有一个标签作为标识符
 * - 跳转目标：br、switch等控制流指令的跳转目标
 * - 不能用于变量声明：label类型不能作为变量的类型
 * - 不能进行算术运算：label类型没有具体的数值
 * 
 * 在LLVM IR中的表示形式：
 * - entry:                          ; 基本块标签
 * - loop_body:                      ; 循环体标签  
 * - if_then:                        ; 条件分支标签
 * - br label %loop_body             ; 无条件跳转到标签
 * - br i1 %cond, label %if_then, label %if_else  ; 条件跳转
 * 
 * 对应控制流结构：
 * - if语句的分支：if_then, if_else, if_end
 * - while循环：loop_cond, loop_body, loop_end
 * - for循环：for_init, for_cond, for_body, for_update, for_end
 * 
 * 基本块示例：
 * ```llvm
 * entry:                    ; 入口基本块
 *   %cmp = icmp slt i32 %a, %b
 *   br i1 %cmp, label %if_then, label %if_else
 * 
 * if_then:                  ; then分支基本块
 *   %result = add i32 %a, %b
 *   br label %if_end
 * 
 * if_else:                  ; else分支基本块
 *   %result2 = sub i32 %a, %b
 *   br label %if_end
 * 
 * if_end:                   ; 汇合基本块
 *   %final = phi i32 [%result, %if_then], [%result2, %if_else]
 *   ret i32 %final
 * ```
 */
public class LabelType extends IRType {
    
    @Override
    public int getByteSize() {
        // 标签类型不应该被用于计算字节大小
        // 标签只是基本块的标识符，不占用实际存储空间
        throw new UnsupportedOperationException("Label type should not be used for byte size calculation: " + getClass());
    }
    
    @Override
    public int getAlignment() {
        // 标签类型不应该被用于计算对齐
        // 标签不是数据类型，没有内存对齐的概念
        throw new UnsupportedOperationException("Label type should not be used for alignment calculation: " + getClass());
    }
    
    /**
     * 判断是否为标签类型
     * 
     * @return 始终返回true
     */
    public boolean isLabelType() {
        return true;
    }
    
    /**
     * 返回LLVM IR格式的标签类型字符串
     * 
     * @return "label"
     */
    @Override
    public String toString() {
        return "label";
    }
}