package middle.llvm;

import middle.llvm.type.*;
import middle.llvm.value.*;
import middle.llvm.value.constant.*;
import middle.llvm.value.instruction.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLVM IR指令和值的创建工厂
 * 
 * <p>提供统一的接口来创建各种LLVM IR指令和值对象，负责：
 * <ul>
 *   <li>维护全局的命名计数器，确保唯一性</li>
 *   <li>管理当前的函数和基本块上下文</li>
 *   <li>提供类型安全的指令创建方法</li>
 *   <li>处理常见的类型转换和优化</li>
 *   <li>维护字符串字面量池，避免重复</li>
 * </ul>
 * 
 * <p>工厂模式的优势：
 * <ul>
 *   <li>集中管理对象创建逻辑</li>
 *   <li>确保对象创建的一致性</li>
 *   <li>简化复杂对象的构造过程</li>
 *   <li>提供统一的错误处理机制</li>
 *   <li>支持对象创建的优化和缓存</li>
 * </ul>
 * 
 * <p>使用模式：
 * <pre>
 * IRInstructionFactory factory = new IRInstructionFactory();
 * 
 * // 设置当前上下文
 * factory.setCurrentFunction(function);
 * factory.setCurrentBasicBlock(block);
 * 
 * // 创建指令
 * AllocaInstruction alloca = factory.createAlloca(IntegerType.I32);
 * LoadInstruction load = factory.createLoad(alloca);
 * StoreInstruction store = factory.createStore(value, alloca);
 * </pre>
 * 
 * @see IRInstruction 指令基类
 * @see IRValue 值基类
 * @see IRModule 模块管理器
 */
public class IRInstructionFactory {
    
    /**
     * 关联的IR模块，工厂直接创建和管理
     */
    protected final IRModule module = new IRModule();
    
    /**
     * 全局名称计数器，用于生成唯一的临时变量名
     */
    private int nameCounter;

    /**
     * 字符串字面量计数器
     */
    private int stringLiteralCounter;
    
    /**
     * Phi指令计数器
     */
    private int phiCounter;
    
    /**
     * 当前正在构建的函数
     */
    protected IRFunction currentFunction;
    
    /**
     * 当前正在构建的基本块
     */
    protected IRBasicBlock currentBasicBlock;
    
    /**
     * 字符串字面量缓存池，避免重复创建相同的字符串
     */
    private final Map<String, IRStringLiteral> stringLiteralPool;

    /**
     * 构造函数，初始化工厂实例
     * 采用参考项目设计模式，直接创建模块实例
     */
    public IRInstructionFactory() {
        this.nameCounter = 0;
        this.stringLiteralCounter = 0;
        this.phiCounter = 0;
        this.currentFunction = null;
        this.currentBasicBlock = null;
        this.stringLiteralPool = new HashMap<>();
    }
    
    /**
     * 设置当前函数上下文
     * 
     * @param function 当前函数
     */
    public void setCurrentFunction(IRFunction function) {
        this.currentFunction = function;
    }
    
    /**
     * 设置当前基本块上下文
     * 
     * @param basicBlock 当前基本块
     */
    public void setCurrentBasicBlock(IRBasicBlock basicBlock) {
        this.currentBasicBlock = basicBlock;
    }
    
    /**
     * 获取下一个唯一的名称计数器值
     * 
     * @return 名称计数器值
     */
    public int getNextNameCounter() {
        return nameCounter++;
    }
    
    /**
     * 获取下一个Phi指令计数器值
     * 
     * @return Phi计数器值
     */
    public int getNextPhiCounter() {
        return phiCounter++;
    }
    
    // ==================== 全局变量创建方法 ====================
    
    /**
     * 创建全局变量
     * 
     * @param name 变量名称
     * @param initializer 初始值
     * @param isConstant 是否为常量
     * @return 创建的全局变量
     */
    public IRGlobalVariable createGlobalVariable(String name, IRConstant initializer, boolean isConstant) {
        IRGlobalVariable globalVar = new IRGlobalVariable(name, initializer, isConstant);
        module.registerGlobalVariable(globalVar);
        return globalVar;
    }

    private int staticVarCnt = 0;
    public IRStaticVariable createStaticVariable(String name, IRConstant initializer) {
        String nameForRegister = currentFunction.getName() + "." + name + "." + staticVarCnt++;
        IRStaticVariable staticVar = new IRStaticVariable(nameForRegister, initializer);
        module.registerStaticVariable(staticVar);
        return staticVar;
    }
    
    // ==================== 函数创建方法 ====================
    
