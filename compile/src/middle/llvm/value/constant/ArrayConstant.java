package middle.llvm.value.constant;

import middle.llvm.type.ArrayType;
import middle.llvm.type.IRType;
import middle.llvm.type.IntegerType;

import java.util.ArrayList;
import java.util.List;

/**
 * LLVM IR数组常量实现
 * 
 * 表示LLVM IR中的数组常量值，主要用于全局数组的初始化。
 * 数组常量包含一系列相同类型的元素值，在编译时确定。
 * 
 * 在LLVM IR中的表示形式：
 * - [3 x i32] [i32 1, i32 2, i32 3]           ; 完全初始化的数组
 * - [5 x i32] [i32 1, i32 2, i32 0, i32 0, i32 0]  ; 部分初始化，其余为0
 * - [10 x i32] zeroinitializer                ; 全零初始化
 * - [4 x i8] c"abc\00"                        ; 字符串常量（以null结尾）
 * 
 * 使用场景：
 * - 全局数组初始化：@global_array = global [3 x i32] [i32 1, i32 2, i32 3]
 * - 字符串字面量：@str = private constant [6 x i8] c"hello\00"
 * - 查找表：@lookup = constant [4 x i32] [i32 10, i32 20, i32 30, i32 40]
 * 
 * 对应SysY语言中的数组初始化：
 * - int arr[3] = {1, 2, 3};        -> [3 x i32] [i32 1, i32 2, i32 3]
 * - int arr[5] = {1, 2};           -> [5 x i32] [i32 1, i32 2, i32 0, i32 0, i32 0]
 * - int arr[10] = {0};             -> [10 x i32] zeroinitializer
 * - char str[] = "hello";          -> [6 x i8] c"hello\00"
 * 
 * 数组常量的特性：
 * - 编译时确定：所有元素值在编译时已知
 * - 自动填充：未指定的元素自动填充为0
 * - 类型一致：所有元素必须是相同类型
 * - 内存布局：元素在内存中连续存储
 */
public class ArrayConstant extends IRConstant {
    
    /**
     * 数组元素值列表
     * 存储数组中每个元素的整数值
     */
    private final List<Integer> elementValues;
    
    /**
     * 构造数组常量（指定初始值）
     * 
     * 如果提供的初始值少于数组长度，剩余元素自动填充为0
     * 
     * @param type 数组类型
     * @param elementValues 初始元素值列表
     */
    public ArrayConstant(ArrayType type, List<Integer> elementValues) {
        super(type);
        this.elementValues = new ArrayList<>(elementValues);
        
        // 数组常量用于全局数组初始化，元素个数可能少于数组长度，需要用0填充
        int requiredLength = type.getArrayLenth();
        int currentLength = this.elementValues.size();
        for (int i = 0; i < requiredLength - currentLength; i++) {
            this.elementValues.add(0);
        }
    }
    
    /**
     * 构造全零数组常量
     * 
     * 创建一个所有元素都为0的数组常量，
     * 在LLVM IR中可以表示为zeroinitializer
     * 
     * @param type 数组类型
     */
    public ArrayConstant(ArrayType type) {
        super(type);
        this.elementValues = new ArrayList<>();
        int requiredLength = type.getArrayLenth();
        for (int i = 0; i < requiredLength; i++) {
            this.elementValues.add(0);
        }
    }
    
    /**
     * 获取数组长度
     * 
     * @return 数组元素个数
     */
    public int getArrayLenth() {
        return elementValues.size();
    }
    
    /**
     * 获取元素值列表
     * 
     * @return 所有元素值的副本
     */
    public List<Integer> getElementValues() {
        return new ArrayList<>(elementValues);
    }
    
    /**
     * 判断是否为全零数组
     * 
     * 全零数组在LLVM IR中可以优化为zeroinitializer，
     * 节省存储空间和提高加载效率
     * 
     * @return 如果所有元素都为0返回true
     */
    @Override
    public boolean isZeroValue() {
        return elementValues.stream().allMatch(value -> value == 0);
    }
    
    /**
     * 判断是否包含字符类型
     * 
     * 字符数组用于字符串处理，元素类型为i8
     * 
     * @return 如果数组元素类型为i8返回true
     */
    @Override
    public boolean containsCharacterType() {
        return ((ArrayType) getType()).getElementType() == IntegerType.I8;
    }
    
    /**
     * 获取常量的所有数值
     * 
     * 返回数组中所有元素的值
     * 
     * @return 所有元素值的列表
     */
    @Override
    public List<Integer> getAllNumbers() {
        return new ArrayList<>(elementValues);
    }
    
