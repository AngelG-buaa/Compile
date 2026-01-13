package middle.llvm.type;

/**
 * LLVM IR数组类型实现
 * 
 * 表示LLVM IR中的数组类型，用于表示固定长度的同类型元素序列。
 * 
 * 在LLVM IR中的表示形式：
 * - [10 x i32]: 包含10个32位整数的数组
 * - [256 x i8]: 包含256个字节的数组（通常用于字符串）
 * - [5 x [3 x i32]]: 5x3的二维整数数组
 * 
 * 对应SysY语言中的数组声明：
 * - int arr[10];        -> [10 x i32]
 * - char str[256];      -> [256 x i8]
 * - int matrix[5][3];   -> [5 x [3 x i32]]
 * 
 * 数组在内存中连续存储，可以通过getelementptr指令进行索引访问：
 * %ptr = getelementptr [10 x i32], [10 x i32]* %arr, i32 0, i32 %index
 */
public class ArrayType extends IRType {
    
    /**
     * 数组元素的类型
     * 支持IntegerType或ArrayType（多维数组）
     */
    private final IRType elementType;
    
    /**
     * 数组的长度（元素个数）
     */
    private final int arrayLength;
    
    /**
     * 构造数组类型
     * 
     * @param elementType 数组元素的类型
     * @param arrayLength 数组长度
     */
    public ArrayType(IRType elementType, int arrayLength) {
        this.elementType = elementType;
        this.arrayLength = arrayLength;
    }
    
    /**
     * 获取数组元素类型
     * 
     * @return 元素类型
     */
    public IRType getElementType() {
        return elementType;
    }
    
    /**
     * 获取数组长度
     * 
     * @return 数组长度
     */
    public int getArrayLenth() {
        return arrayLength;
    }
    
    @Override
    public boolean isArrayType() {
        return true;
    }
    
    /**
     * 判断是否为字符数组
     * 
     * 字符数组通常用于表示字符串：
     * - [256 x i8]: 最大长度为255的字符串（加上'\0'结尾）
     * 
     * @return 如果元素类型是i8返回true
     */
    public boolean isCharArray() {
        return elementType instanceof IntegerType && ((IntegerType)elementType).isByteType();
    }
    
    @Override
    public int getByteSize() {
        // 数组总字节数 = 元素大小 × 数组长度
        return elementType.getByteSize() * arrayLength;
    }
    
    @Override
    public int getAlignment() {
        // 数组的对齐要求等于元素类型的对齐要求
        return elementType.getAlignment();
    }
    
    /**
     * 返回LLVM IR格式的数组类型字符串
     * 
     * @return 格式为 "[length x element_type]" 的字符串
     *         例如: "[10 x i32]", "[256 x i8]"
     */
    @Override
    public String toString() {
        return "[" + arrayLength + " x " + elementType.toString() + "]";
    }
}