    /**
     * 创建函数
     * 
     * @param functionName 函数名称
     * @param returnType 返回类型
     * @param parameterTypes 参数类型列表
     * @return 创建的函数
     */
    public IRFunction createFunction(String functionName, IntegerType returnType, ArrayList<IntegerType> parameterTypes) {
        IRFunction function = new IRFunction(functionName, returnType, parameterTypes);
        module.registerFunction(function);
        return function;
    }
    
    /**
     * 创建基本块
     * 
     * @return 创建的基本块
     */
    public IRBasicBlock createBasicBlock() {
        if (currentFunction == null) {
            throw new IllegalStateException("创建基本块时必须设置当前函数上下文");
        }
        
        IRBasicBlock basicBlock = new IRBasicBlock(currentFunction, getNextNameCounter());
        currentFunction.addBasicBlock(basicBlock);
        return basicBlock;
    }
    
    // ==================== 内存操作指令创建方法 ====================
    
    /**
     * 创建内存分配指令（无初始值）
     * 
     * @param allocatedType 分配的类型
     * @return 创建的alloca指令
     */
    public AllocaInstruction createAlloca(IRType allocatedType) {
        if (currentFunction == null) {
            throw new IllegalStateException("创建alloca指令时必须设置当前函数上下文");
        }
        
        AllocaInstruction alloca = new AllocaInstruction(currentFunction.getEntryBlock(), getNextNameCounter(), allocatedType);
        // alloca指令通常添加到函数入口块的开头
        currentFunction.getEntryBlock().addInstructionToHead(alloca);
        return alloca;
    }
    
    /**
     * 创建内存分配指令（带初始值）
     * 
     * @param allocatedType 分配的类型
     * @param initialValue 初始值
     * @return 创建的alloca指令
     */
    public AllocaInstruction createAlloca(IRType allocatedType, IRConstant initialValue) {
        if (currentFunction == null) {
            throw new IllegalStateException("创建alloca指令时必须设置当前函数上下文");
        }
        
        AllocaInstruction alloca = new AllocaInstruction(currentFunction.getEntryBlock(), getNextNameCounter(), allocatedType, initialValue);
        currentFunction.getEntryBlock().addInstructionToHead(alloca);
        return alloca;
    }
    
    /**
     * 创建加载指令
     * 
     * @param pointer 要加载的指针
     * @return 创建的load指令
     */
    public LoadInstruction createLoad(IRValue pointer) {
        validateCurrentBasicBlock();
        
        LoadInstruction load = new LoadInstruction(currentBasicBlock, getNextNameCounter(), pointer);
        currentBasicBlock.addInstructionToTail(load);
        return load;
    }
    
    /**
     * 创建存储指令
     * 
     * @param value 要存储的值
     * @param pointer 存储目标指针
     * @return 创建的store指令
     */
    public StoreInstruction createStore(IRValue value, IRValue pointer) {
        validateCurrentBasicBlock();
        
        StoreInstruction store = new StoreInstruction(currentBasicBlock, value, pointer);
        currentBasicBlock.addInstructionToTail(store);
        return store;
    }

    /**
     * getelementptr inbounds <类型>, <类型>* basePointer, i32 0, i32 0
     * "类型"是数组，"basePointer"是数组名，将数组类型的首地址转为指针
     *
     * @param basePointer 基础指针
     * @return 创建的GEP指令
     */
    public GetElementPtrInstruction createGetElementPtr(IRValue basePointer) {
        validateCurrentBasicBlock();

        // 确保索引是i32类型
        IRValue index = new IntegerConstant(IntegerType.I32,0);
        
        List<IRValue> indices = new ArrayList<>();
        indices.add(index);
        indices.add(index);

        GetElementPtrInstruction gep = new GetElementPtrInstruction(currentBasicBlock, getNextNameCounter(), basePointer, indices);
        currentBasicBlock.addInstructionToTail(gep);
        return gep;
    }

    /**
     * getelementptr inbounds <类型>, <类型>* basePointer, i32 index
     * "类型"是元素，"basePointer"是指针，求一个偏移index的指针
     * 
     * @param basePointer 基础指针
     * @param index 索引值
     * @return 创建的GEP指令
     */
    public GetElementPtrInstruction createGetElementPtr(IRValue basePointer, IRValue index) {
        validateCurrentBasicBlock();
        
        // 确保索引是i32类型
        IRValue adjustedIndex = ensureIntegerTypeValue(index, IntegerType.I32);
        
        GetElementPtrInstruction gep = new GetElementPtrInstruction(currentBasicBlock, getNextNameCounter(), basePointer, adjustedIndex);
        currentBasicBlock.addInstructionToTail(gep);
        return gep;
    }
    
