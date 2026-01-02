package middle.llvm.value;

import middle.llvm.type.ArrayType;
import middle.llvm.type.IntegerType;
import middle.llvm.type.PointerType;

/**
 * LLVM IR字符串字面量实现
 * 
 * <p>表示LLVM IR中的字符串常量，用于存储程序中的字符串字面量。
 * 字符串字面量在LLVM IR中具有以下特征：
 * <ul>
 *   <li>以字符数组的形式存储，每个字符占用一个i8（字节）</li>
 *   <li>自动添加null终止符（\0）以符合C字符串约定</li>
 *   <li>支持转义字符处理（如\n、\t等）</li>
 *   <li>存储为全局常量，具有私有链接属性</li>
 *   <li>在内存中按字节对齐</li>
 * </ul>
 * 
 * <p>LLVM IR表示示例：
 * <pre>
 * ; 简单字符串
 * @str.0 = private unnamed_addr constant [6 x i8] c"hello\00", align 1
 * 
 * ; 包含转义字符的字符串
 * @str.1 = private unnamed_addr constant [13 x i8] c"hello\0Aworld\00", align 1
 * 
 * ; 包含特殊字符的字符串
 * @str.2 = private unnamed_addr constant [8 x i8] c"tab:\09\00", align 1
 * 
 * ; 空字符串
 * @str.3 = private unnamed_addr constant [1 x i8] c"\00", align 1
 * </pre>
 * 
 * <p>对应SysY语言场景：
 * <ul>
 *   <li>字符串字面量：{@code "hello"} 对应 {@code [6 x i8] c"hello\00"}</li>
 *   <li>printf格式字符串：{@code printf("value: %d\n", x)} 中的格式字符串</li>
 *   <li>字符数组初始化：虽然SysY不直接支持，但编译器内部使用</li>
 * </ul>
 * 
 * <p>内存布局特点：
 * <ul>
 *   <li>存储在程序的只读数据段</li>
 *   <li>按字节对齐（align 1）</li>
 *   <li>具有私有链接属性，避免符号冲突</li>
 *   <li>通过指针类型进行访问</li>
 * </ul>
 * 
 * @see ArrayType 数组类型
 * @see IntegerType 整数类型
 * @see PointerType 指针类型
 */
public class IRStringLiteral extends IRValue {
    /**
     * 原始字符串字面量内容
     * 保存未处理的原始字符串，用于调试和错误报告
     */
    private final String originalLiteral;
    
    /**
     * 处理转义字符后的字符串内容
     * 将转义序列（如\n）转换为实际字符，用于生成LLVM IR
     */
    private final String processedLiteral;
    
    /**
     * 构造字符串字面量
     * 
     * <p>字符串字面量的类型是指向字符数组的指针类型。
     * 数组长度包括字符串内容加上null终止符。
     * 
     * @param stringCounter 字符串计数器，用于生成唯一的字符串标识符
     * @param literal 字符串内容（可能包含转义字符）
     */
    public IRStringLiteral(int stringCounter, String literal) {
        super(null, "@str." + stringCounter, new PointerType(calculateArrayType(literal)));
        this.originalLiteral = literal;
        this.processedLiteral = processEscapeSequences(literal);
    }
    
    /**
     * 获取原始字符串字面量内容
     * 
     * @return 未处理的原始字符串
     */
    public String getOriginalLiteral() {
        return originalLiteral;
    }
    
    /**
     * 获取处理转义字符后的字符串内容
     * 
     * @return 处理后的字符串，转义字符已转换为实际字符
     */
    public String getProcessedLiteral() {
        return processedLiteral;
    }
    
    /**
     * 获取字符串的总长度（包含null终止符）
     * 
     * <p>这个长度用于确定字符数组的大小，符合C字符串的约定。
     * 
     * @return 字符串长度加1（null终止符）
     */
    public int getLength() {
        return processedLiteral.length() + 1; // +1 for null terminator
    }
    
