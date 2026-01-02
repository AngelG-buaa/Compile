package front.lexer;

public class Token {
    private final String tokenContent;
    private final TokenType tokenType;
    private final int lineNum;

    public enum TokenType {
        HEXCON,
        REPEATK,
        UNTILK,
        /**
         * 自己加的，附到testCode串的最后
         * 用 ? (\uD83D)表示
         */
        EOF,

        /**
         * 自己加的，表示出错的token
         */
        ERR,

        IDENFR,
        INTCON,
        STRCON,
        CONSTTK,
        INTTK,
        STATICTK,
        BREAKTK,
        CONTINUETK,
        IFTK,
        MAINTK,
        ELSETK,
        NOT,
        AND,
        OR,
        FORTK,
        RETURNTK,
        VOIDTK,
        PLUS,
        MINU,
        PRINTFTK,
        MULT,
        DIV,
        MOD,
        LSS,
        LEQ,
        GRE,
        GEQ,
        EQL,
        NEQ,
        SEMICN,
        COMMA,
        LPARENT,
        RPARENT,
        LBRACK,
        RBRACK,
        LBRACE,
        RBRACE,
        ASSIGN
    }

    public Token(String tokenContent, TokenType tokenType, int lineNum) {
        this.tokenContent = tokenContent;
        this.tokenType = tokenType;
        this.lineNum = lineNum;
    }

    // Getter 方法
    public String getTokenContent() {
        return tokenContent;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public int getLineNum() {
        return lineNum;
    }

    @Override
    public String toString() {
        return tokenType + " " + tokenContent;
    }
}