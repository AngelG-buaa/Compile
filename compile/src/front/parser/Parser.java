package front.parser;

import error.Error;
import error.ErrorManager;
import error.ErrorType;
import front.lexer.Token;
import front.parser.syntax.*;
import front.parser.syntax.decl.*;
import front.parser.syntax.exp.*;
import front.parser.syntax.exp.Number;
import front.parser.syntax.func.*;
import front.parser.syntax.stmt.*;
import utils.TokenToNode;

import java.util.ArrayList;

public class Parser {
    private ArrayList<Token> input_tokens;
    private ArrayList<TokenNode> tokens;
    private AstNode root;
    private int currentIndex;

    public void init(ArrayList<Token> input_tokens) {
        this.input_tokens = input_tokens;
        this.tokens = TokenToNode.convert(input_tokens);
        this.root = null;
        this.currentIndex = 0;
    }

    public void run() {
        root = parseCompUnit();
    }

    public BranchNode getRoot() {
        return (BranchNode) root;
    }

    // --------------------------辅助方法--------------------------
    
    /**
     * 获取当前token
     */
    private TokenNode getCurrentToken() {
        if (currentIndex >= tokens.size()) {
            return null;
        }
        return tokens.get(currentIndex);
    }

    /**
     * 获取当前token的类型
     */
    private SynType getCurrentTokenType() {
        TokenNode token = getCurrentToken();
        return token != null ? token.getNodeType() : SynType.EOF;
    }

    /**
     * 获取指定偏移位置的token类型
     */
    private SynType getTokenType(int offset) {
        int index = currentIndex + offset;
        if (index >= tokens.size() || index < 0) {
            return SynType.EOF;
        }
        return tokens.get(index).getNodeType();
    }

    /**
     * 消费一个token
     */
    private TokenNode consumeToken() {
        if (currentIndex < tokens.size()) {
            return tokens.get(currentIndex++);
        }
        return null;
    }

    /**
     * 期望并消费指定类型的token
     */
    private TokenNode expectToken(SynType expectedType, ErrorType errorType) {
        if (getCurrentTokenType() == expectedType) {
            return consumeToken();
        } else {
            // 错误处理：缺少期望的token
            int lineNumber;

            // 对于特定的缺失错误，行号应该是前一个非终结符所在行号
            if (errorType == ErrorType.MISS_SEMICN ||
                    errorType == ErrorType.MISS_RPARENT ||
                    errorType == ErrorType.MISS_RBRACK) {
                // 报错行号为前一个非终结符所在行号
                lineNumber = (currentIndex > 0) ? tokens.get(currentIndex - 1).getLineNumber() : 1;
            } else {
                // 其他错误使用当前token行号
                lineNumber = getCurrentToken() != null ? getCurrentToken().getLineNumber() :
                        (currentIndex > 0 ? tokens.get(currentIndex - 1).getLineNumber() : 1);
            }

            if (errorType != null) {
                ErrorManager.AddError(Error.createError(errorType, lineNumber));
            }

            // 创建一个虚拟的token节点，但不消费当前token
            return new TokenNode(expectedType, "", lineNumber);
        }
    }

    // --------------------------递归下降解析方法--------------------------

    /**
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     */
    private BranchNode parseCompUnit() {
        BranchNode compUnit = new BranchNode(SynType.CompUnit);
        
        while (currentIndex < tokens.size()) {
            SynType currentType = getCurrentTokenType();
            
            if (currentType == SynType.CONSTTK) {
                // 常量声明
                compUnit.appendChild(parseDecl());
            } else if (currentType == SynType.VOIDTK) {
                // void函数定义
                compUnit.appendChild(parseFuncDef());
            } else if (currentType == SynType.INTTK) {
                // 需要向前来看区分
                if (getTokenType(1) == SynType.MAINTK) {
                    // 主函数
                    compUnit.appendChild(parseMainFuncDef());
                    break; // 主函数是最后一个
                } else if (getTokenType(2) == SynType.LPARENT) {
                    // int函数定义
                    compUnit.appendChild(parseFuncDef());
                } else {
                    // 变量声明
                    compUnit.appendChild(parseDecl());
                }
            } else if (currentType == SynType.STATICTK) {
                // static变量声明
                compUnit.appendChild(parseDecl());
            } else {
                break;
            }
        }
        
        return compUnit;
    }

