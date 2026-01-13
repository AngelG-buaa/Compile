package middle.checker;

import error.Error;
import error.ErrorManager;
import error.ErrorType;
import front.parser.syntax.*;
import front.parser.syntax.decl.*;
import front.parser.syntax.exp.*;
import front.parser.syntax.func.*;
import front.parser.syntax.stmt.*;
import middle.checker.symbol.*;

import java.util.ArrayList;
import java.util.List;

import static utils.CompareSymbolType.isTypeCompatible;
import static utils.GetExpType.getExpType;

/**
 * 语义分析器类
 *
 * 该类负责对语法分析生成的抽象语法树(AST)进行语义分析，主要功能包括：
 * 1. 符号表管理：建立和维护符号表，处理作用域嵌套
 * 2. 类型检查：检查变量、函数的类型一致性
 * 3. 语义错误检测：检测重定义、未定义、类型不匹配等语义错误
 * 4. 符号表输出：按要求输出符号表到symbol.txt
 *
 * 设计要求：
 * - main是保留关键字，不纳入符号表中
 * - 按作用域序号输出，同一作用域按声明顺序输出
 * - 函数参数属于函数内部作用域
 */
public class Checker {
    private static boolean voidFunc = false;
    private static int loopNum = 0;
    private static int switchNum = 0;

    /**
     * 语义分析入口方法
     *
     * 对整个编译单元进行语义分析，包括初始化符号管理器、
     * 遍历AST进行语义检查，最后输出符号表。
     *
     * @param root 编译单元的根节点，通常是CompUnit类型的AST节点
     */
    public static void analyze(BranchNode root) {
        // 初始化符号管理器
        SymbolManager.init();

        // 访问编译单元
        visitCompUnit(root);

        // 回到根符号表
        SymbolManager.goBackToRootSymbolTable();
    }