    /**
     * 计算字符串对应的数组类型
     * 
     * <p>字符串在LLVM IR中表示为i8类型的数组，数组长度包括null终止符。
     * 
     * @param literal 字符串字面量
     * @return 对应的数组类型 [length x i8]
     */
    private static ArrayType calculateArrayType(String literal) {
        String processed = processEscapeSequences(literal);
        int length = processed.length() + 1; // +1 for null terminator
        return new ArrayType(IntegerType.I8, length);
    }
    
    /**
     * 处理字符串中的转义字符序列
     * 
     * <p>目前支持的转义字符：
     * <ul>
     *   <li>{@code \n} - 换行符</li>
     * </ul>
     * 
     * <p>未来可以扩展支持更多转义字符，如：
     * <ul>
     *   <li>{@code \t} - 制表符</li>
     *   <li>{@code \r} - 回车符</li>
     *   <li>{@code \\} - 反斜杠</li>
     *   <li>{@code \"} - 双引号</li>
     * </ul>
     * 
     * @param input 包含转义字符的原始字符串
     * @return 处理后的字符串，转义字符已转换为实际字符
     */
    private static String processEscapeSequences(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == 'n') {
                    result.append('\n');
                    i++; // 跳过下一个字符
                } else {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * 将字符串转换为LLVM IR的字符串表示格式
     * 
     * <p>LLVM IR字符串格式规则：
     * <ul>
     *   <li>以 {@code c"} 开头，以 {@code "} 结尾</li>
     *   <li>特殊字符用十六进制转义：{@code \0A}（换行）、{@code \09}（制表符）</li>
     *   <li>可打印ASCII字符直接显示</li>
     *   <li>自动添加 {@code \00} 作为null终止符</li>
     * </ul>
     * 
     * <p>转换示例：
     * <ul>
     *   <li>{@code "hello"} → {@code c"hello\00"}</li>
     *   <li>{@code "hello\nworld"} → {@code c"hello\0Aworld\00"}</li>
     *   <li>{@code "tab:\t"} → {@code c"tab:\09\00"}</li>
     * </ul>
     * 
     * @return LLVM IR格式的字符串表示
     */
    public String toLLVMString() {
        StringBuilder builder = new StringBuilder();
        builder.append("c\"");
        
        for (char c : processedLiteral.toCharArray()) {
            if (c == '\n') {
                builder.append("\\0A");
            } else if (c == '\t') {
                builder.append("\\09");
            } else if (c == '\r') {
                builder.append("\\0D");
            } else if (c == '\\') {
                builder.append("\\\\");
            } else if (c == '"') {
                builder.append("\\\"");
            } else if (c >= 32 && c <= 126) {
                // 可打印ASCII字符
                builder.append(c);
            } else {
                // 其他字符用十六进制表示
                builder.append(String.format("\\%02X", (int) c));
            }
        }
        
        builder.append("\\00\""); // null terminator
        return builder.toString();
    }
    
    /**
     * 生成字符串字面量的完整LLVM IR声明
     * 
     * <p>格式：{@code @name = private unnamed_addr constant [length x i8] <content>, align 1}
     * 
     * <p>生成示例：
     * <ul>
     *   <li>{@code @str.0 = private unnamed_addr constant [6 x i8] c"hello\00", align 1}</li>
     *   <li>{@code @str.1 = private unnamed_addr constant [13 x i8] c"hello\0Aworld\00", align 1}</li>
     * </ul>
     * 
     * <p>属性说明：
     * <ul>
     *   <li>{@code private} - 私有链接，避免符号冲突</li>
     *   <li>{@code unnamed_addr} - 允许合并相同内容的字符串</li>
     *   <li>{@code constant} - 只读常量</li>
     *   <li>{@code align 1} - 按字节对齐</li>
     * </ul>
     * 
     * @return 字符串字面量的完整LLVM IR声明
     */
    @Override
    public String toString() {
        ArrayType arrayType = (ArrayType) ((PointerType) getType()).getPointeeType();
        return getName() + " = constant " +
               arrayType.toString() + " " + toLLVMString();
    }
}