package middle.llvm;
// TODO:把isChar给加上

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.decl.BType;
import front.parser.syntax.decl.ConstDecl;
import front.parser.syntax.decl.ConstDef;
import front.parser.syntax.decl.ConstInitVal;
import front.parser.syntax.decl.Decl;
import front.parser.syntax.decl.InitVal;
import front.parser.syntax.decl.VarDecl;
import front.parser.syntax.decl.VarDef;
import front.parser.syntax.exp.AddExp;
import front.parser.syntax.exp.Cond;
import front.parser.syntax.exp.ConstExp;
import front.parser.syntax.exp.EqExp;
import front.parser.syntax.exp.Exp;
import front.parser.syntax.exp.FuncCallUnaryExp;
import front.parser.syntax.exp.FuncRParams;
import front.parser.syntax.exp.LAndExp;
import front.parser.syntax.exp.LOrExp;
import front.parser.syntax.exp.LVal;
import front.parser.syntax.exp.MulExp;
import front.parser.syntax.exp.PrimaryExp;
import front.parser.syntax.exp.PrimaryUnaryExp;
import front.parser.syntax.exp.RelExp;
import front.parser.syntax.exp.UnaryExp;
import front.parser.syntax.exp.UnaryOp;
import front.parser.syntax.func.FuncDef;
import front.parser.syntax.func.FuncFParam;
import front.parser.syntax.func.FuncFParams;
import front.parser.syntax.func.FuncType;
import front.parser.syntax.func.MainFuncDef;
import front.parser.syntax.stmt.AssignStmt;
import front.parser.syntax.stmt.Block;
import front.parser.syntax.stmt.BlockItem;
import front.parser.syntax.stmt.BlockStmt;
import front.parser.syntax.stmt.BreakStmt;
import front.parser.syntax.stmt.CaseStmt;
import front.parser.syntax.stmt.ContinueStmt;
import front.parser.syntax.stmt.ContinueStmt;
import front.parser.syntax.stmt.DecStmt;
import front.parser.syntax.stmt.DoWhileStmt;
import front.parser.syntax.stmt.ExpStmt;
import front.parser.syntax.stmt.ForLoopStmt;
import front.parser.syntax.stmt.ForStmt;
import front.parser.syntax.stmt.GotoStmt;
import front.parser.syntax.stmt.IfStmt;
import front.parser.syntax.stmt.IncStmt;
import front.parser.syntax.stmt.LabelStmt;
import front.parser.syntax.stmt.PrintStmt;
import front.parser.syntax.stmt.RepeatStmt;
import front.parser.syntax.stmt.ReturnStmt;
import front.parser.syntax.stmt.Stmt;
import front.parser.syntax.stmt.SwitchStmt;
import front.parser.syntax.stmt.WhileStmt;
import middle.llvm.IR_Symbol.IRSymTable;
import middle.llvm.type.ArrayType;
import middle.llvm.type.IRType;
import middle.llvm.type.IntegerType;
import middle.llvm.type.PointerType;
import middle.llvm.type.VoidType;
import middle.llvm.value.*;
import middle.llvm.value.constant.ArrayConstant;
import middle.llvm.value.constant.IRConstant;
import middle.llvm.value.constant.IntegerConstant;
import middle.llvm.value.instruction.*;

import java.util.*;

/**
 * <p>何为中间代码生成？
 * <ul>
 *   <li>为AST中的每个Node生成一组IRValue</li>
 *   <li>将这组IRValue插入到module的正确位置</li>
 * </ul>
 */
public class Visitor extends IRInstructionFactory {
    /**
     * 单例的Visitor
     */
    private static Visitor visitor;

    /**
     * 根符号表
     */
    private IRSymTable rootSymbolTable;

    /**
     * 当前符号表
     */
    private IRSymTable curSymbolTable;

    /**
     * 管理所有循环的栈
     */
    private final Deque<LoopStructure> loops;

    /**
     * 管理所有Switch的栈
     */
    private final Deque<SwitchStructure> switches;
    
    /**
     * 标签映射 (Label -> BasicBlock)
     * 每个函数重置一次
     */
    private Map<String, IRBasicBlock> labelMap;
    private ArrayList<GotoStmt> unresolvedGotos; // 处理前向引用

    /**
     * 控制流栈（LoopStructure 或 SwitchStructure）
     */
    private final Deque<Object> controlStack;

    /**
     * 是否加载左值（默认true）。如果为false，visitLVal返回指针而不是值。
     */
    private boolean loadLVal = true;

    /**
     * 编译时可求值则为true
     */
    private boolean culWhileCompiling = false;

    /**
     * 函数的传参为指针则为true
     */
    private boolean ptrParam = false;

    /**
     * Visitor实例生成
     * @return Visitor的唯一实例
     */
    public static Visitor getVisitor() {
        return visitor == null ? new Visitor() : visitor;
    }

    private Visitor() {
        super();
        this.rootSymbolTable = new IRSymTable(null);
        this.curSymbolTable = this.rootSymbolTable;
        this.loops = new ArrayDeque<>();
        this.switches = new ArrayDeque<>();
        this.controlStack = new ArrayDeque<>();
        this.labelMap = new HashMap<>();
        this.unresolvedGotos = new ArrayList<>();
    }






    /**
     * 访问AST节点的主入口
     * @param compUnit 编译单元根节点
     */
    public IRModule visit(BranchNode compUnit) {
        visitCompUnit(compUnit);
        return module;
    }