    /**
     * 获取指定位置的元素常量
     * 
     * 将数组元素包装为IntegerConstant对象，
     * 用于访问数组中的单个元素
     * 
     * @param index 元素索引
     * @return 对应位置的整数常量，索引越界返回null
     */
    public IntegerConstant getElementConstant(int index) {
        if (index >= 0 && index < elementValues.size()) {
            ArrayType arrayType = (ArrayType) getType();
            // 确保elementType是IntegerType，如果是ArrayType（多维数组），这里可能不适用，
            // 但目前的实现是展平的elementValues，所以可能是合理的，取决于elementType是否真的是基础类型。
            // 实际上ArrayType的构造函数允许elementType是IRType。
            // 如果elementType是IntegerType，可以强制转换。
            if (arrayType.getElementType() instanceof IntegerType) {
                return new IntegerConstant((IntegerType) arrayType.getElementType(), elementValues.get(index));
            } else {
                // 如果是多维数组，这里应该怎么做？
                // 目前elementValues是扁平化的整数列表。
                // 如果是多维数组，getElementType()返回的是子数组类型。
                // 那么我们无法创建一个IntegerConstant(ArrayType, val)。
                // 我们应该递归获取基础类型。
                IRType baseType = arrayType.getElementType();
                while (baseType instanceof ArrayType) {
                    baseType = ((ArrayType) baseType).getElementType();
                }
                if (baseType instanceof IntegerType) {
                    return new IntegerConstant((IntegerType) baseType, elementValues.get(index));
                }
            }
        }
        return null;
    }
    
    /**
     * 生成LLVM IR格式的数组常量字符串
     * 
     * 格式：
     * - 全零数组：zeroinitializer
     * - 普通数组：[elementType value1, elementType value2, ...]
     * 
     * 示例：
     * - [i32 1, i32 2, i32 3]
     * - [i8 65, i8 66, i8 67, i8 0]  ; "ABC\0"
     * 
     * @return LLVM IR格式的数组常量定义
     */
    @Override
    public String toString() {
        // 全零数组使用特殊表示
        if (isZeroValue()) {
            return "zeroinitializer";
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        
        ArrayType arrayType = (ArrayType) getType();
        // 修正：elementType可能是ArrayType，如果是多维数组
        IRType elementType = arrayType.getElementType();
        
        // 如果是多维数组，我们需要递归构建字符串，还是说elementValues是扁平的？
        // ArrayConstant的设计似乎是基于扁平的elementValues。
        // 但是LLVM IR中多维数组必须是嵌套的结构：[2 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [2 x i32] [i32 3, i32 4]]
        // 如果我们这里只输出扁平列表，那就是错的。
        
        // 必须根据类型结构重构数据。
        if (elementType instanceof ArrayType) {
            // 这是一个多维数组的层级
            ArrayType subArrayType = (ArrayType) elementType;
            int subLength = subArrayType.getArrayLenth(); // 第一维长度？不，是子元素的长度
            // arrayType = [M x SubType]
            // elementValues has M * sizeof(SubType) items.
            
            // 我们需要把 elementValues 切分成 M 个块，每个块用于构建一个子 ArrayConstant
            // 这是一个递归过程。
            // 但是我们并没有存储子 ArrayConstant 对象，只存储了扁平的 int list。
            // 所以我们需要动态生成子 ArrayConstant 的字符串表示。
            
            // 计算子元素（也是数组）包含的基础元素个数
            int subElementCount = 1;
            IRType temp = subArrayType;
            while (temp instanceof ArrayType) {
                subElementCount *= ((ArrayType) temp).getArrayLenth();
                temp = ((ArrayType) temp).getElementType();
            }
            
            int currentLength = arrayType.getArrayLenth();
            for (int i = 0; i < currentLength; i++) {
                // 提取子列表
                int start = i * subElementCount;
                int end = start + subElementCount;
                List<Integer> subList = elementValues.subList(start, end);
                
                // 递归创建子常量并获取字符串
                ArrayConstant subConst = new ArrayConstant(subArrayType, subList);
                builder.append(subArrayType.toString()).append(" ").append(subConst.toString());
                
                if (i < currentLength - 1) {
                    builder.append(", ");
                }
            }
        } else {
            // 基础类型数组 (i32, i8)
            for (int i = 0; i < elementValues.size(); i++) {
                builder.append(elementType.toString()).append(" ").append(elementValues.get(i));
                if (i < elementValues.size() - 1) {
                    builder.append(", ");
                }
            }
        }
        
        builder.append("]");
        return builder.toString();
    }
}