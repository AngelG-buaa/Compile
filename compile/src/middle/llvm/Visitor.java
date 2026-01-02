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
import front.parser.syntax.stmt.ContinueStmt;
import front.parser.syntax.stmt.ExpStmt;
import front.parser.syntax.stmt.ForLoopStmt;
import front.parser.syntax.stmt.ForStmt;
import front.parser.syntax.stmt.IfStmt;
import front.parser.syntax.stmt.PrintStmt;
import front.parser.syntax.stmt.ReturnStmt;
import front.parser.syntax.stmt.Stmt;
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
            // 计算数组长度
            int arrayLen = visitConstExp(constExp.get(0));
            // 根据数组长度和元素类型，得到数组类型
            ArrayType arrayType = new ArrayType(getIntType(bType), arrayLen);
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
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, arrayLen);
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
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, arrayLen);
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
                    initVals = visitInitVal(initVal, getIntType(bType) == IntegerType.I8, arrayLen);
                    // GEP获取一个int*
                    GetElementPtrInstruction basePtr = createGetElementPtr(alloc);
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
    public ArrayList<IRValue> visitInitVal(InitVal initVal, boolean isChar, int length) {
        ArrayList<IRValue> res = new ArrayList<>();

        // 1. 收集所有显式初始化的值
        ArrayList<Exp> exps = initVal.getExps();
        if (exps != null) {
            for (Exp exp : exps) {
                res.add(visitExp(exp));
            }
        }

        // 2. 自动补零到指定长度
        // 无论是 int 数组还是 char 数组，只要初始值个数少于数组长度，都应该补零
        // 例如: int a[3] = {} -> 补3个0
        //       int a[3] = {1} -> 补2个0
        while (res.size() < length) {
            res.add(new IntegerConstant(isChar ? IntegerType.I8 : IntegerType.I32, 0));
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
        ConstExp constExp = ((ConstDef)constDef).getConstExp();
        ConstInitVal initVal = ((ConstDef) constDef).getInitValue();

        ArrayList<Integer> initInts = visitConstInitVal(initVal);

        // 非数组的普通定义
        if (constExp == null) {
            IntegerConstant constInits
                    = new IntegerConstant(
                    getIntType(bType.getType()),
                    initInts.get(0));

            curSymbolTable.insertSymbol(ident.getContent(),constInits);
        }
        // 数组
        else {
            // 计算数组长度
            int arrayLen = visitConstExp(constExp);
            // 根据数组长度和元素类型，得到数组类型
            ArrayType arrayType = new ArrayType(getIntType(bType.getType()), arrayLen);
            ArrayConstant constArray = new ArrayConstant(arrayType, initInts);

            if (isGlobal()) {
                // 全局数组无需alloca，直接初始化
                IRGlobalVariable globalVariable = createGlobalVariable(ident.getContent(),constArray,true);
                curSymbolTable.insertSymbol(ident.getContent(),globalVariable);
            } else {
                // 局部数组
                /**
                 *     %1 = alloca [3 x i32]
                 *     %2 = getelementptr inbounds [3 x i32], [3 x i32]* %1, i32 0, i32 0
                 *     store i32 1, i32* %2
                 *     %3 = getelementptr inbounds i32, i32* %2, i32 1
                 *     store i32 2, i32* %3
                 *     %4 = getelementptr inbounds i32, i32* %3, i32 1
                 *     store i32 3, i32* %4
                 */
                // alloca长为arrayLen的空间
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
     * 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
     */
    public ArrayList<Integer> visitConstInitVal(ConstInitVal initVal) {
        ArrayList<Integer> res = new ArrayList<>();

        ArrayList<ConstExp> constExps = initVal.getExps();
        if (constExps.size() == 1) {
            res.add(visitConstExp(constExps.get(0)));
        }
        else {
            for (ConstExp constExp : constExps) {
                res.add(visitConstExp(constExp));
            }
        }
        return res;
    }

    /**
     * 访问函数定义
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     */
    public void visitFuncDef(BranchNode funcDef) {
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
     * Stmt → LVal '=' Exp ';'
     * | [Exp] ';'
     * | Block
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * | 'break' ';'
     * | 'continue' ';'
     * | 'return' [Exp] ';'
     * | 'printf''('StringConst {','Exp}')'';'
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
                作用域是在 visitStmt(ifStmt) 的分派中由 visitBlockSubStmt 内部，
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
        /**
         * 用恒定条件的 createBranch 保留了两个后继（exitBb 和 updateBb）
         * 有利于构建/分析 CFG、优化和数据流分析
         * 用 createJump 只会产生一个后继边
         */
        LoopStructure curLoop = loops.peek();
        createBranch(
                new IntegerConstant(IntegerType.I32,1)
                ,curLoop.getExitBlock()
                ,curLoop.getUpdateBlock());
        /**
         * 终结指令（jump/branch）一旦写入当前基本块，这个块就“结束”了
         * 再往里追加指令会产生非法IR
         * 把当前块切到一个哨兵块，能把后续生成的指令引流走，避免污染已终结块。
         * 同时这些占位块也不会出现在最终函数的基本块列表里
         * 因为它们不是通过 factory 的 createBasicBlock 注册的
         */
        currentBasicBlock = new IRBasicBlock(null,200000);
    }

    /**
     * 'continue' ';'
     */
    private void visitContinueStmt(ContinueStmt stmt) {
        LoopStructure curLoop = loops.peek();
        createBranch(
                new IntegerConstant(IntegerType.I32,1)
                ,curLoop.getUpdateBlock()
                ,curLoop.getExitBlock()
        );
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
     * Exp → AddExp
     */
    public IRValue visitExp(Exp exp) {
        return visitAddExp(exp.getAddExp());
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
        } else {
            System.out.println("strange null op from tackleMulExp");
            return null;
        }
    }

    /**
     * UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
     * UnaryExp → PrimaryUnaryExp | FuncCallUnaryExp | UnaryExp
     */
    public IRValue visitUnaryExp(UnaryExp unaryExp) {
        if (unaryExp instanceof PrimaryUnaryExp) {
            return visitPrimaryExp((PrimaryUnaryExp) unaryExp);
        } else if (unaryExp instanceof FuncCallUnaryExp) {
            return visitFunctionCall((FuncCallUnaryExp) unaryExp);
        } else {
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
        // 指向数组（[N x i32] 或 [N x i8]）
        if (pointeeType.isArrayType()) {
            if (culWhileCompiling) {
                /**
                 * 常量折叠场景：索引也是常量，直接返回初始化数组的某元素常量
                 * C示例：int a[3] = {1,2,3};  const int k = a[1];  // 直接用 2
                 */
                int index = ((IntegerConstant)visitExp(exp)).getConstantValue();

                if (value instanceof AllocaInstruction) {
                    AllocaInstruction alloca = (AllocaInstruction) value;
                    ArrayConstant initArray = (ArrayConstant) alloca.getInitialValue();
                    return initArray.getElementConstant(index);
                }
                if (value instanceof IRGlobalVariable) {
                    IRGlobalVariable globalVariable = (IRGlobalVariable) value;
                    ArrayConstant initArray = (ArrayConstant) globalVariable.getInit();
                    return initArray.getElementConstant(index);
                }
            } else {
                /**
                 * 运行期地址计算：
                 * - 无索引：数组名衰变为指针（GEP 到首元素）
                 *   C示例：int a[3];  foo(a);  // 传递 &a[0]
                 * - 有索引：计算 a[index] 的地址（GEP base, 0, index）
                 *   C示例：int a[3];  use(a[1]);  // 取 &a[1]，上层按需 load
                 */
                if (exp == null) {
                    GetElementPtrInstruction GEP = createGetElementPtr(value);
                    return GEP;
                } else {
                    IRValue index = visitExp(exp);
                    GetElementPtrInstruction GEP = createGetElementPtr(value,new IntegerConstant(IntegerType.I32,0),index);
                    return GEP;
                }
            }
        }
        // 指向指针（例如形参为"指向数组的指针"，函数内部再按索引访问）
        if (pointeeType.isPointerType()) {
            /**
             * 典型于函数内部的"指针的指针"场景：
             * - 无索引：先从指针的指针中 load 出数组指针（用于继续传参）
             *   C示例：void f(int *p[]) { g(p); }  // 从 p 的地址中取出 p，再传给 g
             * - 有索引：先 load 出数组指针，再对元素做 GEP
             *   C示例：void f(int *p[]) { use(p[i]); }  // p 是指向首元素的指针，p[i] 需要 GEP
             */
            if (exp == null) {
                LoadInstruction array = createLoad(value);
                return array;
            } else {
                LoadInstruction array = createLoad(value);
                IRValue index = visitExp(exp);
                GetElementPtrInstruction GEP = createGetElementPtr(array,index);
                return GEP;
            }
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

        /**
         * 2. 符号表中存储alloca，而不是FParam
         *   1. FParam 是值， alloca 是地址
         *   2. 变量需要的是可读写的内存位置，而不是只读的值
         *   3. 保证形参和局部变量的使用方式完全一致
         */
        String name = ((FuncFParam)funcFParam).getParamName();
        curSymbolTable.insertSymbol(name, alloc);

        // 3. 将FParam的值存储到alloca中
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