    /**
     * getelementptr inbounds <类型>, <类型>* basePointer, i32 idx0, i32 idx1, ...
     * 通用GEP指令
     * 
     * @param basePointer 基础指针
     * @param indices 索引列表
     * @return 创建的GEP指令
     */
    public GetElementPtrInstruction createGetElementPtr(IRValue basePointer, List<IRValue> indices) {
        validateCurrentBasicBlock();
        
        List<IRValue> adjustedIndices = new ArrayList<>();
        for (IRValue idx : indices) {
            adjustedIndices.add(ensureIntegerTypeValue(idx, IntegerType.I32));
        }
        
        GetElementPtrInstruction gep = new GetElementPtrInstruction(currentBasicBlock, getNextNameCounter(), 
                basePointer, adjustedIndices);
        currentBasicBlock.addInstructionToTail(gep);
        return gep;
    }
    
    // ==================== 算术和比较指令创建方法 ====================
    
    /**
     * 创建二元运算指令
     * 
     * @param operator 运算类型
     * @param leftOperand 左操作数
     * @param rightOperand 右操作数
     * @return 创建的二元运算指令
     */
    public BinaryOperationInstruction createBinaryOperation(BinaryOperationInstruction.BinaryOperator operator,
                                                            IRValue leftOperand, IRValue rightOperand) {
        validateCurrentBasicBlock();
        
        BinaryOperationInstruction binOp
                = new BinaryOperationInstruction(
                        currentBasicBlock,
                        operator,
                        getNextNameCounter(),
                        leftOperand, rightOperand);

        currentBasicBlock.addInstructionToTail(binOp);
        return binOp;
    }
    
    /**
     * 创建比较指令
     * 
     * @param condition 比较条件
     * @param leftOperand 左操作数
     * @param rightOperand 右操作数
     * @return 创建的比较指令
     */
    public CompareInstruction createCompare(CompareInstruction.CompareCondition condition,
                                          IRValue leftOperand, IRValue rightOperand) {
        validateCurrentBasicBlock();
        
        CompareInstruction cmp
                = new CompareInstruction(
                        currentBasicBlock,
                        condition,
                        getNextNameCounter(),
                        leftOperand, rightOperand);

        currentBasicBlock.addInstructionToTail(cmp);
        return cmp;
    }
    
    // ==================== 控制流指令创建方法 ====================
    
    /**
     * 创建无条件跳转指令
     * 
     * @param targetBlock 目标基本块
     * @return 创建的跳转指令
     */
    public JumpInstruction createJump(IRBasicBlock targetBlock) {
        validateCurrentBasicBlock();
        
        JumpInstruction jump = new JumpInstruction(currentBasicBlock, targetBlock);
        currentBasicBlock.addInstructionToTail(jump);
        return jump;
    }
    
    /**
     * 创建条件分支指令
     * 
     * @param condition 分支条件
     * @param trueBlock 条件为真时的目标块
     * @param falseBlock 条件为假时的目标块
     * @return 创建的分支指令
     */
    public BranchInstruction createBranch(IRValue condition, IRBasicBlock trueBlock, IRBasicBlock falseBlock) {
        validateCurrentBasicBlock();
        
        BranchInstruction branch = new BranchInstruction(currentBasicBlock, condition, trueBlock, falseBlock);
        currentBasicBlock.addInstructionToTail(branch);
        return branch;
    }
    
    /**
     * 创建返回指令（有返回值）
     * 
     * @param returnValue 返回值
     * @return 创建的返回指令
     */
    public ReturnInstruction createReturn(IRValue returnValue) {
        validateCurrentBasicBlock();
        
        ReturnInstruction ret = new ReturnInstruction(currentBasicBlock, returnValue);
        currentBasicBlock.addInstructionToTail(ret);
        return ret;
    }
    
    /**
     * 创建返回指令（无返回值）
     * 
     * @return 创建的返回指令
     */
    public ReturnInstruction createReturnVoid() {
        validateCurrentBasicBlock();
        
        ReturnInstruction ret = new ReturnInstruction(currentBasicBlock);
        currentBasicBlock.addInstructionToTail(ret);
        return ret;
    }
    
    // ==================== 函数调用指令创建方法 ====================
    
    /**
     * 创建函数调用指令（有返回值）
     * 
     * @param function 被调用的函数
     * @param arguments 参数列表
     * @return 创建的调用指令
     */
    public CallInstruction createCall(IRValue function, List<IRValue> arguments) {
        validateCurrentBasicBlock();
        
        CallInstruction call = new CallInstruction(currentBasicBlock, getNextNameCounter(), function, arguments);
        currentBasicBlock.addInstructionToTail(call);
        return call;
    }
    