    /**
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     */
    private static void visitCompUnit(BranchNode compUnit) {
        List<AstNode> children = compUnit.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.Decl) {
                visitDecl((Decl) child);
            } else if (child.getNodeType() == SynType.FuncDef) {
                visitFuncDef((FuncDef) child);
            } else if (child.getNodeType() == SynType.MainFuncDef) {
                visitMainFuncDef((MainFuncDef) child);
            }
        }
    }

    /**
     * Decl → ConstDecl | VarDecl
     */
    private static void visitDecl(Decl decl) {
        if (decl.isConstDecl()) {
            visitConstDecl(decl.getConstDecl());
        } else {
            visitVarDecl(decl.getVarDecl());
        }
    }

    /**
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     */
    private static void visitConstDecl(BranchNode constDecl) {
        String type = ((ConstDecl) constDecl).getType().getTypeName();

        ArrayList<ConstDef> constDefs = ((ConstDecl) constDecl).getConstDefs();

        for (ConstDef constDef : constDefs) {
            visitConstDef(constDef, type);
        }
    }

    /**
     * ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
     */
    private static void visitConstDef(BranchNode constDef, String type) {
        String name = ((ConstDef) constDef).getIdentifier().getContent();
        ConstExp constExp = ((ConstDef) constDef).getConstExp();
        ConstInitVal constInitVal = ((ConstDef) constDef).getInitValue();
        int lineNumber = ((ConstDef) constDef).getIdentifier().getLineNumber();

        // 检查重定义
        if (SymbolManager.getCurrentSymbolTable().getSymbol(name) != null) {
            ErrorManager.AddError(Error.createError(ErrorType.NAME_REDEFINE, lineNumber));
            return;
        }

        // 创建符号
        SymbolType symbolType = constExp == null ?
                SymbolType.CONST_INT : SymbolType.CONST_INT_ARRAY;
        ValueSymbol symbol = new ValueSymbol(name, symbolType);
        SymbolManager.addSymbol(symbol, lineNumber);

        if (constExp != null) {
            visitConstExp(constExp);
        }

        visitConstInitVal(constInitVal);
    }

    /**
     * 访问常量初始化值节点
     * ConstInitVal → ConstExp
     *                | '{' [ ConstExp { ',' ConstExp } ] '}'
     *                | StringConst
     * @param constInitVal 常量初始化值节点
     */
    private static void visitConstInitVal(BranchNode constInitVal) {
        ConstInitVal node = (ConstInitVal) constInitVal;
        if (node.isLeaf()) {
            if (node.getConstExp() != null) {
                visitConstExp(node.getConstExp());
            }
        } else {
            for (ConstInitVal child : node.getInitVals()) {
                visitConstInitVal(child);
            }
        }
    }

    /**
     * 访问常量表达式节点
     *
     * @param constExp 常量表达式节点
     */
    private static void visitConstExp(BranchNode constExp) {
        List<AstNode> children = constExp.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.AddExp) {
                visitAddExp((AddExp) child);
            }
        }
    }

    /**
     * 访问变量声明节点
     *
     * 遍历变量声明节点，提取类型和静态标志，处理每个变量定义。
     *
     * @param varDecl 变量声明节点
     */
    private static void visitVarDecl(BranchNode varDecl) {
        String type = ((VarDecl) varDecl).getBType().getTypeName();
        boolean isStatic = ((VarDecl) varDecl).isStatic();

        ArrayList<VarDef> varDefs = ((VarDecl) varDecl).getVarDefs();

        for (VarDef varDef : varDefs) {
            visitVarDef(varDef, type, isStatic);
        }
    }

    /**
     * 访问变量定义节点
     *
     * VarDef → Ident [ '[' ConstExp ']' ]
     *        | Ident [ '[' ConstExp ']' ] '=' InitVal
     *
     * @param varDef 变量定义节点
     * @param type 变量类型
     * @param isStatic 是否为静态变量
     */
    private static void visitVarDef(BranchNode varDef, String type, boolean isStatic) {
        String name = ((VarDef) varDef).getIdentifier().getContent();
        int lineNumber = ((VarDef) varDef).getIdentifier().getLineNumber();
        ArrayList<ConstExp> constExps = ((VarDef) varDef).getConstExps();
        InitVal initVal = ((VarDef) varDef).getInitValue();

        // 检查重定义
        if (SymbolManager.getCurrentSymbolTable().getSymbol(name) != null) {
            ErrorManager.AddError(Error.createError(ErrorType.NAME_REDEFINE, lineNumber));
            return;
        }

        // 确定符号类型
        SymbolType symbolType;
        if (isStatic) {
            symbolType = constExps.isEmpty() ? SymbolType.STATIC_INT : SymbolType.STATIC_INT_ARRAY;
        } else {
            symbolType = constExps.isEmpty() ? SymbolType.INT : SymbolType.INT_ARRAY;
        }
        ValueSymbol symbol = new ValueSymbol(name, symbolType);
        SymbolManager.addSymbol(symbol, lineNumber);

        if (!constExps.isEmpty()) {
            visitConstExp(constExps.get(0));
        }
        if (initVal != null) {
            visitInitVal(initVal);
        }
    }

    /**
     * 访问初始化值节点
     *
     * @param initVal 初始化值节点
     */
    private static void visitInitVal(BranchNode initVal) {
        List<AstNode> children = initVal.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.Exp) {
                visitExp((BranchNode) child);
            } else if (child.getNodeType() == SynType.InitVal) {
                visitInitVal((BranchNode) child);
            }
        }
    }

    /**
     * 访问函数定义节点
     *
     * 处理函数定义，提取函数信息，创建函数符号，处理参数和函数体。
     * 注意：main函数不会被添加到符号表中，因为main是保留关键字。
     *
     * @param funcDef 函数定义节点
     */
    private static void visitFuncDef(FuncDef funcDef) {
        String funcName = funcDef.getFunctionName();
        String funcType = funcDef.getFuncType().getType().getContent();
        int lineNumber = funcDef.getLineNumber();
        Block body = funcDef.getBody();

        if (SymbolManager.getCurrentSymbolTable().getSymbol(funcName) != null) {
            ErrorManager.AddError(Error.createError(ErrorType.NAME_REDEFINE, lineNumber));
        }

        if (funcType.equals("void")) {
            voidFunc = true;
        } else if (funcType.equals("int")) {
            if (body.getItemCount() == 0) {
                ErrorManager.AddError(Error.createError(ErrorType.MISS_RETURN, getLastLineNumber(body)));
            } else if (!hasReturnInBlock(body)) {
                ErrorManager.AddError(Error.createError(ErrorType.MISS_RETURN, getLastLineNumber(body)));
            }
        }

        SymbolType symbolType = SymbolType.getFuncType(funcType);
        FuncSymbol symbol = new FuncSymbol(funcName, symbolType, funcDef.getParams());
        SymbolManager.addSymbol(symbol, lineNumber);

        SymbolManager.enterFunc(funcType);
        SymbolManager.createSonSymbolTable();

        // 在函数体作用域中添加参数到符号表
        if (funcDef.getParams() != null) {
            visitFuncFParams(funcDef.getParams());
        }

        visitBlock(body);

        voidFunc = false;

        SymbolManager.goToFatherSymbolTable();
        SymbolManager.leaveFunc();
    }

    /**
     * 检查代码块是否包含返回语句
     */
    private static boolean hasReturnInBlock(Block block) {
        BlockItem lastBlockItem = block.getBlockItems().get(block.getItemCount() - 1);
        return (lastBlockItem.isStmt() && lastBlockItem.getStmt() instanceof ReturnStmt);
    }

    /**
     * 获取代码块的最后一行行号
     */
    private static int getLastLineNumber(BranchNode block) {
        List<AstNode> children = block.getChildren();
        if (!children.isEmpty()) {
            AstNode lastChild = children.get(children.size() - 1);
            if (lastChild instanceof TokenNode) {
                return ((TokenNode) lastChild).getLineNumber();
            }
        }
        return 23373478; // 默认行号
    }

    /**
     * 访问主函数定义节点
     *
     * 处理main函数定义，但不将main函数添加到符号表（main是保留关键字）。
     *
     * @param mainFuncDef 主函数定义节点
     */
    private static void visitMainFuncDef(BranchNode mainFuncDef) {
        List<AstNode> children = mainFuncDef.getChildren();
        String funcType = "int";
        Block body = ((MainFuncDef) mainFuncDef).getBody();

        if (body.getItemCount() == 0) {
            ErrorManager.AddError(Error.createError(ErrorType.MISS_RETURN, getLastLineNumber(body)));
        } else if (!hasReturnInBlock(body)) {
            ErrorManager.AddError(Error.createError(ErrorType.MISS_RETURN, getLastLineNumber(body)));
        }

        // 处理函数体，但不将main函数添加到符号表
        if (body != null) {
            SymbolManager.enterFunc(funcType);
            SymbolManager.createSonSymbolTable();
            visitBlock(body);

            SymbolManager.goToFatherSymbolTable();
            SymbolManager.leaveFunc();
        }
    }

    /**
     * 访问函数参数列表
     *
     * @param funcFParams 函数参数列表节点
     * @return 参数符号列表
     */
    private static void visitFuncFParams(BranchNode funcFParams) {
        ArrayList<Symbol> params = new ArrayList<>();
        List<AstNode> children = funcFParams.getChildren();

        for (AstNode child : children) {
            if (child.getNodeType() == SynType.FuncFParam) {
                visitFuncFParam((BranchNode) child);
            }
        }
    }

    /**
     * 访问函数参数
     * FuncFParam → BType Ident ['[' ']']
     * @param funcFParam 函数参数节点
     * @return 参数符号
     */
    private static void visitFuncFParam(BranchNode funcFParam) {
        String type = ((FuncFParam) funcFParam).getType().getTypeName();
        String name = ((FuncFParam) funcFParam).getParamName();
        int lineNumber = ((FuncFParam) funcFParam).getLineNumber();
        boolean isArray = ((FuncFParam) funcFParam).isArray();

        if (SymbolManager.getCurrentSymbolTable().getSymbol(name) != null) {
            ErrorManager.AddError(Error.createError(ErrorType.NAME_REDEFINE, lineNumber));
        }

        SymbolType symbolType = isArray ? SymbolType.INT_ARRAY : SymbolType.INT;
        ValueSymbol param = new ValueSymbol(name, symbolType);
        SymbolManager.addSymbol(param, lineNumber);
    }

    /**
     * 访问代码块节点
     *
     * @param block Block节点
     */
    private static void visitBlock(BranchNode block) {
        List<AstNode> children = block.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.BlockItem) {
                visitBlockItem((BranchNode) child);
            }
        }
    }

    /**
     * 访问代码块项节点
     *
     * @param blockItem 代码块项节点
     */
    private static void visitBlockItem(BranchNode blockItem) {
        List<AstNode> children = blockItem.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.Decl) {
                visitDecl((Decl) child);
            } else if (child.getNodeType() == SynType.Stmt) {
                visitStmt((BranchNode) child);
            }
        }
    }

    /**
     * 访问语句节点
     *
     * 文法定义：
     * Stmt → LVal '=' Exp ';'                                    // 赋值语句
     *      | [Exp] ';'                                           // 表达式语句
     *      | Block                                               // 块语句
     *      | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]            // 条件语句
     *      | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt  // 循环语句
     *      | 'break' ';'                                         // 跳出语句
     *      | 'continue' ';'                                      // 继续语句
     *      | 'return' [Exp] ';'                                  // 返回语句
     *      | 'printf' '(' StringConst { ',' Exp} ')' ';'        // 输出语句
     *
     * @param stmt 语句节点，包含各种类型的语句AST结构
     */
    private static void visitStmt(BranchNode stmt) {
        List<AstNode> children = stmt.getChildren();

        // ========== 语句类型识别 ==========
        // 通过检查子节点中的关键字Token来判断语句类型
        boolean isIfStmt = children.stream().anyMatch(child -> child.getNodeType() == SynType.IFTK);
        boolean isForStmt = children.stream().anyMatch(child -> child.getNodeType() == SynType.FORTK);
        boolean isBlockStmt = children.stream().anyMatch(child -> child.getNodeType() == SynType.Block);
        boolean isAssignStmt = children.stream().anyMatch(child -> child.getNodeType() == SynType.LVal) &&
                              children.stream().anyMatch(child -> child.getNodeType() == SynType.ASSIGN);

        // ========== Switch语句处理 ==========
        if (stmt instanceof SwitchStmt) {
            SwitchStmt switchStmt = (SwitchStmt) stmt;
            visitExp(switchStmt.getExp());
            
            switchNum++;
            visitStmt(switchStmt.getStmt());
            switchNum--;
            return;
        }

        // ========== While循环处理 ==========
        if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) stmt;
            visitCond(whileStmt.getCond());
            
            loopNum++;
            visitStmt(whileStmt.getStmt());
            loopNum--;
            return;
        }

        // ========== Do-While循环处理 ==========
        if (stmt instanceof DoWhileStmt) {
            DoWhileStmt doWhileStmt = (DoWhileStmt) stmt;
            
            loopNum++;
            visitStmt(doWhileStmt.getStmt());
            loopNum--;
            
            visitCond(doWhileStmt.getCond());
            return;
        }

        // ========== Goto语句处理 ==========
        if (stmt instanceof GotoStmt) {
            return;
        }

        // ========== Block语句处理 ==========
        // 文法：Stmt → Block
        // AST结构：Stmt -> Block -> { BlockItem* }
        if (isBlockStmt) {
            for (AstNode child : children) {
                if (child.getNodeType() == SynType.Block) {
                    // 创建新的符号表作用域，因为Block会引入新的作用域
                    SymbolManager.createSonSymbolTable();
                    visitBlock((BranchNode) child);
                    // 退出作用域，销毁局部变量
                    SymbolManager.goToFatherSymbolTable();
                }
            }
            return;
        }

        // ========== For循环语句处理 ==========
        // ForLoopStmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        if (isForStmt) {
            ForLoopStmt forLoopStmt = (ForLoopStmt) stmt;
            ForStmt initStmt = (ForStmt) forLoopStmt.getInitStmt();
            ForStmt changeStmt = (ForStmt) forLoopStmt.getChangeStmt();
            Cond cond = (Cond) forLoopStmt.getCond();
            Stmt bodyStmt = (Stmt) forLoopStmt.getBody();

            if (initStmt != null) {
                visitForStmt(initStmt);
            }
            if (changeStmt != null) {
                visitForStmt(changeStmt);
            }
            if (cond != null) {
                visitCond(cond);
            }
            loopNum = loopNum + 1;
            visitStmt(bodyStmt);
            loopNum = loopNum - 1;
            return;
        }

        // ========== If条件语句处理 ==========
        // 文法：Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        // AST结构：Stmt -> 'if' '(' Cond ')' Stmt ['else' Stmt]
        if (isIfStmt) {
            for (AstNode child : children) {
                if (child.getNodeType() == SynType.Cond) {
                    // 处理if条件表达式，进行语义检查
                    visitCond((BranchNode) child);
                } else if (child.getNodeType() == SynType.Stmt) {
                    // 递归处理if分支和else分支的语句
                    // 可能有两个Stmt子节点：then分支和else分支
                    visitStmt((BranchNode) child);
                }
            }
            return;
        }

        // ========== 其他语句类型的通用处理 ==========
        // 包括：赋值语句、表达式语句、return语句、printf语句、break/continue语句
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.LVal) {
                // 处理左值表达式（出现在赋值语句中）
                // 文法：Stmt → LVal '=' Exp ';'
                if (isAssignStmt || stmt instanceof IncStmt || stmt instanceof DecStmt) {
                    // 检查是否对常量进行赋值（错误类型h）
                    checkConstAssignment((BranchNode) child);
                }
                // 对左值进行语义分析，检查标识符是否已定义
                visitLVal((BranchNode) child);
            } else if (child.getNodeType() == SynType.Exp) {
                // 处理表达式
                // 可能出现在：赋值语句的右侧、表达式语句、return语句
                visitExp((BranchNode) child);
            } else if (child.getNodeType() == SynType.Stmt) {
                // 处理嵌套的语句（如if/else中的嵌套语句）
                visitStmt((BranchNode) child);
            } else if (child.getNodeType() == SynType.Block) {
                // 处理嵌套的Block节点（如if/for语句中的Block）
                // 需要创建新的符号表作用域
                SymbolManager.createSonSymbolTable();
                visitBlock((BranchNode) child);
                SymbolManager.goToFatherSymbolTable();
            } else if (child.getNodeType() == SynType.RETURNTK) {
                // 处理return语句
                // 文法：Stmt → 'return' [Exp] ';'
                // 检查返回值类型是否与函数声明匹配（错误类型g）
                checkReturnType(stmt);
            } else if (child.getNodeType() == SynType.PRINTFTK) {
                // 处理printf语句
                // 文法：Stmt → 'printf' '(' StringConst { ',' Exp} ')' ';'
                // 检查格式字符串与参数数量是否匹配（错误类型l）
                checkPrintfFormat(stmt);
            } else if (child.getNodeType() == SynType.CONTINUETK) {
                // 处理continue语句
                // 检查是否在循环中使用（错误类型m）
                if (loopNum < 1) {
                    TokenNode tokenNode = (TokenNode) child;
                    ErrorManager.AddError(Error.createError(ErrorType.BREAK_CONTINUE_ERR, tokenNode.getLineNumber()));
                }
            } else if (child.getNodeType() == SynType.BREAKTK) {
                // 处理break语句
                // 检查是否在循环或switch中使用（错误类型m）
                if (loopNum < 1 && switchNum < 1) {
                    TokenNode tokenNode = (TokenNode) child;
                    ErrorManager.AddError(Error.createError(ErrorType.BREAK_CONTINUE_ERR, tokenNode.getLineNumber()));
                }
            }
        }
    }

    /**
     * 检查常量赋值错误
     */
    private static void checkConstAssignment(BranchNode lVal) {
        List<AstNode> children = lVal.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.IDENFR) {
                TokenNode identifierNode = (TokenNode) child;
                String name = identifierNode.getContent();
                int lineNumber = identifierNode.getLineNumber();

                Symbol symbol = SymbolManager.getSymbol(name);
                if (symbol != null && (symbol.getSymbolType() == SymbolType.CONST_INT ||
                                     symbol.getSymbolType() == SymbolType.CONST_INT_ARRAY)) {
                    ErrorManager.AddError(Error.createError(ErrorType.EDIT_CONST_VALUE, lineNumber));
                }
                break;
            }
        }
    }

    /**
     * 检查返回值类型匹配
     */
    private static void checkReturnType(BranchNode stmt) {
        ReturnStmt returnStmt = (ReturnStmt) stmt;
        int lineNumber = returnStmt.getLineNumber();
        Exp exp = returnStmt.getVal();

        // 检查f
        if (voidFunc && exp != null) {
            ErrorManager.AddError(Error.createError(ErrorType.VOID_RETURN, lineNumber));
            return;
        }

        if (exp != null) {
            visitExp(exp);
        }
    }

    /**
     * 检查printf格式字符串匹配
     */
    private static void checkPrintfFormat(BranchNode stmt) {
        List<AstNode> children = stmt.getChildren();
        String formatString = null;
        int paramCount = 0;
        int lineNumber = -1;

        for (AstNode child : children) {
            if (child.getNodeType() == SynType.PRINTFTK) {
                TokenNode printfToken = (TokenNode) child;
                lineNumber = printfToken.getLineNumber();
            } else if (child.getNodeType() == SynType.STRCON) {
                TokenNode stringToken = (TokenNode) child;
                formatString = stringToken.getContent();
            } else if (child.getNodeType() == SynType.Exp) {
                paramCount++;
            }
        }

        if (formatString != null && lineNumber != -1) {
            // 计算格式字符串中%d的数量
            int formatCount = 0;
            for (int i = 0; i < formatString.length() - 1; i++) {
                if (formatString.charAt(i) == '%' && formatString.charAt(i + 1) == 'd') {
                    formatCount++;
                }
            }

            if (formatCount != paramCount) {
                ErrorManager.AddError(Error.createError(ErrorType.PRINTF_PARAM_NUM_ERR, lineNumber));
            }
        }
    }

    /**
     * 访问左值节点
     *
     * @param lVal LVal节点
     */
    private static void visitLVal(BranchNode lVal) {
        List<AstNode> children = lVal.getChildren();
        String name = null;
        int lineNumber = -1;

        for (AstNode child : children) {
            if (child.getNodeType() == SynType.IDENFR) {
                TokenNode identifierNode = (TokenNode) child;
                name = identifierNode.getContent();
                lineNumber = identifierNode.getLineNumber();

                // 检查标识符是否已定义
                Symbol symbol = SymbolManager.getSymbol(name);
                if (symbol == null) {
                    ErrorManager.AddError(Error.createError(ErrorType.NAME_UNDEFINED, lineNumber));
                }
            } else if (child.getNodeType() == SynType.Exp) {
                visitExp((BranchNode) child);
            }
        }
    }

    /**
     * 访问表达式节点
     *
     * @param exp Exp节点
     */
    private static void visitExp(BranchNode exp) {
        List<AstNode> children = exp.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.AddExp) {
                visitAddExp((AddExp) child);
            }
        }
    }

    /**
     * 访问加法表达式
     *
     * @param addExp 加法表达式节点
     */
    private static void visitAddExp(AddExp addExp) {
        if (addExp.getMulExp() != null) {
            visitMulExp(addExp.getMulExp());
        }

        if (addExp.getAddExp() != null) {
            visitAddExp(addExp.getAddExp());
        }
    }

    /**
     * 访问乘法表达式
     *
     * @param mulExp 乘法表达式节点
     */
    private static void visitMulExp(MulExp mulExp) {
        if (mulExp.getUnaryExp() != null) {
            visitUnaryExp(mulExp.getUnaryExp());
        }

        if (mulExp.getMulExp() != null) {
            visitMulExp(mulExp.getMulExp());
        }
    }

    /**
     * 访问一元表达式
     *
     * @param unaryExp 一元表达式节点
     */
    private static void visitUnaryExp(UnaryExp unaryExp) {
        if (unaryExp instanceof FuncCallUnaryExp) {
            // 函数调用类型
            visitFuncCallUnaryExp((FuncCallUnaryExp) unaryExp);
        } else if (unaryExp instanceof PrimaryUnaryExp) {
            // PrimaryUnaryExp只是包装器，直接处理其内部的PrimaryExp
            PrimaryUnaryExp primaryUnaryExp = (PrimaryUnaryExp) unaryExp;
            visitPrimaryExp(primaryUnaryExp.getPrimaryExp());
        } else {
            // 处理普通UnaryExp（UnaryOp + UnaryExp）
            List<AstNode> children = unaryExp.getChildren();
            for (AstNode child : children) {
                if (child.getNodeType() == SynType.UnaryExp) {
                    visitUnaryExp((UnaryExp) child);
                }
                // UnaryOp不需要特殊处理，只是操作符
            }
        }
    }

    /**
     * 访问基本表达式
     *
     * @param primaryExp 基本表达式节点
     */
    private static void visitPrimaryExp(PrimaryExp primaryExp) {
        if (primaryExp.getExp() != null) {
            visitExp(primaryExp.getExp());
        } else if (primaryExp.getLVal() != null) {
            visitLVal(primaryExp.getLVal());
        }
    }

    /**
     * 访问函数调用表达式
     *
     * @param funcCallUnaryExp 函数调用表达式节点
     */
    private static void visitFuncCallUnaryExp(FuncCallUnaryExp funcCallUnaryExp) {
        String funcName = funcCallUnaryExp.getFunctionName();
        int lineNumber = funcCallUnaryExp.getLineNumber();

        // 特判getint
        if (funcName.equals("getint")) {
            for (AstNode child : funcCallUnaryExp.getChildren()) {
                if (child.getNodeType() == SynType.FuncRParams) {
                    ErrorManager.AddError(Error.createError(ErrorType.FUNC_PARAM_NUM_ERR, lineNumber));
                    return;
                }
            }
            return;
        }

        // 检查函数是否已定义
        Symbol symbol = SymbolManager.getSymbol(funcName);
        if (symbol == null) {
            ErrorManager.AddError(Error.createError(ErrorType.NAME_UNDEFINED, lineNumber));
            return;
        }

        // 检查是否为函数符号
        if (!(symbol instanceof FuncSymbol)) {
            ErrorManager.AddError(Error.createError(ErrorType.NAME_UNDEFINED, lineNumber));
            return;
        }

        FuncSymbol funcSymbol = (FuncSymbol) symbol;

        // 处理函数参数并检查参数匹配
        List<AstNode> children = funcCallUnaryExp.getChildren();
        ArrayList<SymbolType> actualParamTypes = new ArrayList<>();

        for (AstNode child : children) {
            if (child.getNodeType() == SynType.FuncRParams) {
                FuncRParams funcRParams = (FuncRParams) child;
                visitFuncRParams(funcRParams);

                // 收集实际参数类型
                for (Exp exp : funcRParams.getExps()) {
                    actualParamTypes.add(getExpType(exp));
                }

                break;
            }
        }

        // 检查参数数量
        ArrayList<SymbolType> formalParamTypes = funcSymbol.getFormalParamList();
        if (actualParamTypes.size() != formalParamTypes.size()) {
            ErrorManager.AddError(Error.createError(ErrorType.FUNC_PARAM_NUM_ERR, lineNumber));
            return;
        }

        // 检查参数类型
        for (int i = 0; i < actualParamTypes.size(); i++) {
            SymbolType actualType = actualParamTypes.get(i);
            SymbolType formalType = formalParamTypes.get(i);

            if (!isTypeCompatible(actualType, formalType)) {
                ErrorManager.AddError(Error.createError(ErrorType.FUNC_PARAM_TYPE_ERR, lineNumber));
                break;
            }
        }
    }

    /**
     * 访问函数实参列表
     *
     * @param funcRParams 函数实参列表节点
     */
    private static void visitFuncRParams(FuncRParams funcRParams) {
        for (Exp exp : funcRParams.getExps()) {
            visitExp(exp);
        }
    }

    /**
     * 访问条件表达式节点
     * Cond → LOrExp
     * @param cond 条件表达式节点
     */
    private static void visitCond(BranchNode cond) {
        List<AstNode> children = cond.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.LOrExp) {
                visitLOrExp((BranchNode) child);
            }
        }
    }

    /**
     * 访问逻辑或表达式节点
     *
     * @param lOrExp 逻辑或表达式节点
     */
    private static void visitLOrExp(BranchNode lOrExp) {
        List<AstNode> children = lOrExp.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.LAndExp) {
                visitLAndExp((BranchNode) child);
            } else if (child.getNodeType() == SynType.LOrExp) {
                visitLOrExp((BranchNode) child);
            }
        }
    }

    /**
     * 访问逻辑与表达式节点
     *
     * @param lAndExp 逻辑与表达式节点
     */
    private static void visitLAndExp(BranchNode lAndExp) {
        List<AstNode> children = lAndExp.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.EqExp) {
                visitEqExp((BranchNode) child);
            } else if (child.getNodeType() == SynType.LAndExp) {
                visitLAndExp((BranchNode) child);
            }
        }
    }

    /**
     * 访问相等表达式节点
     *
     * @param eqExp 相等表达式节点
     */
    private static void visitEqExp(BranchNode eqExp) {
        List<AstNode> children = eqExp.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.RelExp) {
                visitRelExp((BranchNode) child);
            } else if (child.getNodeType() == SynType.EqExp) {
                visitEqExp((BranchNode) child);
            }
        }
    }

    /**
     * 访问关系表达式节点
     *
     * @param relExp 关系表达式节点
     */
    private static void visitRelExp(BranchNode relExp) {
        List<AstNode> children = relExp.getChildren();
        for (AstNode child : children) {
            if (child.getNodeType() == SynType.AddExp) {
                visitAddExp((AddExp) child);
            } else if (child.getNodeType() == SynType.RelExp) {
                visitRelExp((BranchNode) child);
            }
        }
    }

    /**
     * ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
     * @param forInput For语句节点
     */
    private static void visitForStmt(BranchNode forInput) {
        ForStmt forStmt = (ForStmt) forInput;
        ArrayList<LVal> lVals = forStmt.getLVals();
        ArrayList<Exp> exps = forStmt.getExps();

        for (int i = 0; i < lVals.size(); i++) {
            LVal lVal = lVals.get(i);
            Exp exp = exps.get(i);
            int lineNumber = lVal.getLineNumber();

            TokenNode indent = lVal.getIdentifier();
            String name = indent.getContent();
            Symbol sym = SymbolManager.getSymbol(name);

            if (sym != null) {
                if (sym.getSymbolType() == SymbolType.CONST_INT || sym.getSymbolType() == SymbolType.CONST_INT_ARRAY) {
                    ErrorManager.AddError(Error.createError(ErrorType.EDIT_CONST_VALUE, lineNumber));
                }
            }

            visitLVal(lVal);
            visitExp(exp);
        }
    }

    /**
     * 获取基本类型字符串
     *
     * @param bType 基本类型节点
     * @return 类型字符串
     */
    private static String getBTypeString(BType bType) {
        return bType.getTypeName();
    }

    /**
     * 获取函数类型字符串
     *
     * @param funcType 函数类型节点
     * @return 函数类型字符串
     */
    private static String getFuncTypeString(FuncType funcType) {
        return funcType.getReturnType();
    }
}