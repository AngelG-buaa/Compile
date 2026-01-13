package front.parser.syntax;

/**
 * 符号[...]表示方括号内包含的为可选项
 * 符号{...}表示花括号内包含的为可重复 0 次或多次的项
 * 终结符或者是由单引号括起的串，或者是 Ident、IntConst、StringConst 这样的记号
 * 所有类似 'main' 这样的用单引号括起的字符串都是保留的关键字
 */
public enum SynType {
    REPEATTK,
    UNTILTK,
    HEXCON,

    /**
     * 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
     * 1.是否存在Decl 2.是否存在FuncDef
     */
    CompUnit,

    /**
     * 基本类型 BType → 'int'
     */
    BType,

    /**
     * 声明 Decl → ConstDecl | VarDecl // 覆盖两种声明
     * 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     * 变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
     */
    Decl,
    ConstDecl,
    VarDecl,

    /**
     * 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
     *          包含普通变量、一维数组两种情况
     * 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
     *          包含普通常量、一维数组定义
     */
    ConstDef,
    VarDef,

    /**
     * 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
     *          1.常表达式初值
     *          2.一维数组初值
     * 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
     *          1.表达式初值
     *          2.一维数组初值
     */
    ConstInitVal,
    InitVal,

    /**
     * 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     *          1.无形参 2.有形参
     * 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
     * 函数类型 FuncType → 'void' | 'int' // 覆盖两种类型的函数
     */
    FuncDef,
    MainFuncDef,
    FuncType,

    /**
     * 函数形参 FuncFParam → BType Ident ['[' ']']
     *          1.普通变量 2.一维数组变量
     * 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
     *          1.花括号内重复0次 2.花括号内重复多次
     * 函数实参表达式 FuncRParams → Exp { ',' Exp }
     *          1.花括号内重复0次
     *          2.花括号内重复多次
     *          3.Exp需要覆盖数组传参和部分数组传参
     */
    FuncFParam,
    FuncFParams,
    FuncRParams,

    /**
     * 语句块 Block → '{' { BlockItem } '}'
     * 语句块项 BlockItem → Decl | Stmt // 覆盖两种语句块项
     */
    Block,
    BlockItem,

    Stmt,
    ForStmt,

    UnaryOp,
    IDENFR,

    Exp,
    LVal,
    PrimaryExp,
    UnaryExp,

    SwitchStmt,
    CaseStmt,
    DoWhileStmt,
    GotoStmt,
    LabelStmt,
    SWITCHTK,
    CASETK,
    DEFAULTTK,
    GOTOTK,
    CONSTTK,
    INTTK,
    BREAKTK,

    FORTK,
    STATICTK,MulExp,
    AddExp,
    RelExp,
    EqExp,
    CONTINUETK,
    IFTK,

    STRCON,


    Cond,
    MAINTK,
    GETINTTK,
    PRINTFTK,
    RETURNTK,
    PLUS,
    MINU,
    INC,
    DEC,
    VOIDTK,
    MULT,
    DIV,
    MOD,
    LSS,
    LEQ,
    GRE,
    GEQ,
    EQL,
    NEQ,
    ELSETK,
    NOT,
    AND,
    OR,
    LAndExp,
    LOrExp,
    ConstExp,

    Number,

    INTCON,
    ASSIGN,
    PLUSASSIGN,
    MINUASSIGN,
    MULTASSIGN,
    DIVASSIGN,
    MODASSIGN,
    QUESTION,
    COLON,
    DOTK,
    WHILETK,
    BITANDK,
    BITORK,
    BITXORK,
    SHLK,
    ASHRK,
    RPARENT,
    LBRACK,
    RBRACK,
    LBRACE,
    RBRACE,
    SEMICN,
    COMMA,
    LPARENT,
    EOF,
    ERROR
}