    /**
     * 创建函数调用指令（无返回值）
     * 
     * @param function 被调用的函数
     * @param arguments 参数列表
     * @return 创建的调用指令
     */
    public CallInstruction createCallVoid(IRValue function, List<IRValue> arguments) {
        validateCurrentBasicBlock();
        
        CallInstruction call = new CallInstruction(currentBasicBlock, function, arguments);
        currentBasicBlock.addInstructionToTail(call);
        return call;
    }
    
    // ==================== 类型转换指令创建方法 ====================
    
    /**
     * 创建零扩展指令
     * 
     * @param value 要扩展的值
     * @param targetType 目标类型
     * @return 创建的零扩展指令
     */
    public ZeroExtendInstruction createZeroExtend(IRValue value, IntegerType targetType) {
        validateCurrentBasicBlock();
        
        ZeroExtendInstruction zext = new ZeroExtendInstruction(currentBasicBlock, getNextNameCounter(), value, targetType);
        currentBasicBlock.addInstructionToTail(zext);
        return zext;
    }
    
    /**
     * 创建截断指令
     * 
     * @param value 要截断的值
     * @param targetType 目标类型
     * @return 创建的截断指令
     */
    public TruncateInstruction createTruncate(IRValue value, IntegerType targetType) {
        validateCurrentBasicBlock();
        
        TruncateInstruction trunc = new TruncateInstruction(currentBasicBlock, getNextNameCounter(), value, targetType);
        currentBasicBlock.addInstructionToTail(trunc);
        return trunc;
    }
    
    // ==================== 字符串字面量创建方法 ====================
    
    /**
     * 创建或获取字符串字面量
     * 
     * @param literal 字符串内容
     * @return 字符串字面量对象
     */
    public IRStringLiteral createStringLiteral(String literal) {
        // 检查缓存池中是否已存在相同的字符串
        IRStringLiteral existing = stringLiteralPool.get(literal);
        if (existing != null) {
            return existing;
        }
        
        // 创建新的字符串字面量
        IRStringLiteral stringLiteral = new IRStringLiteral(stringLiteralCounter++, literal);
        stringLiteralPool.put(literal, stringLiteral);
        module.registerStringLiteral(stringLiteral);
        
        return stringLiteral;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 验证当前基本块上下文是否有效
     * 
     * @throws IllegalStateException 如果当前基本块为null
     */
    private void validateCurrentBasicBlock() {
        if (currentBasicBlock == null) {
            throw new IllegalStateException("创建指令时必须设置当前基本块上下文");
        }
    }
    
    /**
     * 确保值为指定的整数类型，必要时插入类型转换指令
     * 
     * @param value 原始值
     * @param targetType 目标整数类型
     * @return 需要转换 ? 转换指令 : 原值
     */
    protected IRValue ensureIntegerTypeValue(IRValue value, IntegerType targetType) {
        if (!(value.getType() instanceof IntegerType)) {
            throw new IllegalArgumentException("值必须是整数类型");
        }
        
        IntegerType valueType = (IntegerType) value.getType();
        if (valueType.equals(targetType)) {
            return value;  // 不需要转换，返回原值
        }
        
        // 需要类型转换
        if (valueType.getBitWidth() < targetType.getBitWidth()) {
            // 零扩展
            return createZeroExtend(value, targetType);
        } else {
            // 截断
            return createTruncate(value, targetType);
        }
    }

    /**
     * 确保值为指定的整数类型，必要时插入类型转换指令
     * 
     * @param value 原始值
     * @param targetType 目标整数类型
     * @return 需要转换 ? 转换指令 : null
     */
    protected IRInstruction ensureIntegerType(IRValue value, IntegerType targetType) {
        if (!(value.getType() instanceof IntegerType)) {
            throw new IllegalArgumentException("值必须是整数类型");
        }
        
        IntegerType valueType = (IntegerType) value.getType();
        if (valueType.equals(targetType)) {
            return null;
        }
        
        // 需要类型转换
        if (valueType.getBitWidth() < targetType.getBitWidth()) {
            // 零扩展
            return createZeroExtend(value, targetType);
        } else {
            // 截断
            return createTruncate(value, targetType);
        }
    }
    
    /**
     * 获取关联的IR模块
     * 
     * @return IR模块
     */
    public IRModule getModule() {
        return module;
    }
    
    /**
     * 获取当前函数
     * 
     * @return 当前函数，可能为null
     */
    public IRFunction getCurrentFunction() {
        return currentFunction;
    }
    
    /**
     * 获取当前基本块
     * 
     * @return 当前基本块，可能为null
     */
    public IRBasicBlock getCurrentBasicBlock() {
        return currentBasicBlock;
    }
}