    /**
     * Decl → ConstDecl | VarDecl
     */
    private Decl parseDecl() {
        Decl decl = new Decl();
        
        if (getCurrentTokenType() == SynType.CONSTTK) {
            decl.appendChild(parseConstDecl());
        } else {
            decl.appendChild(parseVarDecl());
        }
        
        return decl;
    }

    /**
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     */
    private ConstDecl parseConstDecl() {
        ConstDecl constDecl = new ConstDecl();

        // 'const'
        constDecl.appendChild(expectToken(SynType.CONSTTK, null));
        constDecl.appendChild(parseBType());
        constDecl.appendChild(parseConstDef());

        // 解析多个ConstDef
        while (getCurrentTokenType() == SynType.COMMA) {
            constDecl.appendChild(consumeToken()); // 消费逗号
            constDecl.appendChild(parseConstDef());
        }

        // ';' - 错误类型 i: 缺少分号
        constDecl.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));

        return constDecl;
    }

    /**
     * BType → 'int'
     */
    private BType parseBType() {
        BType bType = new BType();
        bType.appendChild(expectToken(SynType.INTTK, null));
        return bType;
    }

    /**
     * ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
     */
    private ConstDef parseConstDef() {
        ConstDef constDef = new ConstDef();

        // Ident
        constDef.appendChild(expectToken(SynType.IDENFR, null));

        // 可选的数组维度
        if (getCurrentTokenType() == SynType.LBRACK) {
            constDef.appendChild(consumeToken()); // 消费 '['
            constDef.appendChild(parseConstExp());
            // ']' - 错误类型 k: 缺少右中括号
            constDef.appendChild(expectToken(SynType.RBRACK, ErrorType.MISS_RBRACK));
        }

        constDef.appendChild(expectToken(SynType.ASSIGN, null));

        // ConstInitVal
        constDef.appendChild(parseConstInitVal());

        return constDef;
    }

    /**
     * ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
     */
    private ConstInitVal parseConstInitVal() {
        ConstInitVal constInitVal = new ConstInitVal();
        
        if (getCurrentTokenType() == SynType.LBRACE) {
            // '{' [ ConstExp { ',' ConstExp } ] '}'
            constInitVal.appendChild(consumeToken()); // '{'
            
            if (getCurrentTokenType() != SynType.RBRACE) {
                constInitVal.appendChild(parseConstExp());
                
                while (getCurrentTokenType() == SynType.COMMA) {
                    constInitVal.appendChild(consumeToken()); // ','
                    constInitVal.appendChild(parseConstExp());
                }
            }
            
            constInitVal.appendChild(expectToken(SynType.RBRACE, null));
        } else {
            // ConstExp
            constInitVal.appendChild(parseConstExp());
        }
        
        return constInitVal;
    }

    /**
     * VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
     */
    private VarDecl parseVarDecl() {
        VarDecl varDecl = new VarDecl();

        // [ 'static' ]
        if (getCurrentTokenType() == SynType.STATICTK) {
            varDecl.appendChild(consumeToken());
        }

        // BType
        varDecl.appendChild(parseBType());

        // VarDef
        varDecl.appendChild(parseVarDef());

        // { ',' VarDef }
        while (getCurrentTokenType() == SynType.COMMA) {
            varDecl.appendChild(consumeToken()); // ','
            varDecl.appendChild(parseVarDef());
        }

        // ';' - 错误类型 i: 缺少分号
        varDecl.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));

        return varDecl;
    }

    /**
     * VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
     */
    private VarDef parseVarDef() {
        VarDef varDef = new VarDef();

        // Ident
        varDef.appendChild(expectToken(SynType.IDENFR, null));

        // [ '[' ConstExp ']' ]
        if (getCurrentTokenType() == SynType.LBRACK) {
            varDef.appendChild(consumeToken()); // '['
            varDef.appendChild(parseConstExp());
            // ']' - 错误类型 k: 缺少右中括号
            varDef.appendChild(expectToken(SynType.RBRACK, ErrorType.MISS_RBRACK));
        }

        // [ '=' InitVal ]
        if (getCurrentTokenType() == SynType.ASSIGN) {
            varDef.appendChild(consumeToken()); // '='
            varDef.appendChild(parseInitVal());
        }

        return varDef;
    }

    /**
     * InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
     */
    private InitVal parseInitVal() {
        InitVal initVal = new InitVal();
        
        if (getCurrentTokenType() == SynType.LBRACE) {
            // '{' [ Exp { ',' Exp } ] '}'
            initVal.appendChild(consumeToken()); // '{'
            
            if (getCurrentTokenType() != SynType.RBRACE) {
                initVal.appendChild(parseExp());
                
                while (getCurrentTokenType() == SynType.COMMA) {
                    initVal.appendChild(consumeToken()); // ','
                    initVal.appendChild(parseExp());
                }
            }
            
            initVal.appendChild(expectToken(SynType.RBRACE, null));
        } else {
            // Exp
            initVal.appendChild(parseExp());
        }
        
        return initVal;
    }

    /**
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     */
    private FuncDef parseFuncDef() {
        FuncDef funcDef = new FuncDef();

        // FuncType
        funcDef.appendChild(parseFuncType());

        // Ident
        funcDef.appendChild(expectToken(SynType.IDENFR, null));

        // '('
        funcDef.appendChild(expectToken(SynType.LPARENT, null));

        // [FuncFParams]
        if (getCurrentTokenType() == SynType.INTTK) {
            funcDef.appendChild(parseFuncFParams());
        }

        // ')' - 错误类型 j: 缺少右小括号
        funcDef.appendChild(expectToken(SynType.RPARENT, ErrorType.MISS_RPARENT));

        // Block
        funcDef.appendChild(parseBlock());

        return funcDef;
    }

    /**
     * MainFuncDef → 'int' 'main' '(' ')' Block
     */
    private MainFuncDef parseMainFuncDef() {
        MainFuncDef mainFuncDef = new MainFuncDef();

        // 'int'
        mainFuncDef.appendChild(expectToken(SynType.INTTK, null));

        // 'main'
        mainFuncDef.appendChild(expectToken(SynType.MAINTK, null));

        // '('
        mainFuncDef.appendChild(expectToken(SynType.LPARENT, null));

        // ')' - 错误类型 j: 缺少右小括号
        mainFuncDef.appendChild(expectToken(SynType.RPARENT, ErrorType.MISS_RPARENT));

        // Block
        mainFuncDef.appendChild(parseBlock());

        return mainFuncDef;
    }

    /**
     * FuncType → 'void' | 'int'
     */
    private FuncType parseFuncType() {
        FuncType funcType = new FuncType();
        
        SynType currentType = getCurrentTokenType();
        if (currentType == SynType.VOIDTK || currentType == SynType.INTTK) {
            funcType.appendChild(consumeToken());
        }
        
        return funcType;
    }

    /**
     * FuncFParams → FuncFParam { ',' FuncFParam }
     */
    private FuncFParams parseFuncFParams() {
        FuncFParams funcFParams = new FuncFParams();

        // FuncFParam
        funcFParams.appendChild(parseFuncFParam());

        // { ',' FuncFParam }
        while (getCurrentTokenType() == SynType.COMMA) {
            funcFParams.appendChild(consumeToken()); // ','
            funcFParams.appendChild(parseFuncFParam());
        }

        return funcFParams;
    }

    /**
     * FuncFParam → BType Ident ['[' ']']
     */
    private FuncFParam parseFuncFParam() {
        FuncFParam funcFParam = new FuncFParam();

        // BType
        funcFParam.appendChild(parseBType());

        // Ident
        funcFParam.appendChild(expectToken(SynType.IDENFR, null));

        // ['[' ']']
        if (getCurrentTokenType() == SynType.LBRACK) {
            funcFParam.appendChild(consumeToken()); // '['
            // ']' - 错误类型 k: 缺少右中括号
            funcFParam.appendChild(expectToken(SynType.RBRACK, ErrorType.MISS_RBRACK));
        }

        return funcFParam;
    }

    /**
     * Block → '{' { BlockItem } '}'
     */
    private Block parseBlock() {
        Block block = new Block();
        
        // '{'
        block.appendChild(expectToken(SynType.LBRACE, null));
        
        // { BlockItem }
        while (getCurrentTokenType() != SynType.RBRACE && getCurrentTokenType() != SynType.EOF) {
            block.appendChild(parseBlockItem());
        }
        
        // '}'
        block.appendChild(expectToken(SynType.RBRACE, null));
        
        return block;
    }

    /**
     * BlockItem → Decl | Stmt
     */
    private BlockItem parseBlockItem() {
        BlockItem blockItem = new BlockItem();
        
        SynType currentType = getCurrentTokenType();
        if (currentType == SynType.CONSTTK || currentType == SynType.INTTK || currentType == SynType.STATICTK) {
            blockItem.appendChild(parseDecl());
        } else {
            blockItem.appendChild(parseStmt());
        }
        
        return blockItem;
    }

    /**
     * Stmt → LVal '=' Exp ';' | [Exp] ';' | Block | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] 
     *      | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt | 'break' ';' | 'continue' ';' 
     *      | 'return' [Exp] ';' | 'printf' '(' StringConst { ',' Exp} ')' ';'
     */
    private Stmt parseStmt() {
        SynType currentType = getCurrentTokenType();
        
        switch (currentType) {
            case LBRACE:
                // Block
                BlockStmt blockStmt = new BlockStmt();
                blockStmt.appendChild(parseBlock());
                return blockStmt;
                
            case IFTK:
                return parseIfStmt();
                
            case FORTK:
                return parseForLoopStmt();
                
            case BREAKTK:
                return parseBreakStmt();
                
            case CONTINUETK:
                return parseContinueStmt();
                
            case RETURNTK:
                return parseReturnStmt();
                
            case PRINTFTK:
                return parsePrintStmt();

            case REPEATTK:
                return parseRepeatStmt();
                
            case IDENFR:
                // 需要向前看判断是赋值语句还是表达式语句
                return parseAssignOrExpStmt();
                
            default:
                // [Exp] ';'
                return parseExpStmt();
        }
    }

    /**
     * 解析赋值语句或表达式语句
     */
    private Stmt parseAssignOrExpStmt() {
        // 向前看，判断是否为赋值语句
        int lookAhead = 0;
        while (getTokenType(lookAhead) != SynType.ASSIGN &&
                getTokenType(lookAhead) != SynType.SEMICN &&
                getTokenType(lookAhead) != SynType.EOF) {
            lookAhead++;
        }

        if (getTokenType(lookAhead) == SynType.ASSIGN) {
            // 赋值语句: LVal '=' Exp ';'
            AssignStmt assignStmt = new AssignStmt();
            assignStmt.appendChild(parseLVal());
            assignStmt.appendChild(expectToken(SynType.ASSIGN, null));
            assignStmt.appendChild(parseExp());
            // ';' - 错误类型 i: 缺少分号
            assignStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));
            return assignStmt;
        } else {
            // 表达式语句
            return parseExpStmt();
        }
    }

    /**
     * [Exp] ';'
     */
    private ExpStmt parseExpStmt() {
        ExpStmt expStmt = new ExpStmt();

        if (getCurrentTokenType() != SynType.SEMICN) {
            expStmt.appendChild(parseExp());
        }

        // ';' - 错误类型 i: 缺少分号
        expStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));
        return expStmt;
    }

    /**
     * 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     */
    private IfStmt parseIfStmt() {
        IfStmt ifStmt = new IfStmt();

        // 'if'
        ifStmt.appendChild(expectToken(SynType.IFTK, null));

        // '('
        ifStmt.appendChild(expectToken(SynType.LPARENT, null));

        // Cond
        ifStmt.appendChild(parseCond());

        // ')' - 错误类型 j: 缺少右小括号
        ifStmt.appendChild(expectToken(SynType.RPARENT, ErrorType.MISS_RPARENT));

        // Stmt
        ifStmt.appendChild(parseStmt());

        // [ 'else' Stmt ]
        if (getCurrentTokenType() == SynType.ELSETK) {
            ifStmt.appendChild(consumeToken()); // 'else'
            ifStmt.appendChild(parseStmt());
        }

        return ifStmt;
    }

    /**
     * 'repeat' Stmt 'until' '(' Cond ')' ';'
     */
    private RepeatStmt parseRepeatStmt() {
        RepeatStmt repeatStmt = new RepeatStmt();
        repeatStmt.appendChild(expectToken(SynType.REPEATTK, null));
        repeatStmt.appendChild(parseStmt());
        repeatStmt.appendChild(expectToken(SynType.UNTILTK, null));
        repeatStmt.appendChild(expectToken(SynType.LPARENT, null));
        repeatStmt.appendChild(parseCond());
        repeatStmt.appendChild(expectToken(SynType.RPARENT, null));
        repeatStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));
        return repeatStmt;
    }

    /**
     * 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     */
    private ForLoopStmt parseForLoopStmt() {
        ForLoopStmt forStmt = new ForLoopStmt();

        // 'for'
        forStmt.appendChild(expectToken(SynType.FORTK, null));

        // '('
        forStmt.appendChild(expectToken(SynType.LPARENT, null));

        // [ForStmt]
        if (getCurrentTokenType() != SynType.SEMICN) {
            forStmt.appendChild(parseForStmt());
        }

        // ';' - 错误类型 i: 缺少分号
        forStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));

        // [Cond]
        if (getCurrentTokenType() != SynType.SEMICN) {
            forStmt.appendChild(parseCond());
        }

        // ';' - 错误类型 i: 缺少分号
        forStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));

        // [ForStmt]
        if (getCurrentTokenType() != SynType.RPARENT) {
            forStmt.appendChild(parseForStmt());
        }

        // ')' - 错误类型 j: 缺少右小括号
        forStmt.appendChild(expectToken(SynType.RPARENT, ErrorType.MISS_RPARENT));

        // Stmt
        forStmt.appendChild(parseStmt());

        return forStmt;
    }

    /**
     * ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
     */
    private ForStmt parseForStmt() {
        ForStmt forStmt = new ForStmt();
        
        // LVal '=' Exp
        forStmt.appendChild(parseLVal());
        forStmt.appendChild(expectToken(SynType.ASSIGN, null));
        forStmt.appendChild(parseExp());
        
        // { ',' LVal '=' Exp }
        while (getCurrentTokenType() == SynType.COMMA) {
            forStmt.appendChild(consumeToken()); // ','
            forStmt.appendChild(parseLVal());
            forStmt.appendChild(expectToken(SynType.ASSIGN, null));
            forStmt.appendChild(parseExp());
        }
        
        return forStmt;
    }

    /**
     * 'break' ';'
     */
    private BreakStmt parseBreakStmt() {
        BreakStmt breakStmt = new BreakStmt();
        breakStmt.appendChild(expectToken(SynType.BREAKTK, null));
        breakStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));
        return breakStmt;
    }

    /**
     * 'continue' ';'
     */
    private ContinueStmt parseContinueStmt() {
        ContinueStmt continueStmt = new ContinueStmt();
        continueStmt.appendChild(expectToken(SynType.CONTINUETK, null));
        continueStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));
        return continueStmt;
    }

    /**
     * 'return' [Exp] ';'
     */
    private ReturnStmt parseReturnStmt() {
        ReturnStmt returnStmt = new ReturnStmt();
        
        // 'return'
        returnStmt.appendChild(expectToken(SynType.RETURNTK, null));
        
        // [Exp]
        if (getCurrentTokenType() != SynType.SEMICN) {
            returnStmt.appendChild(parseExp());
        }
        
        // ';'
        returnStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));
        
        return returnStmt;
    }

    /**
     * 'printf' '(' StringConst { ',' Exp } ')' ';'
     */
    private PrintStmt parsePrintStmt() {
        PrintStmt printStmt = new PrintStmt();
        
        // 'printf'
        printStmt.appendChild(expectToken(SynType.PRINTFTK, null));
        
        // '('
        printStmt.appendChild(expectToken(SynType.LPARENT, null));
        
        // StringConst
        printStmt.appendChild(expectToken(SynType.STRCON, null));
        
        // {','Exp}
        while (getCurrentTokenType() == SynType.COMMA) {
            printStmt.appendChild(consumeToken()); // ','
            printStmt.appendChild(parseExp());
        }
        
        // ')'
        printStmt.appendChild(expectToken(SynType.RPARENT, ErrorType.MISS_RPARENT));
        
        // ';'
        printStmt.appendChild(expectToken(SynType.SEMICN, ErrorType.MISS_SEMICN));
        
        return printStmt;
    }

    /**
     * Exp → AddExp
     */
    private Exp parseExp() {
        Exp exp = new Exp();
        exp.appendChild(parseAddExp());
        return exp;
    }

    /**
     * Cond → LOrExp
     */
    private Cond parseCond() {
        Cond cond = new Cond();
        cond.appendChild(parseLOrExp());
        return cond;
    }

    /**
     * LVal → Ident ['[' Exp ']']
     */
    private LVal parseLVal() {
        LVal lVal = new LVal();
        
        // Ident
        lVal.appendChild(expectToken(SynType.IDENFR, null));
        
        // ['[' Exp ']']
        if (getCurrentTokenType() == SynType.LBRACK) {
            lVal.appendChild(consumeToken()); // '['
            lVal.appendChild(parseExp());
            lVal.appendChild(expectToken(SynType.RBRACK, ErrorType.MISS_RBRACK));
        }
        
        return lVal;
    }

    /**
     * PrimaryExp → '(' Exp ')' | LVal | Number
     */
    private PrimaryExp parsePrimaryExp() {
        PrimaryExp primaryExp = new PrimaryExp();
        
        SynType currentType = getCurrentTokenType();
        if (currentType == SynType.LPARENT) {
            // '(' Exp ')'
            primaryExp.appendChild(consumeToken()); // '('
            primaryExp.appendChild(parseExp());
            primaryExp.appendChild(expectToken(SynType.RPARENT, ErrorType.MISS_RPARENT));
        } else if (currentType == SynType.IDENFR) {
            // LVal
            primaryExp.appendChild(parseLVal());
        } else {
            // Number
            primaryExp.appendChild(parseNumber());
        }
        
        return primaryExp;
    }

    /**
     * Number → IntConst
     */
    private Number parseNumber() {
        Number number = new Number();
        if (getCurrentTokenType() == SynType.INTCON) {
            number.appendChild(expectToken(SynType.INTCON, null));
        } else {
            number.appendChild(expectToken(SynType.HEXCON, null));
        }
        return number;
    }

    /**
     * UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
     */
    private UnaryExp parseUnaryExp() {
        SynType currentType = getCurrentTokenType();
        
        if (currentType == SynType.PLUS || currentType == SynType.MINU || currentType == SynType.NOT) {
            // UnaryOp UnaryExp
            UnaryExp unaryExp = new UnaryExp();
            unaryExp.appendChild(parseUnaryOp());
            unaryExp.appendChild(parseUnaryExp());
            return unaryExp;
        } else if (currentType == SynType.IDENFR && getTokenType(1) == SynType.LPARENT) {
            // Ident '(' [FuncRParams] ')'
            FuncCallUnaryExp funcCallExp = new FuncCallUnaryExp();
            funcCallExp.appendChild(consumeToken()); // Ident
            funcCallExp.appendChild(consumeToken()); // '('
            
            // [FuncRParams]
            if (getCurrentTokenType() != SynType.RPARENT) {
                funcCallExp.appendChild(parseFuncRParams());
            }
            
            funcCallExp.appendChild(expectToken(SynType.RPARENT, ErrorType.MISS_RPARENT));
            return funcCallExp;
        } else {
            // PrimaryExp
            PrimaryUnaryExp primaryUnaryExp = new PrimaryUnaryExp();
            primaryUnaryExp.appendChild(parsePrimaryExp());
            return primaryUnaryExp;
        }
    }

    /**
     * UnaryOp → '+' | '−' | '!'
     */
    private UnaryOp parseUnaryOp() {
        UnaryOp unaryOp = new UnaryOp();
        
        SynType currentType = getCurrentTokenType();
        if (currentType == SynType.PLUS || currentType == SynType.MINU || currentType == SynType.NOT) {
            unaryOp.appendChild(consumeToken());
        }
        
        return unaryOp;
    }

    /**
     * FuncRParams → Exp { ',' Exp }
     */
    private FuncRParams parseFuncRParams() {
        FuncRParams funcRParams = new FuncRParams();
        
        // Exp
        funcRParams.appendChild(parseExp());
        
        // { ',' Exp }
        while (getCurrentTokenType() == SynType.COMMA) {
            funcRParams.appendChild(consumeToken()); // ','
            funcRParams.appendChild(parseExp());
        }
        
        return funcRParams;
    }

    /**
     * MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     */
    private MulExp parseMulExp() {
        MulExp mulExp = new MulExp();
        mulExp.appendChild(parseUnaryExp());
        
        while (true) {
            SynType currentType = getCurrentTokenType();
            if (currentType == SynType.MULT || currentType == SynType.DIV || currentType == SynType.MOD) {
                MulExp newMulExp = new MulExp();
                newMulExp.appendChild(mulExp);
                newMulExp.appendChild(consumeToken()); // 操作符
                newMulExp.appendChild(parseUnaryExp());
                mulExp = newMulExp;
            } else {
                break;
            }
        }
        
        return mulExp;
    }

    /**
     * AddExp → MulExp | AddExp ('+' | '−') MulExp
     */
    private AddExp parseAddExp() {
        AddExp addExp = new AddExp();
        addExp.appendChild(parseMulExp());
        
        while (true) {
            SynType currentType = getCurrentTokenType();
            if (currentType == SynType.PLUS || currentType == SynType.MINU) {
                AddExp newAddExp = new AddExp();
                newAddExp.appendChild(addExp);
                newAddExp.appendChild(consumeToken()); // 操作符
                newAddExp.appendChild(parseMulExp());
                addExp = newAddExp;
            } else {
                break;
            }
        }
        
        return addExp;
    }

    /**
     * RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
     * 左递归消除后: RelExp → AddExp { ('<' | '>' | '<=' | '>=') AddExp }
     */
    private RelExp parseRelExp() {
        RelExp relExp = new RelExp();
        relExp.appendChild(parseAddExp());
        
        while (true) {
            SynType currentType = getCurrentTokenType();
            if (currentType == SynType.LSS || currentType == SynType.GRE || 
                currentType == SynType.LEQ || currentType == SynType.GEQ) {
                RelExp newRelExp = new RelExp();
                newRelExp.appendChild(relExp);
                newRelExp.appendChild(consumeToken()); // 操作符
                newRelExp.appendChild(parseAddExp());
                relExp = newRelExp;
            } else {
                break;
            }
        }
        
        return relExp;
    }

    /**
     * EqExp → RelExp | EqExp ('==' | '!=') RelExp
     * 左递归消除后: EqExp → RelExp { ('==' | '!=') RelExp }
     */
    private EqExp parseEqExp() {
        EqExp eqExp = new EqExp();
        eqExp.appendChild(parseRelExp());
        
        while (true) {
            SynType currentType = getCurrentTokenType();
            if (currentType == SynType.EQL || currentType == SynType.NEQ) {
                EqExp newEqExp = new EqExp();
                newEqExp.appendChild(eqExp);
                newEqExp.appendChild(consumeToken()); // 操作符
                newEqExp.appendChild(parseRelExp());
                eqExp = newEqExp;
            } else {
                break;
            }
        }
        
        return eqExp;
    }

    /**
     * LAndExp → EqExp | LAndExp '&&' EqExp
     * 左递归消除后: LAndExp → EqExp { '&&' EqExp }
     */
    private LAndExp parseLAndExp() {
        LAndExp lAndExp = new LAndExp();
        lAndExp.appendChild(parseEqExp());
        
        while (getCurrentTokenType() == SynType.AND) {
            LAndExp newLAndExp = new LAndExp();
            newLAndExp.appendChild(lAndExp);
            newLAndExp.appendChild(consumeToken()); // '&&'
            newLAndExp.appendChild(parseEqExp());
            lAndExp = newLAndExp;
        }
        
        return lAndExp;
    }

    /**
     * LOrExp → LAndExp | LOrExp '||' LAndExp
     * 左递归消除后: LOrExp → LAndExp { '||' LAndExp }
     */
    private LOrExp parseLOrExp() {
        LOrExp lOrExp = new LOrExp();
        lOrExp.appendChild(parseLAndExp());
        
        while (getCurrentTokenType() == SynType.OR) {
            LOrExp newLOrExp = new LOrExp();
            newLOrExp.appendChild(lOrExp);
            newLOrExp.appendChild(consumeToken()); // '||'
            newLOrExp.appendChild(parseLAndExp());
            lOrExp = newLOrExp;
        }
        
        return lOrExp;
    }

    /**
     * ConstExp → AddExp
     */
    private ConstExp parseConstExp() {
        ConstExp constExp = new ConstExp();
        constExp.appendChild(parseAddExp());
        return constExp;
    }
}