    /**
     * 访问编译单元
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     */
    public void visitCompUnit(BranchNode compUnit) {
        List<AstNode> children = compUnit.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.Decl) {
                visitDecl((Decl) child);
            } else if (child.getNodeType() == SynType.FuncDef) {
                visitFuncDef((BranchNode) child);
            } else if (child.getNodeType() == SynType.MainFuncDef) {
                visitMainFuncDef((BranchNode) child);
            }
        }
    }

    /**
     * 访问声明
     * Decl → ConstDecl | VarDecl
     */
    public void visitDecl(Decl decl) {
        if (decl.isVarDecl()) {
            visitVarDecl(decl.getVarDecl());
        } else {
            visitConstDecl(decl.getConstDecl());
        }
    }

    /**
     * 访问变量声明
     * VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
     */
    public void visitVarDecl(VarDecl varDecl) {
        List<AstNode> children = varDecl.getChildren();
        TokenNode bType = varDecl.getBType().getType();
        boolean isStatic = varDecl.isStatic();

        for (AstNode child : children) {
            if (child.getNodeType() == SynType.VarDef) {
                visitVarDef((BranchNode) child, bType, isStatic);
            }
        }
    }

    /**
     * 访问变量定义
     * VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
     */
    public void visitVarDef(BranchNode varDef, TokenNode bType, boolean isStatic) {
        // 语法分析阶段记录的结构化信息
        TokenNode identifier = ((VarDef) varDef).getIdentifier();
        String varName = identifier.getContent();
        ArrayList<ConstExp> constExp = ((VarDef) varDef).getConstExps();
        InitVal initVal = ((VarDef) varDef).getInitValue();
        boolean isGetInt = ((VarDef) varDef).isGetintVariable();
        // initVal具体是什么
        ArrayList<IRValue> initVals;

        // 非数组的普通变量
        if (constExp.isEmpty()) {
            // 全局变量
            if (isGlobal()) {
                IRGlobalVariable globalVariable;
                // 不带初始值，自动补0
                if (initVal == null) {
                    IntegerConstant init
                            = new IntegerConstant(IntegerType.I32,0);
                    globalVariable = createGlobalVariable(varName,init,false);
                }
                // 带了初始值，用initVal
                else {
                    culWhileCompiling = true;
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, 1);
                    culWhileCompiling = false;

                    IntegerConstant init
                            = new IntegerConstant(
                            getIntType(bType),
                            ((IntegerConstant) initVals.get(0)).getConstantValue());
                    globalVariable = createGlobalVariable(varName,init,false);
                }
                // 在符号表里存
                curSymbolTable.insertSymbol(varName,globalVariable);
            }
            // static静态局部变量
            else if (isStatic) {
                IRStaticVariable staticVariable;
                // 不带初始值，自动补0
                if (initVal == null) {
                    IntegerConstant init
                            = new IntegerConstant(IntegerType.I32,0);
                    staticVariable = createStaticVariable(varName,init);
                }
                // 带了初始值，用initVal
                else {
                    culWhileCompiling = true;
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, 1);
                    culWhileCompiling = false;

                    IntegerConstant init
                            = new IntegerConstant(
                            getIntType(bType),
                            ((IntegerConstant) initVals.get(0)).getConstantValue());
                    staticVariable = createStaticVariable(varName,init);
                }
                curSymbolTable.insertSymbol(varName,staticVariable);
            }
            // getint的变量
            else if (isGetInt) {
                AllocaInstruction alloc = createAlloca(getIntType(bType));
                curSymbolTable.insertSymbol(varName,alloc);

                ArrayList<IRValue> params = new ArrayList<>();
                CallInstruction getIntCall = createCall(IRFunction.GETINT, params);

                IRInstruction resized = ensureIntegerType(getIntCall, getIntType(bType));
                createStore(resized == null ? getIntCall : resized, alloc);
            }
            // 普通非静态局部变量
            else {
                AllocaInstruction alloc = createAlloca(getIntType(bType));
                curSymbolTable.insertSymbol(varName,alloc);

                // 有初值
                if (initVal != null) {
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, 1);
                    IRValue oriInstruction = initVals.get(0);
                    // 处理位数不匹配
                    IRInstruction newInstruction = ensureIntegerType(initVals.get(0),getIntType(bType));
                    if (newInstruction == null) {
                        createStore(oriInstruction, alloc);
                    } else {
                        createStore(newInstruction, alloc);
                    }
                }
            }
        }
        // 数组变量定义
        else {
            // 计算数组类型（支持多维）
            IRType currentType = getIntType(bType);
            // 从右向左构建ArrayType (int a[2][3] -> [2 x [3 x i32]])
            for (int i = constExp.size() - 1; i >= 0; i--) {
                int dim = visitConstExp(constExp.get(i));
                currentType = new ArrayType(currentType, dim);
            }
            ArrayType arrayType = (ArrayType) currentType;
            
            // 全局数组
            if (isGlobal()) {
                IRGlobalVariable globalVariable;
                // 不带初始值，自动补0
                if (initVal == null) {
                    IRConstant initializer = new ArrayConstant(arrayType);
                    globalVariable = createGlobalVariable(varName,initializer,false);
                    curSymbolTable.insertSymbol(varName,globalVariable);
                }
                // 有初始值
                else {
                    culWhileCompiling = true;
                    // 计算总元素个数用于扁平化初始化
                    int totalSize = 1;
                    IRType temp = arrayType;
                    while (temp instanceof ArrayType) {
                        totalSize *= ((ArrayType) temp).getArrayLenth();
                        temp = ((ArrayType) temp).getElementType();
                    }
                    
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, totalSize);
                    culWhileCompiling = false;

                    ArrayList<Integer> initInts = new ArrayList<>();
                    if (initVals != null) {
                        for (IRValue value : initVals) {
                            IntegerConstant constInt = (IntegerConstant) value;
                            initInts.add(constInt.getConstantValue());
                        }
                    }

                    ArrayConstant constArray = new ArrayConstant(arrayType, initInts);
                    globalVariable = createGlobalVariable(varName,constArray,false);

                    curSymbolTable.insertSymbol(varName,globalVariable);
                }
            }
            // static静态局部变量
            else if (isStatic) {
                IRStaticVariable staticVariable;
                // 不带初始值，自动补0
                if (initVal == null) {
                    IRConstant initializer = new ArrayConstant(arrayType);
                    staticVariable = createStaticVariable(varName,initializer);
                    curSymbolTable.insertSymbol(varName,staticVariable);
                }
                // 有初始值
                else {
                    culWhileCompiling = true;
                    // 计算总元素个数
                    int totalSize = 1;
                    IRType temp = arrayType;
                    while (temp instanceof ArrayType) {
                        totalSize *= ((ArrayType) temp).getArrayLenth();
                        temp = ((ArrayType) temp).getElementType();
                    }
                    
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, totalSize);
                    culWhileCompiling = false;

                    ArrayList<Integer> initInts = new ArrayList<>();
                    if (initVals != null) {
                        for (IRValue value : initVals) {
                            IntegerConstant constInt = (IntegerConstant) value;
                            initInts.add(constInt.getConstantValue());
                        }
                    }

                    ArrayConstant constArray = new ArrayConstant(arrayType, initInts);
                    staticVariable = createStaticVariable(varName,constArray);

                    curSymbolTable.insertSymbol(varName,staticVariable);
                }
            }
            // 普通非静态局部数组
            else {
                AllocaInstruction alloc = createAlloca(arrayType);
                curSymbolTable.insertSymbol(varName,alloc);
                // 处理初始值
                if (initVal != null) {
                    // 计算总元素个数
                    int totalSize = 1;
                    IRType temp = arrayType;
                    while (temp instanceof ArrayType) {
                        totalSize *= ((ArrayType) temp).getArrayLenth();
                        temp = ((ArrayType) temp).getElementType();
                    }
                    
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, totalSize);
                    // GEP获取一个int*
                    // 对于多维数组，我们需要解引用到最底层元素
                    // alloc 是 [n x [m x i32]]*
                    // 我们需要 GEP 0, 0, 0 ... 得到 i32*
                    
                    ArrayList<IRValue> indices = new ArrayList<>();
                    indices.add(new IntegerConstant(IntegerType.I32, 0)); // dereference pointer
                    IRType tempType = arrayType;
                    while (tempType instanceof ArrayType) {
                        indices.add(new IntegerConstant(IntegerType.I32, 0)); // index into array
                        tempType = ((ArrayType) tempType).getElementType();
                    }
                    
                    GetElementPtrInstruction basePtr = createGetElementPtr(alloc, indices);
                    
                    // 处理位数不匹配
                    IRInstruction instruction = ensureIntegerType(initVals.get(0),getIntType(bType));
                    if (instruction != null) {
                        createStore(instruction,basePtr);
                    } else {
                        createStore(initVals.get(0),basePtr);
                    }
                    // 处理后续元素
                    GetElementPtrInstruction ptr = null;
                    for (int i = 1; i < initVals.size(); i ++) {
                        ptr = createGetElementPtr(basePtr,new IntegerConstant(IntegerType.I32,i));
                        // 处理位数不匹配
                        instruction = ensureIntegerType(initVals.get(i),getIntType(bType));
                        if (instruction != null) {
                            createStore(instruction,ptr);
                        } else {
                            createStore(initVals.get(i),ptr);
                        }
                    }
                }
            }
        }
    }

    /**
     * InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
     */
    /**
     * 辅助方法：生成指定类型的零值列表
     */
    private ArrayList<IRValue> getZeroValues(IRType type) {
        ArrayList<IRValue> res = new ArrayList<>();
        if (type.isBasicIntegerType()) {
            res.add(new IntegerConstant((IntegerType) type, 0));
        } else if (type.isArrayType()) {
            ArrayType arrType = (ArrayType) type;
            int total = arrType.getArrayLenth();
            ArrayList<IRValue> subZeros = getZeroValues(arrType.getElementType());
            for (int i = 0; i < total; i++) {
                res.addAll(subZeros);
            }
        }
        return res;
    }

    public ArrayList<IRValue> visitInitVal(InitVal initVal, boolean isChar, int length) {
        // Wrapper for compatibility or specific 1D array/scalar usage
        IRType eleType = isChar ? IntegerType.I8 : IntegerType.I32;
        IRType type = length == 1 ? eleType : new ArrayType(eleType, length);
        
        return visitInitVal(initVal, type);
    }
    
    public ArrayList<IRValue> visitInitVal(InitVal initVal, IRType type) {
        ArrayList<IRValue> res = new ArrayList<>();
        
        if (type.isBasicIntegerType()) { // isIntegerType -> isBasicIntegerType or isI
            if (initVal.isLeaf()) {
                // 表达式初始化
                res.add(visitExp(initVal.getExp()));
            } else {
                // '{' Exp '}' 形式
                if (!initVal.getInitVals().isEmpty()) {
                    res.addAll(visitInitVal(initVal.getInitVals().get(0), type));
                }
            }
        } else if (type.isArrayType()) {
            ArrayType arrType = (ArrayType) type;
            int eleNum = arrType.getArrayLenth(); // getElementNum -> getArrayLenth
            IRType eleType = arrType.getElementType();
            
            if (initVal.isLeaf()) {
                // 不应该发生，除非是字符串字面量初始化 char[] (TODO)
            } else {
                ArrayList<InitVal> children = initVal.getInitVals();
                int childIdx = 0;
                for (int i = 0; i < eleNum; i++) {
                    if (childIdx < children.size()) {
                        res.addAll(visitInitVal(children.get(childIdx++), eleType));
                    } else {
                        res.addAll(getZeroValues(eleType));
                    }
                }
            }
        }
        return res;
    }

    /**
     * 辅助方法：生成指定类型的零值列表（Integer）
     */
    private ArrayList<Integer> getZeroInts(IRType type) {
        ArrayList<Integer> res = new ArrayList<>();
        if (type.isBasicIntegerType()) { // isIntegerType -> isBasicIntegerType
            res.add(0);
        } else if (type.isArrayType()) {
            ArrayType arrType = (ArrayType) type;
            int total = arrType.getArrayLenth(); // getElementNum -> getArrayLenth
            ArrayList<Integer> subZeros = getZeroInts(arrType.getElementType());
            for (int i = 0; i < total; i++) {
                res.addAll(subZeros);
            }
        }
        return res;
    }

    public ArrayList<Integer> visitConstInitVal(ConstInitVal initVal, boolean isChar, int length) {
        IRType eleType = isChar ? IntegerType.I8 : IntegerType.I32;
        IRType type = length == 1 ? eleType : new ArrayType(eleType, length);
        return visitConstInitVal(initVal, type);
    }
    
    public ArrayList<Integer> visitConstInitVal(ConstInitVal initVal, IRType type) {
        ArrayList<Integer> res = new ArrayList<>();
        
        if (type.isBasicIntegerType()) { // isIntegerType -> isBasicIntegerType
            if (initVal.isLeaf()) {
                res.add(visitConstExp(initVal.getConstExp()));
            } else {
                if (!initVal.getInitVals().isEmpty()) {
                    res.addAll(visitConstInitVal(initVal.getInitVals().get(0), type));
                }
            }
        } else if (type.isArrayType()) {
            ArrayType arrType = (ArrayType) type;
            int eleNum = arrType.getArrayLenth(); // getElementNum -> getArrayLenth
            IRType eleType = arrType.getElementType();
            
            if (!initVal.isLeaf()) {
                ArrayList<ConstInitVal> children = initVal.getInitVals();
                int childIdx = 0;
                for (int i = 0; i < eleNum; i++) {
                    if (childIdx < children.size()) {
                        res.addAll(visitConstInitVal(children.get(childIdx++), eleType));
                    } else {
                        res.addAll(getZeroInts(eleType));
                    }
                }
            }
        }
        return res;
    }

    /**
     * ConstExp → AddExp
     */
    public int visitConstExp(ConstExp constExp) {
        culWhileCompiling = true;
        IntegerConstant res = (IntegerConstant) visitAddExp(constExp.getAddExp());
        culWhileCompiling = false;
        return res.getConstantValue();
    }

    /**
     * 访问常量声明
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     */
    public void visitConstDecl(BranchNode constDecl) {
        BType bType = ((ConstDecl)constDecl).getType();
        ArrayList<ConstDef> constDefs = ((ConstDecl)constDecl).getConstDefs();

        for (ConstDef constDef : constDefs) {
            visitConstDef(constDef,bType);
        }
    }

    /**
     * 访问常量定义
     * ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
     */
    public void visitConstDef(BranchNode constDef, BType bType) {
        TokenNode ident = ((ConstDef)constDef).getIdentifier();
        ArrayList<ConstExp> constExps = ((ConstDef)constDef).getConstExps();
        ConstInitVal initVal = ((ConstDef) constDef).getInitValue();

        // 非数组的普通定义
        if (constExps.isEmpty()) {
            IntegerType type = getIntType(bType.getType());
            ArrayList<Integer> initInts = visitConstInitVal(initVal, type);
            int val = initInts.isEmpty() ? 0 : initInts.get(0);
            IntegerConstant constInits = new IntegerConstant(type, val);

            curSymbolTable.insertSymbol(ident.getContent(),constInits);
        }
        // 数组
        else {
            // 计算数组类型
            IRType currentType = getIntType(bType.getType());
            for (int i = constExps.size() - 1; i >= 0; i--) {
                int dim = visitConstExp(constExps.get(i));
                currentType = new ArrayType(currentType, dim);
            }
            ArrayType arrayType = (ArrayType) currentType;
            
            ArrayList<Integer> initInts = visitConstInitVal(initVal, arrayType);
            ArrayConstant constArray = new ArrayConstant(arrayType, initInts);

            if (isGlobal()) {
                // 全局数组无需alloca，直接初始化
                IRGlobalVariable globalVariable = createGlobalVariable(ident.getContent(),constArray,true);
                curSymbolTable.insertSymbol(ident.getContent(),globalVariable);
            } else {
                // 局部数组
                AllocaInstruction alloc = createAlloca(arrayType,constArray);
                curSymbolTable.insertSymbol(ident.getContent(),alloc);
                // GEP得到int*指针
                GetElementPtrInstruction basePtr = createGetElementPtr(alloc);
                // 存constArray的首元素
                createStore(constArray.getElementConstant(0),basePtr);
                // 存constArray的后续元素
                GetElementPtrInstruction ptr = null;
                for (int i = 1; i < initInts.size(); i ++) {
                    ptr = createGetElementPtr(basePtr,new IntegerConstant(IntegerType.I32,i));
                    createStore(constArray.getElementConstant(i),ptr);
                }
            }
        }
    }

    /**
     * 访问函数定义
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     */
    public void visitFuncDef(BranchNode funcDef) {
        labelMap.clear();
        FuncType funcType = ((FuncDef)funcDef).getFuncType();
        TokenNode indent = ((FuncDef)funcDef).getIdentifier();
        FuncFParams fParams = ((FuncDef)funcDef).getParams();
        Block block = ((FuncDef)funcDef).getBody();

        // 创建Function，插入符号表
        IntegerType returnType = getIntType(funcType.getType());
        ArrayList<IntegerType> paramTypes = new ArrayList<>();
        ArrayList<FuncFParam> params;
        if (fParams != null) {
            params = fParams.getParams();
            for (FuncFParam param : params) {
                IntegerType baseType = getIntType(param.getBaseType());
                if (param.isArray()) {
                    paramTypes.add(new PointerType(baseType));
                } else {
                    paramTypes.add(baseType);
                }
            }
        }
        currentFunction = createFunction(indent.getContent(),returnType,paramTypes);
        curSymbolTable.insertSymbol(indent.getContent(),currentFunction);

        /**
         * ; 有返回值函数 - 展现所有三个特性
         * define dso_local i32 @hasReturn(i32 %a0) {
         * entry:
         *     ; === 特性1: entryBlock中全部是先alloca，再store ===
         *     %v0 = alloca i32                    ; 为形参x分配空间
         *     %v1 = alloca i32                    ; 为局部变量a分配空间
         *     %v2 = alloca i32                    ; 为局部变量b分配空间
         *     store i32 %a0, i32* %v0            ; 存储形参x的值
         *     store i32 10, i32* %v1             ; 存储a的初值10
         *     store i32 20, i32* %v2             ; 存储b的初值20
         *
         *     ; === 特性2: 分析block产生多个basicBlock，产生br指令时新建basicBlock ===
         *     %v3 = load i32, i32* %v0           ; 加载x的值
         *     %v4 = icmp sgt i32 %v3, 0          ; 比较 x > 0
         *     br i1 %v4, label %if_true, label %if_false  ; 条件跳转，产生新的基本块
         *
         * if_true:                               ; 新建的基本块
         *     %v5 = load i32, i32* %v1           ; 加载a
         *     %v6 = load i32, i32* %v2           ; 加载b
         *     %v7 = add i32 %v5, %v6             ; a + b
         *     store i32 %v7, i32* %v1            ; 存储到a
         *     %v8 = load i32, i32* %v1           ; 加载a的值
         *     ret i32 %v8                        ; 返回a - 有返回值函数必有ret
         *
         * if_false:                              ; 新建的基本块
         *     %v9 = load i32, i32* %v2           ; 加载b
         *     %v10 = mul i32 %v9, 2              ; b * 2
         *     store i32 %v10, i32* %v2           ; 存储到b
         *     %v11 = load i32, i32* %v2          ; 加载b的值
         *     ret i32 %v11                       ; 返回b - 有返回值函数必有ret
         * }
         *
         * ; 无返回值函数 - 展现人工补ret的特性
         * define dso_local void @noReturn(i32 %a0) {
         * entry:
         *     ; === 特性1: entryBlock中全部是先alloca，再store ===
         *     %v0 = alloca i32                    ; 为形参y分配空间
         *     %v1 = alloca i32                    ; 为局部变量c分配空间
         *     %v2 = alloca i32                    ; 为局部变量d分配空间
         *     store i32 %a0, i32* %v0            ; 存储形参y的值
         *     store i32 5, i32* %v1              ; 存储c的初值5
         *     store i32 15, i32* %v2             ; 存储d的初值15
         *
         *     ; === 特性2: 分析block产生多个basicBlock，产生br指令时新建basicBlock ===
         *     %v3 = load i32, i32* %v0           ; 加载y的值
         *     %v4 = icmp sgt i32 %v3, 10         ; 比较 y > 10
         *     br i1 %v4, label %if_true, label %if_false  ; 条件跳转，产生新的基本块
         *
         * if_true:                               ; 新建的基本块
         *     %v5 = load i32, i32* %v1           ; 加载c
         *     %v6 = load i32, i32* %v2           ; 加载d
         *     %v7 = add i32 %v5, %v6             ; c + d
         *     store i32 %v7, i32* %v1            ; 存储到c
         *     br label %end                       ; 跳转到结束块
         *
         * if_false:                              ; 新建的基本块
         *     %v8 = load i32, i32* %v2           ; 加载d
         *     %v9 = load i32, i32* %v1           ; 加载c
         *     %v10 = sub i32 %v8, %v9            ; d - c
         *     store i32 %v10, i32* %v2           ; 存储到d
         *     br label %end                       ; 跳转到结束块
         *
         * end:                                   ; 汇聚块
         *     ; === 特性3: 无返回值函数可能需要人工补ret ===
         *     ret void                           ; 人工补充的ret void指令
         * }
         */
        // currentFunction的符号表
        enter();
        // entryBlock应该是先alloca再store
        currentBasicBlock = createBasicBlock();
        visitFuncFParams(fParams);
        // visitBlock，产生0个或多个basicBlock，当产生br时新建basicBlock
        visitBlock(block);
        // void函数人工补ret
        if (currentFunction.getReturnType() instanceof VoidType) {
            IRInstruction lastInst = currentBasicBlock.getLastInstruction();
            // 如果lastInst是jump，后面加也没有用
            if (lastInst == null || !(lastInst instanceof BranchInstruction || lastInst instanceof ReturnInstruction)) {
                createReturnVoid();
            }
        }
        // 退出currentFunction的符号表
        leave();
    }

    /**
     * 访问主函数定义
     * MainFuncDef → 'int' 'main' '(' ')' Block
     */
    public void visitMainFuncDef(BranchNode mainFuncDef) {
        Block mainBody = ((MainFuncDef)mainFuncDef).getBody();
        ArrayList<IntegerType> paramTypes = new ArrayList<>();
        currentFunction = createFunction("main",IntegerType.I32,paramTypes);
        curSymbolTable.insertSymbol("main",currentFunction);
        enter();
        currentBasicBlock = createBasicBlock();
        visitBlock(mainBody);
        leave();
    }

    /**
     * Block → '{' { BlockItem } '}'
     */
    public void visitBlock(Block block) {
        for (BlockItem blockItem : block.getBlockItems()) {
            visitBlockItem(blockItem);
        }
    }

    /**
     * BlockItem → Decl | Stmt
     */
    public void visitBlockItem(BlockItem blockItem) {
        if (blockItem.isDecl()) {
            visitDecl(blockItem.getDecl());
        }
        else {
            visitStmt(blockItem.getStmt());
        }
    }

    /**
     * SwitchStmt → 'switch' '(' Exp ')' Stmt
     */
    public void visitSwitchStmt(SwitchStmt stmt) {
        // 1. 计算switch的条件表达式
        IRValue cond = visitExp(stmt.getExp());
        
        // 2. 准备关键基本块
        IRBasicBlock startBlock = currentBasicBlock; // switch语句开始前的块，用于后续插入分发逻辑
        IRBasicBlock exitBlock = createBasicBlock(); // switch语句结束后的出口块
        SwitchStructure switchStruct = new SwitchStructure(startBlock, exitBlock);
        
        // 3. 维护控制流栈，以便break语句能找到跳转目标
        switches.push(switchStruct);
        controlStack.push(switchStruct);
        
        // 4. 创建一个“死代码”占位块 (entryDummy)
        // 作用：switch中第一个case之前的代码是不可达的（死代码）。
        // 我们将当前块切换到这个占位块，让这些死代码生成在其中。
        // 注意：我们不会生成跳转到entryDummy的指令，因此它在运行时永远不会被执行。
        IRBasicBlock entryDummy = createBasicBlock();
        currentBasicBlock = entryDummy;
        
        // 5. 访问switch体（会包含多个CaseStmt）
        // 在访问CaseStmt时，会不断更新currentBasicBlock到新的case块
        if (stmt.getStmt() != null) {
            visitStmt(stmt.getStmt());
        }
        
        // 6. 处理最后一个case的fall-through
        // 如果最后一个case没有break（即没有终结指令），需要跳转到出口块
        if (!currentBasicBlock.isTerminated()) {
            createJump(exitBlock);
        }
        
        // 7. 回填分发逻辑 (Dispatch Logic)
        // 将currentBasicBlock切回到startBlock，在其中插入级联比较指令
        currentBasicBlock = startBlock;
        
        IRBasicBlock defaultBlock = switchStruct.getDefaultBlock();
        
        // 遍历所有case，生成 if-else if 风格的级联比较
        for (Map.Entry<Integer, IRBasicBlock> entry : switchStruct.getCases().entrySet()) {
            int val = entry.getKey(); // case的值
            IRBasicBlock target = entry.getValue(); // case对应的跳转目标块
            IRBasicBlock nextCheck = createBasicBlock(); // 下一次比较的块
            
            // 生成比较指令: %cmp = icmp eq %cond, val
            IRInstruction cmp = createCompare(CompareInstruction.CompareCondition.EQ, cond, new IntegerConstant(IntegerType.I32, val));
            // 生成条件跳转: br %cmp, label %target, label %nextCheck
            createBranch(cmp, target, nextCheck);
            
            // 切换到下一个比较块继续生成
            currentBasicBlock = nextCheck;
        }
        
        // 8. 处理default分支
        // 如果没有匹配任何case，跳转到default块（如果没有default，则跳到exitBlock）
        // 注意：SwitchStructure在没有default时，getDefaultBlock()通常返回exitBlock
        createJump(defaultBlock);
        
        // 9. 恢复上下文
        currentBasicBlock = exitBlock; // switch语句结束后，控制流停在exitBlock
        switches.pop();
        controlStack.pop();
    }

    /**
     * CaseStmt → 'case' ConstExp ':' Stmt | 'default' ':' Stmt
     */
    public void visitCaseStmt(CaseStmt stmt) {
        SwitchStructure curSwitch = switches.peek();
        IRBasicBlock caseBlock = createBasicBlock(); // 为当前case创建一个新的基本块
        
        // 1. 处理Fall-through（贯穿）
        // 检查上一个块是否已经结束（例如是否有break）。
        // 如果没结束，说明上一个case需要贯穿执行到当前case，因此插入跳转指令。
        if (!currentBasicBlock.isTerminated()) {
            createJump(caseBlock);
        }
        
        // 2. 切换到当前case块
        currentBasicBlock = caseBlock;
        
        // 3. 注册case信息到SwitchStructure，供后续生成分发逻辑使用
        if (stmt.isDefault()) {
            curSwitch.setDefaultBlock(caseBlock);
        } else {
            int val = visitConstExp(stmt.getConstExp());
            curSwitch.addCase(val, caseBlock);
        }
        
        // 4. 访问case内部的语句
        if (stmt.getStmt() != null) {
            visitStmt(stmt.getStmt());
        }
    }

    /**
     * DoWhileStmt → 'do' Stmt 'while' '(' Cond ')' ';'
     */
    public void visitDoWhileStmt(DoWhileStmt stmt) {
        IRBasicBlock bodyBlock = createBasicBlock();
        IRBasicBlock condBlock = createBasicBlock();
        IRBasicBlock exitBlock = createBasicBlock();
        
        LoopStructure loop = new LoopStructure(currentBasicBlock, condBlock, bodyBlock, condBlock, exitBlock);
        loops.push(loop);
        controlStack.push(loop);
        
        createJump(bodyBlock);
        
        currentBasicBlock = bodyBlock;
        visitStmt(stmt.getStmt());
        createJump(condBlock);
        
        currentBasicBlock = condBlock;
        visitCond(stmt.getCond(), bodyBlock, exitBlock);
        
        currentBasicBlock = exitBlock;
        loops.pop();
        controlStack.pop();
    }

    /**
     * 'repeat' Stmt 'until' '(' Cond ')' ';'
     */
    public void visitRepeatStmt(RepeatStmt stmt) {
        IRBasicBlock bodyBlock = createBasicBlock();
        IRBasicBlock condBlock = createBasicBlock();
        IRBasicBlock exitBlock = createBasicBlock();

        LoopStructure loop = new LoopStructure(currentBasicBlock, condBlock, bodyBlock, condBlock, exitBlock);
        loops.push(loop);
        controlStack.push(loop);

        createJump(bodyBlock);

        currentBasicBlock = bodyBlock;
        visitStmt(stmt.getStmt());
        createJump(condBlock);

        currentBasicBlock = condBlock;
        visitCond(stmt.getCond(), exitBlock, bodyBlock);

        currentBasicBlock = exitBlock;
        loops.pop();
        controlStack.pop();
    }

    /**
     * GotoStmt → 'goto' Ident ';'
     */
    public void visitGotoStmt(GotoStmt stmt) {
        String label = stmt.getIdentifier().getContent();
        IRBasicBlock target = labelMap.get(label);
        if (target == null) {
            target = createBasicBlock(); // Use createBasicBlock instead of new IRBasicBlock
            labelMap.put(label, target);
        }
        createJump(target);
        currentBasicBlock = createBasicBlock(); // Use createBasicBlock instead of new IRBasicBlock
    }

    /**
     * LabelStmt → Ident ':' Stmt
     */
    public void visitLabelStmt(LabelStmt stmt) {
        String label = stmt.getIdentifier().getContent();
        IRBasicBlock block = labelMap.get(label);
        if (block == null) {
            block = createBasicBlock();
            labelMap.put(label, block);
        } else {
            // 调整顺序：将已创建的块移动到当前位置
            currentFunction.getBasicBlocks().remove(block);
            currentFunction.getBasicBlocks().add(block);
        }
        
        if (!currentBasicBlock.isTerminated()) {
            createJump(block);
        }
        currentBasicBlock = block;
        if (stmt.getStmt() != null) {
            visitStmt(stmt.getStmt());
        }
    }
    
    // 需要在 visitFuncDef 中重置 labelMap
    // ... 在 visitFuncDef 开头添加 labelMap.clear();

    /**
     * Stmt → LVal '=' Exp ';'
     * | [Exp] ';'
     * | Block
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * | 'break' ';'
     * | 'continue' ';'
     * | 'return' [Exp] ';'
     * | 'printf''('StringConst {','Exp}')'';'
     * | 'switch' '(' Exp ')' Stmt
     * | 'case' ConstExp ':' Stmt
     * | 'default' ':' Stmt
     * | 'do' Stmt 'while' '(' Cond ')' ';'
     * | 'goto' Ident ';'
     * | Ident ':' Stmt
     */
    public void visitStmt(Stmt stmt) {
        List<AstNode> children = stmt.getChildren();

        if (stmt instanceof AssignStmt) {
            visitAssignStmt((AssignStmt) stmt);
        } else if (stmt instanceof IfStmt) {
            visitIfStmt((IfStmt) stmt);
        } else if (stmt instanceof ReturnStmt) {
            visitReturnStmt((ReturnStmt) stmt);
        } else if (stmt instanceof BlockStmt) {
            visitBlockStmt((BlockStmt) stmt);
        } else if (stmt instanceof ForLoopStmt) {
            visitForLoopStmt((ForLoopStmt) stmt);
        } else if (stmt instanceof BreakStmt) {
            visitBreakStmt((BreakStmt) stmt);
        } else if (stmt instanceof ContinueStmt) {
            visitContinueStmt((ContinueStmt) stmt);
        } else if (stmt instanceof PrintStmt) {
            visitPrintfStmt((PrintStmt) stmt);
        } else if (stmt instanceof ExpStmt) {
            visitExpStmt((ExpStmt) stmt);
        } else if (stmt instanceof IncStmt) {
            visitIncStmt((IncStmt) stmt);
        } else if (stmt instanceof DecStmt) {
            visitDecStmt((DecStmt) stmt);
        } else if (stmt instanceof SwitchStmt) {
            visitSwitchStmt((SwitchStmt) stmt);
        } else if (stmt instanceof CaseStmt) {
            visitCaseStmt((CaseStmt) stmt);
        } else if (stmt instanceof DoWhileStmt) {
            visitDoWhileStmt((DoWhileStmt) stmt);
        } else if (stmt instanceof WhileStmt) {
            visitWhileStmt((WhileStmt) stmt);
        } else if (stmt instanceof GotoStmt) {
            visitGotoStmt((GotoStmt) stmt);
        } else if (stmt instanceof LabelStmt) {
            visitLabelStmt((LabelStmt) stmt);
        } else if (stmt instanceof RepeatStmt) {
            visitRepeatStmt((RepeatStmt) stmt);
        }
    }

    private void visitIncStmt(IncStmt stmt) {
        IRValue ptr = visitLVal(stmt.getIdentifier());
        if (!(ptr.getType() instanceof PointerType)) {
            return;
        }
        IRValue oldValue = createLoad(ptr); // !!!!!
        IRInstruction resizeInst = ensureIntegerType(oldValue, IntegerType.I32);
        oldValue = resizeInst == null ? oldValue : resizeInst;
        IRValue newValue = createBinaryOperation(
                BinaryOperationInstruction.BinaryOperator.ADD,
                oldValue,
                new IntegerConstant(IntegerType.I32, 1));
        IRType pointeeType = ((PointerType) ptr.getType()).getPointeeType();
        if (pointeeType instanceof IntegerType) {
            IRInstruction cast = ensureIntegerType(newValue, (IntegerType) pointeeType);
            createStore(cast == null ? newValue : cast, ptr);
        } else {
            createStore(newValue, ptr);
        }
    }

    private void visitDecStmt(DecStmt stmt) {
        IRValue ptr = visitLVal(stmt.getIdentifier());
        if (!(ptr.getType() instanceof PointerType)) {
            return;
        }
        IRValue oldValue = createLoad(ptr);
        IRInstruction resizeInst = ensureIntegerType(oldValue, IntegerType.I32);
        oldValue = resizeInst == null ? oldValue : resizeInst;
        IRValue newValue = createBinaryOperation(
                BinaryOperationInstruction.BinaryOperator.SUB,
                oldValue,
                new IntegerConstant(IntegerType.I32, 1));
        IRType pointeeType = ((PointerType) ptr.getType()).getPointeeType();
        if (pointeeType instanceof IntegerType) {
            IRInstruction cast = ensureIntegerType(newValue, (IntegerType) pointeeType);
            createStore(cast == null ? newValue : cast, ptr);
        } else {
            createStore(newValue, ptr);
        }
    }

    /**
     * LVal '=' Exp ';'
     * 用visit子节点得到的IRValue来store
     */
    private void visitAssignStmt(AssignStmt stmt) {
        IRValue LVal = visitLVal(stmt.getLVal());
        IRValue Exp = visitExp(stmt.getExp());

        // 处理位数不匹配
        IRInstruction exp = ensureIntegerType(Exp,(IntegerType) ((PointerType) LVal.getType()).getPointeeType());

        if (exp != null) {
            createStore(exp,LVal);
        } else {
            createStore(Exp,LVal);
        }
    }

    /**
     * [Exp] ';'
     */
    private void visitExpStmt(ExpStmt stmt) {
        Exp content = stmt.getExp();
        if (content != null) {
            visitExp(content);
        }
    }

    /**
     * Block,重点在于要进入到子符号表中
     */
    private void visitBlockStmt(BlockStmt stmt) {
        Block block = stmt.getBlock();
        enter();
        visitBlock(block);
        leave();
    }

    /**
     * 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     *
     */
    private void visitIfStmt(IfStmt stmt) {
        Cond cond = stmt.getCondition();
        Stmt ifStmt = stmt.getIfStmt();
        Stmt elseStmt = stmt.getElseStmt();

        /**
         * 没有else
         * ; 前置块中，visitCond(cond, successBb, failureBb) 生成条件跳转
         * ; %cond 的计算由 visitCond 内部完成
         * br i1 %cond, label %bb_if, label %bb_end
         *
         * bb_if:
         *   ; visitStmt(ifStmt) 在此生成 if 分支体的指令
         *   ; ...
         *   br label %bb_end
         *
         * bb_end:
         *   ; curBb = endBb，后续语句在此继续
         *   ; ...
         */
        if (elseStmt == null) {
            IRBasicBlock bb_if = createBasicBlock();
            IRBasicBlock bb_end = createBasicBlock();

            visitCond(cond,bb_if,bb_end);

            // 把当前 IR 写入位置切换到 if 分支对应的基本块
                /*
                如果 if 分支体是一个 Block，
                作用域是在 visitStmt(ifStmt) 的分派中由 visitBlockStmt 内部，
                调用 enter()/leave() 自动管理的；
                如果分支体不是 Block，则本就不应该新建作用域
                 */
            currentBasicBlock = bb_if;
            visitStmt(ifStmt);
            createJump(bb_end);

            currentBasicBlock = bb_end;
        }
        /**
         * 有else
         * ; 前置块中，visitCond(cond, successBb, failureBb) 生成条件跳转
         * ; %cond 的计算由 visitCond 内部完成，这里只展示控制流
         * br i1 %cond, label %bb_if, label %bb_else
         *
         * bb_if:
         *   ; visitStmt(ifStmt) 在此生成 if 分支体的指令
         *   ; ...
         *   br label %bb_end
         *
         * bb_else:
         *   ; visitStmt(elStmt) 在此生成 else 分支体的指令
         *   ; ...
         *   br label %bb_end
         *
         * bb_end:
         *   ; curBb = endBb，后续语句在此继续
         *   ; ...
         */
        else {
            IRBasicBlock bb_if = createBasicBlock();
            IRBasicBlock bb_else = createBasicBlock();
            IRBasicBlock bb_end = createBasicBlock();

            visitCond(cond,bb_if,bb_else);

            currentBasicBlock = bb_if;
            visitStmt(ifStmt);
            createJump(bb_end);

            currentBasicBlock = bb_else;
            visitStmt(elseStmt);
            createJump(bb_end);

            currentBasicBlock = bb_end;
        }
    }

    /**
     * WhileStmt → 'while' '(' Cond ')' Stmt
     */
    private void visitWhileStmt(WhileStmt stmt) {
        // 1. 获取条件表达式和循环体
        Cond cond = stmt.getCond();
        Stmt bodyStmt = stmt.getStmt();

        // 2. 创建关键基本块
        // condBlock: 条件判断块
        // bodyBlock: 循环体块
        // exitBlock: 循环结束后的出口块
        IRBasicBlock condBlock = createBasicBlock();
        IRBasicBlock bodyBlock = createBasicBlock();
        IRBasicBlock exitBlock = createBasicBlock();

        // 3. 维护循环结构栈 (LoopStructure)
        // 用于处理 break (跳转到 exitBlock) 和 continue (跳转到 condBlock)
        // 注意：while循环的 updateBlock 就是 condBlock (因为没有专门的步进语句块)
        LoopStructure loop = new LoopStructure(currentBasicBlock, condBlock, bodyBlock, condBlock, exitBlock);
        loops.push(loop);
        controlStack.push(loop);

        // 4. 从当前块跳转到条件判断块
        createJump(condBlock);
        
        // 5. 生成条件判断逻辑
        // 在 condBlock 中计算条件，如果为真跳到 bodyBlock，否则跳到 exitBlock
        currentBasicBlock = condBlock;
        visitCond(cond, bodyBlock, exitBlock);

        // 6. 生成循环体逻辑
        currentBasicBlock = bodyBlock;
        visitStmt(bodyStmt);

        // 7.在循环体 bodyBlock 的末尾生成回跳指令 createJump 之前
        // 应该检查 currentBasicBlock.isTerminated()
        if (!currentBasicBlock.isTerminated()) {
            createJump(condBlock);
        }

        // 8. 恢复上下文
        // 循环结束后，控制流停在 exitBlock
        currentBasicBlock = exitBlock;
        loops.pop();
        controlStack.pop();
    }

    /**
     * 访问for语句
     * 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     *
     * ; 入口块（initBb）：对应 curBb，先执行可选的 initStmt（初始化），然后跳到 condBb
     * initBb:
     * ; -- initStmt IR（可能为空）：例如 i = 0 等
     * ;   %i.ptr = ...                 ; 变量指针（示例）
     * ;   store i32 0, i32* %i.ptr     ; 初始化（示例）
     * br label %condBb
     *
     * ; 条件块：根据 cond 决定进入 bodyBb 或跳到 endBb
     * condBb:
     * ; -- cond IR（可能为空）
     * ; 情况1：有 cond，例如 %cond = icmp slt i32 %i, %n
     * ;   %cond = icmp slt i32 %i_val, %n_val
     * ;   br i1 %cond, label %bodyBb, label %endBb
     * ;
     * ; 情况2：无 cond（visitForSubStmt 中使用 makeJump(bodyBb)）
     * br label %bodyBb
     *
     * ; 循环体块：执行 stmt 的主体语句，然后跳到 forwardBb
     * bodyBb:
     * ; -- stmt IR（循环体内容）：例如调用、赋值、打印等
     * ;   ...（根据具体语句生成）
     * br label %forwardBb
     *
     * ; 自增/推进块：执行 forwardStmt（如 i = i + 1），然后回到 condBb
     * forwardBb:
     * ; -- forwardStmt IR（可能为空）：例如 i = i + 1
     * ;   %i.cur = load i32, i32* %i.ptr
     * ;   %i.next = add i32 %i.cur, 1
     * ;   store i32 %i.next, i32* %i.ptr
     * br label %condBb
     *
     * ; 结束块：循环退出后的控制流在此汇合
     * endBb:
     * ; -- 后续语句或函数尾部（可能是下一条语句或 ret）
     * ;   ...（根据上下文生成）
     */
    private void visitForLoopStmt(ForLoopStmt stmt) {
        ForStmt initStmt = stmt.getInitStmt();
        Cond cond = stmt.getCond();
        ForStmt forwardStmt = stmt.getChangeStmt();
        Stmt bodyStmt = stmt.getBody();

        // 后面的create要用到Block，先都定义出来
        IRBasicBlock condBlock = createBasicBlock();
        IRBasicBlock forBody = createBasicBlock();
        IRBasicBlock exitBlock = createBasicBlock();
        IRBasicBlock updateBlock = createBasicBlock();
            /*
            创建一个 Loop 对象，保存了该 for 循环的关键基本块引用：
            curBb（初始化语句所在块，即 initBb）、
            condBb（条件判断）、
            forwardBb（步进/迭代）、
            bodyBb（循环体）、
            endBb（循环结束/跳出）。
            这样后续在生成 continue 或 break 时，
            就能准确跳到对应的块
            （continue -> 一般去 forwardBb；break -> endBb）
             */
        LoopStructure thisLoop = new LoopStructure(currentBasicBlock,condBlock,forBody,updateBlock,exitBlock);
        loops.push(thisLoop);
        controlStack.push(thisLoop);


        // initBb 的创建由visitForStmt完成
        if (initStmt != null) {
            visitForStmt(initStmt);
        }
        createJump(condBlock);

        // cond
        currentBasicBlock = condBlock; // 这就是"condBb:"
        if (cond == null) {
            createJump(forBody);
        } else {
            // 跳转指令由visitCond生成，可能跳到forBody或exitBlock
            visitCond(cond,forBody,exitBlock);
        }

        // update
        currentBasicBlock = updateBlock;
        if (forwardStmt != null) {
            visitForStmt(forwardStmt);
        }
        createJump(condBlock);

        // forBody
        currentBasicBlock = forBody;
        visitStmt(bodyStmt);
        createJump(updateBlock);

        // exitBlock
        currentBasicBlock = exitBlock;
        loops.pop();
        controlStack.pop();
    }

    /**
     * ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
     */
    private void visitForStmt(ForStmt forStmt) {
        for (int i = 0;i < forStmt.getLenth();i++) {
            IRValue LVal = visitLVal(forStmt.getLVals().get(i));
            IRValue Exp = visitExp(forStmt.getExps().get(i));

            // 处理位数不匹配
            IRInstruction exp = ensureIntegerType(Exp, (IntegerType) ((PointerType) LVal.getType()).getPointeeType());

            if (exp != null) {
                createStore(exp, LVal);
            } else {
                createStore(Exp, LVal);
            }
        }
    }

    /**
     * 'break' ';'
     */
    private void visitBreakStmt(BreakStmt stmt) {
        Object top = controlStack.peek();
        if (top instanceof LoopStructure) {
            LoopStructure curLoop = (LoopStructure) top;
            createBranch(
                    new IntegerConstant(IntegerType.I32,1)
                    ,curLoop.getExitBlock()
                    ,curLoop.getUpdateBlock());
        } else if (top instanceof SwitchStructure) {
            SwitchStructure curSwitch = (SwitchStructure) top;
            createJump(curSwitch.getExitBlock());
        }
        currentBasicBlock = new IRBasicBlock(null,200000);
    }

    /**
     * 'continue' ';'
     */
    private void visitContinueStmt(ContinueStmt stmt) {
        Iterator<Object> it = controlStack.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof LoopStructure) {
                LoopStructure curLoop = (LoopStructure) obj;
                createBranch(
                        new IntegerConstant(IntegerType.I32,1)
                        ,curLoop.getUpdateBlock()
                        ,curLoop.getExitBlock()
                );
                break;
            }
        }
        currentBasicBlock = new IRBasicBlock(null,200001);
    }

    /**
     * 'return' [Exp] ';'
     */
    private void visitReturnStmt(ReturnStmt stmt) {
        Exp retExp = stmt.getVal();
        /**
         * -------------------
         * int f(int a) { return a + 1; }
         * -------------------
         * ; entry/bodyBb
         * %v0 = load i32, i32* %a
         * %v1 = add i32 %v0, 1
         * ret i32 %v1
         * -------------------
         * -------------------
         * char g() { return 65; }
         * -------------------
         * ; entry/bodyBb
         * %v0 = trunc i32 65 to i8
         * ret i8 %v0
         */
        if (retExp != null) {
            IRValue expValue = visitExp(retExp);
            IRInstruction resizeInst = ensureIntegerType(expValue,(IntegerType) currentFunction.getReturnType());
            expValue = resizeInst == null ? expValue : resizeInst;
            createReturn(expValue);
        } else {
            createReturnVoid();
        }
        // 换到哨兵块防止“终结后追加指令”的非法 IR
        currentBasicBlock = new IRBasicBlock(null,200002);
    }

    /**
     * 'printf''('StringConst {','Exp}')'';'
     */
    private void visitPrintfStmt(PrintStmt stmt) {
        ArrayList<String> formatParts = stmt.splitFormat();
        ArrayList<Exp> exps = stmt.getExps();

        // 将ArrayList<Exp> exps转换成ArrayList<IRValue> values存起来
        // "hello %d world %c" 会得到 ["hello ", "%d", " world ", "%c"]
        ArrayList<IRValue> values = new ArrayList<>();
        for (int i = 0; i < exps.size(); i++) {
            IRValue value = visitExp(exps.get(i));
            // declare void @putint(i32)
            // declare void @putch(i32)
            // 不存在%s
            IRInstruction resizeInst = ensureIntegerType(value,IntegerType.I32);
            value = resizeInst == null ? value : resizeInst;
            values.add(value);
        }

        // 将printf变成对putchar、putint、putstr的call
        int valueIdx = 0;
        for (int i = 0; i < formatParts.size(); i++) {
            if (formatParts.get(i).equals("%c")) {
                IRValue value = values.get(valueIdx++);
                // putchar要求传入I8
                IRInstruction resizeInst = ensureIntegerType(value,IntegerType.I8);
                value = resizeInst == null ? value : resizeInst;
                // 创建传参列表
                ArrayList<IRValue> params = new ArrayList<>();
                params.add(value);
                // call
                createCallVoid(IRFunction.PUTCH,params);
            } else if (formatParts.get(i).equals("%d")) {
                IRValue value = values.get(valueIdx++);
                // putint要求传入I32
                IRInstruction resizeInst = ensureIntegerType(value,IntegerType.I32);
                value = resizeInst == null ? value : resizeInst;
                // 创建传参列表
                ArrayList<IRValue> params = new ArrayList<>();
                params.add(value);
                // call
                createCallVoid(IRFunction.PUTINT,params);
            } else {
                // 打印"hello"这种东西
                    /*
                    1. 创建或复用全局字符串字面量（字符串池），并注册到 module ，例如生成 @.strN = [len x i8] c"..." 。
                    2. 对全局字符串做 getelementptr ，索引 [0, 0] ，得到指向首字符的 i8* 指针（内部会把索引用 i32 统一）。
                    3. 以 i8* 指针作为唯一参数调用 putstr
                     */
                createStringLiteralAndPutstr(formatParts.get(i));
            }
        }

    }

    /**
     * Cond → LOrExp
     */
    private void visitCond(Cond cond,IRBasicBlock trueBlock,IRBasicBlock falseBlock) {
        visitLOrExp(cond.getLOrExp(),trueBlock,falseBlock);
    }

    /**
     * LOrExp → LAndExp | LOrExp '||' LAndExp
     *
     * C：
     * if (a || b) { ... } else { ... }
     * 转换为：
     * if (a) {
     *     // 直接跳转到true分支
     * } else {
     *     if (b) {
     *         // 跳转到true分支
     *     } else {
     *         // 跳转到false分支
     *     }
     * }
     */
    private void visitLOrExp(LOrExp exp, IRBasicBlock trueBlock, IRBasicBlock falseBlock) {
        if (exp.getLOrExp() == null) {
            // 基础情况：LOrExp → LAndExp
            // 只有一个LAndExp，直接处理
            visitLAndExp(exp.getLAndExp(), trueBlock, falseBlock);
        } else {
            // 递归情况：LOrExp → LOrExp '||' LAndExp
            // 左操作数：LOrExp，右操作数：LAndExp

            // 创建中间基本块，用于处理右操作数
            IRBasicBlock nextBlock = createBasicBlock();

            // 处理左操作数（LOrExp）
            // 如果左操作数为真，直接跳转到trueBlock
            // 如果左操作数为假，跳转到rightBlock继续计算右操作数
            visitLOrExp(exp.getLOrExp(), trueBlock, nextBlock);

            // 切换到右操作数的基本块
            currentBasicBlock = nextBlock;

            // 处理右操作数（LAndExp）
            // 如果右操作数为真，跳转到trueBlock
            // 如果右操作数为假，跳转到falseBlock
            visitLAndExp(exp.getLAndExp(), trueBlock, falseBlock);
        }
    }

    /**
     * LAndExp → EqExp | LAndExp '&&' EqExp
     *
     * 短路求值逻辑：
     * if (cond1 && cond2) { ... } else { ... }
     * 转换为：
     * if (cond1) {
     *     if (cond2) {
     *         // 跳转到true分支
     *     } else {
     *         // 跳转到false分支
     *     }
     * } else {
     *     // 跳转到false分支
     * }
     */
    private void visitLAndExp(LAndExp exp, IRBasicBlock trueBlock, IRBasicBlock falseBlock) {
        if (exp.getLAndExp() == null) {
            // 基础情况：LAndExp → EqExp
            // 只有一个EqExp，直接处理
            visitEqExp(exp.getEqExp(), trueBlock, falseBlock);
        } else {
            // 递归情况：LAndExp → LAndExp '&&' EqExp
            // 左操作数：LAndExp，右操作数：EqExp

            // 创建中间基本块，用于处理右操作数
            IRBasicBlock nextBlock = createBasicBlock();

            // 处理左操作数（LAndExp）
            // 如果左操作数为真，跳转到rightBlock继续计算右操作数
            // 如果左操作数为假，直接跳转到falseBlock
            visitLAndExp(exp.getLAndExp(), nextBlock, falseBlock);

            // 切换到右操作数的基本块
            currentBasicBlock = nextBlock;

            // 处理右操作数（EqExp）
            // 如果右操作数为真，跳转到trueBlock
            // 如果右操作数为假，跳转到falseBlock
            visitEqExp(exp.getEqExp(), trueBlock, falseBlock);
        }
    }

    /**
     * EqExp → RelExp | EqExp ('==' | '!=') RelExp
     */
    private void visitEqExp(EqExp exp, IRBasicBlock trueBlock, IRBasicBlock falseBlock) {
        if (exp.getEqExp() == null) {
            // 基础情况：EqExp → RelExp
            // 只有一个RelExp，直接处理
            visitRelExp(exp.getRelExp(), trueBlock, falseBlock);
        } else {
            // 递归情况：EqExp → EqExp ('==' | '!=') RelExp
            // 左操作数：EqExp，右操作数：RelExp

            // 计算左操作数的值
            IRValue leftValue = visitEqExpAsValue(exp.getEqExp());
            // 计算右操作数的值
            IRValue rightValue = visitRelExpAsValue(exp.getRelExp());

            // 确保操作数类型一致（转换为i32）
            IRInstruction leftInst = ensureIntegerType(leftValue, IntegerType.I32);
            leftValue = leftInst == null ? leftValue : leftInst;

            IRInstruction rightInst = ensureIntegerType(rightValue, IntegerType.I32);
            rightValue = rightInst == null ? rightValue : rightInst;

            // 根据操作符创建比较指令
            TokenNode operator = exp.getOperator();
            CompareInstruction.CompareCondition condition;

            if (operator.getNodeType() == SynType.EQL) {
                condition = CompareInstruction.CompareCondition.EQ;
            } else if (operator.getNodeType() == SynType.NEQ) {
                condition = CompareInstruction.CompareCondition.NE;
            } else {
                throw new IllegalArgumentException("未知的相等性操作符: " + operator.getNodeType());
            }

            CompareInstruction cmp = createCompare(condition, leftValue, rightValue);

            // 根据比较结果进行分支
            createBranch(cmp, trueBlock, falseBlock);
        }
    }

    /**
     * RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
     */
    private void visitRelExp(RelExp exp, IRBasicBlock trueBlock, IRBasicBlock falseBlock) {
        if (exp.getRelExp() == null) {
            // 基础情况：RelExp → AddExp
            // 只有一个AddExp，需要与0比较（处理 if(a) 的情况）
            IRValue value = visitAddExp(exp.getAddExp());

            // 确保类型为i32
            IRInstruction inst = ensureIntegerType(value, IntegerType.I32);
            value = inst == null ? value : inst;

            // 与0比较：value != 0
            CompareInstruction cmp = createCompare(CompareInstruction.CompareCondition.NE,
                    value, new IntegerConstant(IntegerType.I32, 0));

            // 根据比较结果进行分支
            createBranch(cmp, trueBlock, falseBlock);
        } else {
            // 递归情况：RelExp → RelExp ('<' | '>' | '<=' | '>=') AddExp
            // 左操作数：RelExp，右操作数：AddExp

            // 计算左操作数的值
            IRValue leftValue = visitRelExpAsValue(exp.getRelExp());
            // 计算右操作数的值
            IRValue rightValue = visitAddExp(exp.getAddExp());

            // 确保操作数类型一致（转换为i32）
            IRInstruction leftInst = ensureIntegerType(leftValue, IntegerType.I32);
            leftValue = leftInst == null ? leftValue : leftInst;

            IRInstruction rightInst = ensureIntegerType(rightValue, IntegerType.I32);
            rightValue = rightInst == null ? rightValue : rightInst;

            // 根据操作符创建比较指令
            CompareInstruction.CompareCondition condition = getCompareCondition(exp);

            CompareInstruction cmp = createCompare(condition, leftValue, rightValue);

            // 根据比较结果进行分支
            createBranch(cmp, trueBlock, falseBlock);
        }
    }

    /**
     * 计算EqExp的值（用于表达式求值，而非条件分支）
     */
    private IRValue visitEqExpAsValue(EqExp exp) {
        if (exp.getEqExp() == null) {
            // 基础情况：EqExp → RelExp
            return visitRelExpAsValue(exp.getRelExp());
        } else {
            // 递归情况：EqExp → EqExp ('==' | '!=') RelExp
            IRValue leftValue = visitEqExpAsValue(exp.getEqExp());
            IRValue rightValue = visitRelExpAsValue(exp.getRelExp());

            // 确保操作数类型一致
            IRInstruction leftInst = ensureIntegerType(leftValue, IntegerType.I32);
            leftValue = leftInst == null ? leftValue : leftInst;

            IRInstruction rightInst = ensureIntegerType(rightValue, IntegerType.I32);
            rightValue = rightInst == null ? rightValue : rightInst;

            // 根据操作符创建比较指令
            TokenNode operator = exp.getOperator();
            CompareInstruction.CompareCondition condition;

            if (operator.getNodeType() == SynType.EQL) {
                condition = CompareInstruction.CompareCondition.EQ;
            } else if (operator.getNodeType() == SynType.NEQ) {
                condition = CompareInstruction.CompareCondition.NE;
            } else {
                throw new IllegalArgumentException("未知的相等性操作符: " + operator.getNodeType());
            }

            return createCompare(condition, leftValue, rightValue);
        }
    }

    /**
     * 计算RelExp的值（用于表达式求值，而非条件分支）
     */
    private IRValue visitRelExpAsValue(RelExp exp) {
        if (exp.getRelExp() == null) {
            // 基础情况：RelExp → AddExp
            return visitAddExp(exp.getAddExp());
        } else {
            // 递归情况：RelExp → RelExp ('<' | '>' | '<=' | '>=') AddExp
            IRValue leftValue = visitRelExpAsValue(exp.getRelExp());
            IRValue rightValue = visitAddExp(exp.getAddExp());

            // 确保操作数类型一致
            IRInstruction leftInst = ensureIntegerType(leftValue, IntegerType.I32);
            leftValue = leftInst == null ? leftValue : leftInst;

            IRInstruction rightInst = ensureIntegerType(rightValue, IntegerType.I32);
            rightValue = rightInst == null ? rightValue : rightInst;

            // 根据操作符创建比较指令
            CompareInstruction.CompareCondition condition = getCompareCondition(exp);

            return createCompare(condition, leftValue, rightValue);
        }
    }

    private CompareInstruction.CompareCondition getCompareCondition(RelExp exp) {
        TokenNode operator = exp.getOperator();
        CompareInstruction.CompareCondition condition;

        if (operator.getNodeType() == SynType.LSS) {
            condition = CompareInstruction.CompareCondition.SLT;
        } else if (operator.getNodeType() == SynType.LEQ) {
            condition = CompareInstruction.CompareCondition.SLE;
        } else if (operator.getNodeType() == SynType.GRE) {
            condition = CompareInstruction.CompareCondition.SGT;
        } else if (operator.getNodeType() == SynType.GEQ) {
            condition = CompareInstruction.CompareCondition.SGE;
        } else {
            throw new IllegalArgumentException("未知的关系操作符: " + operator.getNodeType());
        }
        return condition;
    }


    /**
     * 表达式求值入口
     * 文法层级：Exp -> AddExp -> MulExp -> UnaryExp
     */
    public IRValue visitExp(Exp exp) {
        AstNode child = exp.getChildren().get(0);
        // 根据子节点类型分发到对应的处理方法
        if (child instanceof AddExp) {
            return visitAddExp((AddExp) child);
        }
        return null;
    }

    /**
     * 访问加减表达式
     * AddExp → MulExp | AddExp ('+' | '-') MulExp
     */
    public IRValue visitAddExp(AddExp addExp) {
        if (culWhileCompiling) {
            return new IntegerConstant(
                    IntegerType.I32,
                    calcAddExp(addExp));
        } else {
            return tackleAddExp(addExp);
        }
    }

    /**
     * 编译时计算加减表达式
     * AddExp → MulExp | AddExp ('+' | '-') MulExp
     */
    private int calcAddExp(AddExp addExp) {
        if (addExp == null) {
            System.out.println("strange null addExp from calcAddExp");
            return 0;
        }

        if (addExp.isMulExp()) {
            // 叶子：MulExp
            return trans2Int(visitMulExp(addExp.getMulExp()));
        }

        // 递归处理左操作数（AddExp）
        int left = calcAddExp(addExp.getAddExp());
        // 右操作数：MulExp
        int right = trans2Int(visitMulExp(addExp.getMulExp()));
        // 创建运算指令
        TokenNode op = addExp.getOperator();
        if (op != null && op.getNodeType() == SynType.MINU) {
            return left - right;
        } else if (op != null && op.getNodeType() == SynType.PLUS) {
            return left + right;
        } else {
            System.out.println("strange null op from calcAddExp");
            return 0;
        }
    }

    /**
     * 运行时处理加减表达式
     * AddExp → MulExp | AddExp ('+' | '-') MulExp
     */
    private IRValue tackleAddExp(AddExp addExp) {
        if (addExp == null) {
            System.out.println("strange null addExp from tackleAddExp");
            return null;
        }

        if (addExp.isMulExp()) {
            // 叶子：MulExp
            return visitMulExp(addExp.getMulExp());
        }

        // 递归处理左操作数（AddExp）
        IRValue left = visitAddExp(addExp.getAddExp());
        // 统一成I32
        IRInstruction resizeInst = ensureIntegerType(left,IntegerType.I32);
        left = resizeInst == null ? left : resizeInst;
        // 右操作数：MulExp
        IRValue right = visitMulExp(addExp.getMulExp());
        // 统一成I32
        resizeInst = ensureIntegerType(right,IntegerType.I32);
        left = resizeInst == null ? left : resizeInst;
        // 创建运算指令
        TokenNode op = addExp.getOperator();
        if (op != null && op.getNodeType() == SynType.MINU) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.SUB,
                    left,right);
        } else if (op != null && op.getNodeType() == SynType.PLUS) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.ADD,
                    left,right);
        } else {
            System.out.println("strange null op from tackleAddExp");
            return null;
        }
    }

    /**
     * 访问乘除表达式
     * MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     */
    public IRValue visitMulExp(MulExp mulExp) {
        if (culWhileCompiling) {
            return new IntegerConstant(IntegerType.I32,calcMulExp(mulExp));
        } else {
            return tackleMulExp(mulExp);
        }
    }

    /**
     * 编译时计算乘除表达式
     * MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     */
    private int calcMulExp(MulExp mulExp) {
        if (mulExp == null) {
            System.out.println("strange null mulExp from calcMulExp");
            return 0;
        }

        if (mulExp.isUnaryExp()) {
            // 叶子：UnaryExp
            return trans2Int(visitUnaryExp(mulExp.getUnaryExp()));
        }

        // 递归处理左操作数（MulExp）
        int left = calcMulExp(mulExp.getMulExp());
        // 右操作数：UnaryExp
        int right = trans2Int(visitUnaryExp(mulExp.getUnaryExp()));

        TokenNode op = mulExp.getOperator();
        if (op != null && op.getNodeType() == SynType.MULT) {
            return left * right;
        } else if (op != null && op.getNodeType() == SynType.DIV) {
            return left / right;
        } else if (op != null && op.getNodeType() == SynType.MOD) {
            return left % right;
        } else if (op != null && op.getNodeType() == SynType.BITANDK) {
            return left & right;
        } else if (op != null && op.getNodeType() == SynType.BITORK) {
            return left | right;
        } else if (op != null && op.getNodeType() == SynType.BITXORK) {
            return left ^ right;
        } else if (op != null && op.getNodeType() == SynType.SHLK) {
            return left << right;
        } else if (op != null && op.getNodeType() == SynType.ASHRK) {
            return left >> right;
        } else {
            System.out.println("strange null op from calcMulExp");
            return 0;
        }
    }

    /**
     * 运行时处理乘除表达式
     */
    private IRValue tackleMulExp(MulExp mulExp) {
        if (mulExp == null) {
            System.out.println("strange null mulExp from tackleMulExp");
            return null;
        }

        if (mulExp.isUnaryExp()) {
            // 叶子：UnaryExp
            return visitUnaryExp(mulExp.getUnaryExp());
        }

        // 递归计算左操作数（MulExp）
        IRValue left = visitMulExp(mulExp.getMulExp());
        // 统一成 I32
        IRInstruction resizeInst = ensureIntegerType(left, IntegerType.I32);
        left = resizeInst == null ? left : resizeInst;

        // 右操作数：UnaryExp
        IRValue right = visitUnaryExp(mulExp.getUnaryExp());
        // 统一成 I32
        resizeInst = ensureIntegerType(right, IntegerType.I32);
        right = resizeInst == null ? right : resizeInst;

        TokenNode op = mulExp.getOperator();
        if (op != null && op.getNodeType() == SynType.MULT) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.MUL,
                    left, right);
        } else if (op != null && op.getNodeType() == SynType.DIV) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.SDIV,
                    left, right);
        } else if (op != null && op.getNodeType() == SynType.MOD) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.SREM,
                    left, right);
        } else if (op != null && op.getNodeType() == SynType.BITANDK) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.BITAND,
                    left, right);
        } else if (op != null && op.getNodeType() == SynType.BITORK) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.BITOR,
                    left, right);
        } else if (op != null && op.getNodeType() == SynType.BITXORK) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.BITXOR,
                    left, right);
        } else if (op != null && op.getNodeType() == SynType.SHLK) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.SHL,
                    left, right);
        } else if (op != null && op.getNodeType() == SynType.ASHRK) {
            return createBinaryOperation(
                    BinaryOperationInstruction.BinaryOperator.ASHR,
                    left, right);
        } else {
            System.out.println("strange null op from tackleMulExp");
            return null;
        }
    }

    /**
     * UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
     * UnaryExp → UnaryExp ('++' | '--')  // Postfix
     * UnaryOp → '+' | '-' | '!' | '++' | '--'
     */
    public IRValue visitUnaryExp(UnaryExp unaryExp) {
        if (unaryExp instanceof PrimaryUnaryExp) {
            return visitPrimaryExp((PrimaryUnaryExp) unaryExp);
        } else if (unaryExp instanceof FuncCallUnaryExp) {
            return visitFunctionCall((FuncCallUnaryExp) unaryExp);
        } else {
            // Check for Postfix: UnaryExp (INC/DEC)
            ArrayList<AstNode> children = (ArrayList<AstNode>) unaryExp.getChildren();
            if (children.size() == 2 && children.get(1) instanceof TokenNode) {
                TokenNode op = (TokenNode) children.get(1);
                UnaryExp inner = (UnaryExp) children.get(0);
                
                loadLVal = false;
                IRValue ptr = visitUnaryExp(inner);
                loadLVal = true;
                
                IRValue oldVal = createLoad(ptr);
                int delta = (op.getNodeType() == SynType.INC) ? 1 : -1;
                IRValue newVal = createBinaryOperation(BinaryOperationInstruction.BinaryOperator.ADD, 
                                                     oldVal, new IntegerConstant(IntegerType.I32, delta));
                createStore(newVal, ptr);
                return oldVal;
            }
            
            // Check for Prefix INC/DEC
            // UnaryOp UnaryExp
            UnaryOp opNode = unaryExp.getOperator();
            if (opNode != null) {
                TokenNode opToken = opNode.getOperator();
                if (opToken.getTokenType() == SynType.INC || opToken.getTokenType() == SynType.DEC) {
                    UnaryExp inner = unaryExp.getExpr();
                    
                    loadLVal = false;
                    IRValue ptr = visitUnaryExp(inner);
                    loadLVal = true;
                    
                    IRValue oldVal = createLoad(ptr);
                    int delta = (opToken.getTokenType() == SynType.INC) ? 1 : -1;
                    IRValue newVal = createBinaryOperation(BinaryOperationInstruction.BinaryOperator.ADD, 
                                                         oldVal, new IntegerConstant(IntegerType.I32, delta));
                    createStore(newVal, ptr);
                    return newVal;
                }
            }
            
            return visitUnaryOpExp(unaryExp);
        }
    }

    /**
     * Ident '(' [FuncRParams] ')'
     * FuncRParams → Exp { ',' Exp }
     */
    private IRValue visitFunctionCall(FuncCallUnaryExp unaryExp) {
        String funcName = unaryExp.getFunctionName();
        FuncRParams realParamExps = unaryExp.getFuncRParams();

        IRFunction function;

        // 特判getInt()
        if (funcName.equals("getint")) {
            function = IRFunction.GETINT;
        } else {
            function = (IRFunction) curSymbolTable.lookupSymbol(funcName);
        }


        // 收集实参列表
        ArrayList<IntegerType> formalParamTypes = function.getParameterTypes();
        ArrayList<IRValue> realParamsForIR = new ArrayList<>();

        for (int i = 0; i < formalParamTypes.size(); i++) {
            // 获取真实参数的IRValue
            IntegerType formalType = formalParamTypes.get(i);
            ptrParam = ! formalType.isI();
            IRValue realParamValue = visitExp(realParamExps.get(i));
            ptrParam = false;

            // 非指针的参数要对齐
            if (formalType.isI()) {
                IRValue resizeInst = ensureIntegerType(realParamValue, formalType);
                realParamValue = resizeInst == null ? realParamValue : resizeInst;
            }

            // 加入到用来生成llvm IR的表中去
            realParamsForIR.add(realParamValue);
        }

        // 调用指令
        CallInstruction funcCall;

        // void函数
        if (function.getReturnType() instanceof VoidType) {
            funcCall = createCallVoid(function,realParamsForIR);
        }
        // 有返回值
        else {
            funcCall = createCall(function,realParamsForIR);
        }

        return funcCall;
    }

    /**
     * UnaryOp UnaryExp
     * UnaryOp → '+' | '−' | '!'
     */
    private IRValue visitUnaryOpExp(UnaryExp unaryExp) {
        if (culWhileCompiling) {
            return new IntegerConstant(IntegerType.I32,calcUnaryOpExp(unaryExp));
        } else {
            return tackleUnaryOpExp(unaryExp);
        }
    }

    /**
     * UnaryOp UnaryExp
     * UnaryOp → '+' | '−' | '!'
     */
    private int calcUnaryOpExp(UnaryExp unaryExp) {
        UnaryExp exp = unaryExp.getExpr();
        TokenNode op = unaryExp.getOperator().getOperator();

        IntegerConstant expValue = (IntegerConstant) visitUnaryExp(exp);
        int expNum = expValue.getConstantValue();

        if (op.getTokenType() == SynType.MINU) {
            expNum = -expNum;
        } else if (op.getTokenType() == SynType.NOT) {
            // TODO:可能有问题
            if (expNum == 0) {
                expNum = 1;
            } else {
                expNum = 0;
            }
        }

        return expNum;
    }

    /**
     * UnaryOp UnaryExp
     * UnaryOp → '+' | '−' | '!'
     */
    private IRValue tackleUnaryOpExp(UnaryExp unaryExp) {
        UnaryExp exp = unaryExp.getExpr();
        TokenNode op = unaryExp.getOperator().getOperator();

        IRValue expValue = visitUnaryExp(exp);

        IRInstruction resizeInst = ensureIntegerType(expValue, IntegerType.I32);
        expValue = resizeInst == null ? expValue : resizeInst;

        IRValue res = expValue;

        if (op.getTokenType() == SynType.MINU) {
            res = createBinaryOperation(BinaryOperationInstruction.BinaryOperator.SUB,new IntegerConstant(IntegerType.I32,0),expValue);
        } else if (op.getTokenType() == SynType.NOT) {
            // 把 !x 编译成 “ x == 0 ” 的布尔判断
            res = createCompare(CompareInstruction.CompareCondition.EQ, expValue,new IntegerConstant(IntegerType.I32,0));
        }

        return res;
    }

    /**
     * PrimaryExp → '(' Exp ')' | LVal | Number
     */
    public IRValue visitPrimaryExp(PrimaryUnaryExp primaryUnaryExp) {
        PrimaryExp primaryExp = primaryUnaryExp.getPrimaryExp();

        if (primaryExp.isExp()) {
            return visitExp(primaryExp.getExp());
        } else if (primaryExp.isLVal()) {
            return visitPrimLVal(primaryExp);
        } else if (primaryExp.isNumber()) {
            // return new IntegerConstant(IntegerType.I32,primaryExp.getNumber());
            return visitNumber(primaryExp);
        } else {
            System.out.println("primaryExp is strange by visitPrimaryExp");
            return null;
        }
    }

    /**
     * LVal 在 C 语义里表示“有地址的对象”（lvalue），在 IR 里通常先表示为一个指针（地址），是否需要用 load 取出值取决于使用场景。
     * 这个方法根据场景返回“指针本身”或“从指针加载出的值”，以匹配 C 的实参传递和表达式求值规则。
     */
    public IRValue visitPrimLVal(PrimaryExp primaryExp) {
        LVal lval = primaryExp.getLVal();

        /**
         * const int c = 3; use(c);
         * 非数组常量，符号表里存的是IntegerConstant，不是内存地址，因此不会也不能 load
         */
        if (culWhileCompiling) {
            return visitLVal(lval);
        }
        else {
            if (ptrParam) {
                /**
                 * int a[10]; use(a);
                 * 当前在函数实参位置，且形参期望“指针类型”
                 * 此时恰是指针，传递 a 的地址，不 load
                 */
                ptrParam = false;
                return visitLVal(lval);
            } else {
                /**
                 * int x; use(x);
                 * 从 x 的地址 load 得到 i32 值再传递
                 */
                IRValue res = visitLVal(lval);

                // LVal应该是指针的，但是有非数组常量也是LVal
                // 确定LVal是指针再load
                if (!(res.getType().isI())) {
                    res = createLoad(res);
                }
                return res;
            }
        }
    }

    /**
     * LVal → Ident {'[' Exp ']'}
     */
    public IRValue visitLVal(LVal lVal) {
        Exp exp = lVal.getExp(0);
        String indentifier = lVal.getIdentifierName();

        IRValue value = curSymbolTable.lookupSymbol(indentifier);

        // 非数组常量或已折叠为"立即数"的标识符：直接是 i32/i8 值，不是地址
        // C示例：const int c = 3;  在表达式里使用 c（不是取地址）=> 直接用 3
        if (value.getType().isI()) {
            return value;
        }

        // value 是指针类型（指向变量/数组/指针等）
        IRType pointeeType = ((PointerType)value.getType()).getPointeeType();

        // 指向普通变量（i32/i8）
        if (pointeeType.isI()) {
            if (culWhileCompiling && value instanceof IRGlobalVariable globalVariable) {
                /**
                 * 处于"可计算/常量折叠"上下文时，不能生成 load；直接取全局变量的初始常量用于拼接其他常量/全局初始化
                 * C示例：int g = 42;  const int k = g;  // 使用 g 的初始化常量 42
                 */
                return globalVariable.getInit();
            } else {
                /**
                 * 普通求值场景：返回指针本身（是否 load 由上层决定）
                 * C示例：int x;  表达式里用 x（作为 rvalue）通常需要先从 &x load 出值
                 */
                return value;
            }
        }
        // 指向数组（[N x i32] 或 [N x i8] 或多维数组）
        if (pointeeType.isArrayType()) {
            if (culWhileCompiling) {
                // 常量折叠：计算线性索引
                int totalIndex = 0;
                // 获取当前维度的数组类型
                ArrayType currentArrayType = (ArrayType) pointeeType;
                
                // 处理第一个索引
                if (exp != null) {
                    totalIndex = ((IntegerConstant)visitExp(exp)).getConstantValue();
                }
                
                // 处理后续索引
                for (int i = 1; i < lVal.getExps().size(); i++) {
                    // 获取下一维度的元素数量
                    if (currentArrayType.getElementType().isArrayType()) {
                        currentArrayType = (ArrayType) currentArrayType.getElementType();
                        int dimSize = currentArrayType.getArrayLenth(); // 这里的逻辑需要修正，应该是每一层的大小
                        // 实际上ArrayType只知道自己的长度，不知道子元素的具体结构（除非递归）
                        // 需要重新计算步长
                        // 简单处理：假设我们已经到了最后一维，直接取值是不对的
                        // 需要按照多维数组的线性化公式：idx = idx * dim_size + next_idx
                    }
                    // 这里逻辑比较复杂，因为ArrayType结构是嵌套的
                    // [2 x [3 x i32]]
                    // a[1][2] -> 1 * 3 + 2
                }
                
                // 重新实现多维数组常量折叠逻辑
                // 1. 获取所有维度的大小
                ArrayList<Integer> dims = new ArrayList<>();
                IRType temp = pointeeType;
                while (temp instanceof ArrayType) {
                    dims.add(((ArrayType) temp).getArrayLenth());
                    temp = ((ArrayType) temp).getElementType();
                }
                
                // 2. 获取所有索引值
                ArrayList<Integer> indices = new ArrayList<>();
                if (exp != null) indices.add(((IntegerConstant)visitExp(exp)).getConstantValue());
                for (int i = 1; i < lVal.getExps().size(); i++) {
                    indices.add(((IntegerConstant)visitExp(lVal.getExps().get(i))).getConstantValue());
                }
                
                // 3. 计算线性偏移
                int flatIndex = 0;
                // 注意：dims的第一个维度（dims.get(0)）在计算中其实只作为上限检查（可选），
                // 计算偏移时用到的是后续维度的大小。
                // 比如 int a[2][3]; a[1][2] -> 1 * 3 + 2
                
                flatIndex = 0;
                for (int i = 0; i < indices.size(); i++) {
                    int stride = 1;
                    // 计算当前维度之后的总步长
                    for (int j = i + 1; j < dims.size(); j++) {
                        stride *= dims.get(j);
                    }
                    flatIndex += indices.get(i) * stride;
                }

                if (value instanceof AllocaInstruction) {
                    AllocaInstruction alloca = (AllocaInstruction) value;
                    // Check if initial value exists
                    if (alloca.getInitialValue() == null) {
                         // Uninitialized or zero-initialized by default?
                         // If no initial value, we can assume 0 or return undefined.
                         return new IntegerConstant(IntegerType.I32, 0); 
                    }
                    ArrayConstant initArray = (ArrayConstant) alloca.getInitialValue();
                    return initArray.getElementConstant(flatIndex);
                }
                if (value instanceof IRGlobalVariable) {
                    IRGlobalVariable globalVariable = (IRGlobalVariable) value;
                    if (globalVariable.getInit() == null) {
                        return new IntegerConstant(IntegerType.I32, 0);
                    }
                    ArrayConstant initArray = (ArrayConstant) globalVariable.getInit();
                    return initArray.getElementConstant(flatIndex);
                }
            } else {
                /**
                 * 运行期地址计算（多维数组 GEP）：
                 * 生成一系列 GEP 指令或单个多索引 GEP 指令
                 */
                ArrayList<IRValue> indices = new ArrayList<>();
                indices.add(new IntegerConstant(IntegerType.I32, 0)); // 第一个0用于解引用指针
                
                if (exp != null) {
                    indices.add(visitExp(exp));
                }
                
                for (int i = 1; i < lVal.getExps().size(); i++) {
                    indices.add(visitExp(lVal.getExps().get(i)));
                }
                
                // 如果索引数量少于维度数量，结果是指向子数组的指针
                // 如果索引数量等于维度数量，结果是指向元素的指针
                
                // 自动Decay：如果提供的索引少于数组维度，自动补一个0，使数组名/子数组Decay为指向首元素的指针
                int dimCount = 0;
                IRType temp = pointeeType;
                while (temp instanceof ArrayType) {
                    dimCount++;
                    temp = ((ArrayType) temp).getElementType();
                }
                
                int providedCount = lVal.getExps().size();
                if (providedCount < dimCount) {
                    indices.add(new IntegerConstant(IntegerType.I32, 0));
                }
                
                return createGetElementPtr(value, indices);
            }
        }
        // 指向指针（函数参数中的多维数组，如 int a[][3] -> [3 x i32]*）
        if (pointeeType.isPointerType()) {
            // 先load出基地址
            LoadInstruction baseAddr = createLoad(value);
            
            ArrayList<IRValue> indices = new ArrayList<>();
            // 注意：对于指针指向的数组，第一个索引就是第一维的偏移，不需要额外的0
            
            if (exp != null) {
                indices.add(visitExp(exp));
            } else {
                // 如果没有提供任何索引，直接返回加载出的指针
                return baseAddr;
            }
            
            for (int i = 1; i < lVal.getExps().size(); i++) {
                indices.add(visitExp(lVal.getExps().get(i)));
            }
            
            // 自动Decay：计算维度（包括指针本身的一维）
            int dimCount = 1; // 指针本身算一维
            IRType temp = ((PointerType) value.getType()).getPointeeType(); // baseAddr type is same as value type (loaded pointer)
            // Wait, baseAddr is loaded value. value is alloca (pointer to pointer).
            // pointeeType variable in this method is ((PointerType)value.getType()).getPointeeType() -> i32* or [3xi32]*
            
            temp = pointeeType;
            if (temp instanceof PointerType) {
                 // baseAddr is the pointer.
                 temp = ((PointerType) temp).getPointeeType();
            }
            // Now temp is the element type of the pointer (e.g. i32 or [3xi32])
            
            while (temp instanceof ArrayType) {
                dimCount++;
                temp = ((ArrayType) temp).getElementType();
            }
            
            // 用户提供的索引数量 (在indices中)
            // indices 包含第一个索引（如果exp!=null）和后续索引
            // 注意：对于指针访问，第一个索引是指针偏移。
            // int a[] -> a[i] -> GEP a, i.
            // int a[][3] -> a[i][j] -> GEP a, i, j.
            
            // 如果 int a[][3]. use a[i]. indices=[i]. provided=1. dim=2 ([3xi32]*).
            // Need GEP a, i, 0.
            
            if (indices.size() < dimCount) {
                indices.add(new IntegerConstant(IntegerType.I32, 0));
            }
            
            return createGetElementPtr(baseAddr, indices);
        }

        return null;
    }

    /**
     * Number → IntConst
     */
    private IRValue visitNumber(PrimaryExp primaryExp) {
        return new IntegerConstant(IntegerType.I32,primaryExp.getNumber());
    }














    // ==================== 辅助方法 ====================

    /**
     * 进入新的符号表作用域
     */
    private void enter() {
        curSymbolTable = new IRSymTable(curSymbolTable);
    }

    /**
     * 退出当前符号表作用域
     */
    private void leave() {
        curSymbolTable = curSymbolTable.getParentScope();
    }

    /**
     * 判断是否在全局作用域
     */
    private boolean isGlobal() {
        return curSymbolTable == rootSymbolTable;
    }

    /**
     * 访问函数形参列表
     * FuncFParams → FuncFParam { ',' FuncFParam }
     */
    private void visitFuncFParams(BranchNode funcFParams) {
        if (funcFParams == null) {
            return;
        }
        List<AstNode> children = funcFParams.getChildren();
        int paramIndex = 0;

        for (AstNode child : children) {
            if (child.getNodeType() == SynType.FuncFParam) {
                visitFuncFParam((BranchNode) child, paramIndex++);
            }
        }
    }

    /**
     * 访问函数形参
     * FuncFParam → BType Ident ['[' ']']
     *
     * define i32 @func(i32 %a0) {
     * entry:
     *     %v1 = alloca i32        ; 为形参x分配栈空间
     *     store i32 %a0, i32* %v1 ; 将形参值存储到alloca中
     *
     *     ; 后续使用x时：
     *     %v2 = load i32, i32* %v1    ; 读取x的值
     *     %v3 = add i32 %v2, 1        ; x + 1
     *     store i32 %v3, i32* %v1     ; 将新值存回x
     *
     *     %v4 = load i32, i32* %v1    ; 读取返回值
     *     ret i32 %v4
     * }
     */
    private void visitFuncFParam(BranchNode funcFParam, int paramIndex) {
        IntegerType integerType = currentFunction.getParameterTypes().get(paramIndex);
        // 1. 为形参分配栈空间
        AllocaInstruction alloc = createAlloca(integerType);

        // 2. 符号表中存储alloca，而不是FParam
        String name = ((FuncFParam)funcFParam).getParamName();
        curSymbolTable.insertSymbol(name, alloc);

        // 3. 将FParam的值存储到alloca中
        // 对于多维数组参数，integerType已经是PointerType了
        IRFunctionParameter param = currentFunction.getParameters().get(paramIndex);
        createStore(param,alloc);
    }

    /**
     * 创建字符串字面量并调用putstr
     * 有错误，应该是：
     * 1. 创建或复用全局字符串字面量（字符串池），并注册到 module ，例如生成 @.strN = [len x i8] c"..." 。    OK
     * 2. 对全局字符串做 getelementptr ，索引 [0, 0] ，得到指向首字符的 i8* 指针（内部会把索引用 i32 统一）。
     * 3. 以 i8* 指针作为唯一参数调用 putstr
     */
    private void createStringLiteralAndPutstr(String str) {
        IRStringLiteral stringLiteral = createStringLiteral(str);
        GetElementPtrInstruction strPtr = createGetElementPtr(stringLiteral);
        ArrayList<IRValue> args = new ArrayList<>();
        args.add(strPtr);
        createCallVoid(IRFunction.PUTSTR,args);
    }

    /**
     * 将IRValue转换为int
     */
    private int trans2Int(IRValue value) {
        if (value instanceof IntegerConstant) {
            return ((IntegerConstant) value).getConstantValue();
        }
        return 0;
    }

    /**
     * 求basicType的TokenNode对应的IntegerType(IR用)类型
     */
    private IntegerType getIntType(TokenNode basicType) {
        switch (basicType.getTokenType()) {
            case INTTK :
                return IntegerType.I32;
            case VOIDTK:
                return new VoidType();
            default:
                return null;
        }
    }
}