package front.lexer;

import error.ErrorManager;
import error.ErrorType;
import error.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Lexer {
    // 源代码相关
    private String sourceCode;
    private int currentIndex;
    private int currentLine;
    private int currentColumn;

    // Token存储
    private ArrayList<Token> tokens;
    private ArrayList<String> errorMessages;

    // 关键字映射表
    private static final Map<String, Token.TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("const", Token.TokenType.CONSTTK);
        KEYWORDS.put("int", Token.TokenType.INTTK);
        KEYWORDS.put("static", Token.TokenType.STATICTK);
        KEYWORDS.put("break", Token.TokenType.BREAKTK);
        KEYWORDS.put("continue", Token.TokenType.CONTINUETK);
        KEYWORDS.put("if", Token.TokenType.IFTK);
        KEYWORDS.put("main", Token.TokenType.MAINTK);
        KEYWORDS.put("else", Token.TokenType.ELSETK);
        KEYWORDS.put("for", Token.TokenType.FORTK);
        KEYWORDS.put("return", Token.TokenType.RETURNTK);
        KEYWORDS.put("void", Token.TokenType.VOIDTK);
        KEYWORDS.put("printf", Token.TokenType.PRINTFTK);
        KEYWORDS.put("repeat", Token.TokenType.REPEATK);
        KEYWORDS.put("until", Token.TokenType.UNTILK);
        KEYWORDS.put("do", Token.TokenType.DOK);
        KEYWORDS.put("while", Token.TokenType.WHILEK);
        KEYWORDS.put("bitand", Token.TokenType.BITANDK);
        KEYWORDS.put("switch", Token.TokenType.SWITCHTK);
        KEYWORDS.put("case", Token.TokenType.CASETK);
        KEYWORDS.put("default", Token.TokenType.DEFAULTTK);
        KEYWORDS.put("goto", Token.TokenType.GOTOTK);
    }

    public void init(String sourceCode) {
        this.sourceCode = sourceCode + "\uD83D"; // 添加EOF标记
        this.currentIndex = 0;
        this.currentLine = 1;
        this.currentColumn = 1;
        this.tokens = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
    }

    public void run() {
        while (currentIndex < sourceCode.length() && getCurrentChar() != '\uD83D') {
            try {
                // 跳过空格
                skipWhitespaceAndComments();

                // 结束
                if (currentIndex >= sourceCode.length() || getCurrentChar() == '\uD83D') {
                    break;
                }

                // 处理各种token类型
                char currentChar = getCurrentChar();

                if (Character.isLetter(currentChar) || currentChar == '_') {
                    parseIdentifierOrKeyword();
                } else if (Character.isDigit(currentChar)) {
                    parseNumber();
                } else if (currentChar == '"') {
                    parseString();
                } else if (isOperatorStart(currentChar)) {
                    parseOperator();
                } else if (isPunctuation(currentChar)) {
                    parsePunctuation();
                } else {
                    // 遇到非法字符，创建ERR token并继续
                    handleIllegalCharacter();
                }
            } catch (Exception e) {
                // 异常处理：创建ERR token并尝试恢复
                handleLexicalError("Unexpected lexical error: " + e.getMessage());
                skipToNextValidPosition();
            }
        }

        // 添加EOF token
        addToken("", Token.TokenType.EOF);
    }

    private void skipWhitespaceAndComments() {
        while (currentIndex < sourceCode.length()) {
            char ch = getCurrentChar();

            if (Character.isWhitespace(ch)) {
                if (ch == '\n') {
                    currentLine++;
                    currentColumn = 1;
                } else {
                    currentColumn++;
                }
                currentIndex++;
            } else if (ch == '/' && peekNextChar() == '/') {
                // 单行注释
                skipSingleLineComment();
            } else if (ch == '/' && peekNextChar() == '*') {
                // 多行注释
                skipMultiLineComment();
            } else {
                break;
            }
        }
    }

    private void skipSingleLineComment() {
        currentIndex += 2; // 跳过 //
        currentColumn += 2;

        while (currentIndex < sourceCode.length() && getCurrentChar() != '\n') {
            currentIndex++;
            currentColumn++;
        }

        if (currentIndex < sourceCode.length() && getCurrentChar() == '\n') {
            currentIndex++;
            currentLine++;
            currentColumn = 1;
        }
    }

    private void skipMultiLineComment() {
        currentIndex += 2; // 跳过 /*
        currentColumn += 2;

        while (currentIndex < sourceCode.length() - 1) {
            if (getCurrentChar() == '*' && peekNextChar() == '/') {
                currentIndex += 2; // 跳过 */
                currentColumn += 2;
                return;
            }

            if (getCurrentChar() == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
            currentIndex++;
        }

        // 如果到达文件末尾仍未找到注释结束，记录错误但继续
        handleLexicalError("Unterminated multi-line comment");
    }

    private void parseIdentifierOrKeyword() {
        int startIndex = currentIndex;
        int startColumn = currentColumn;
        StringBuilder identifier = new StringBuilder();

        while (currentIndex < sourceCode.length() &&
                (Character.isLetterOrDigit(getCurrentChar()) || getCurrentChar() == '_')) {
            identifier.append(getCurrentChar());
            currentIndex++;
            currentColumn++;
        }

        String identifierStr = identifier.toString();
        Token.TokenType tokenType = KEYWORDS.getOrDefault(identifierStr, Token.TokenType.IDENFR);

        addToken(identifierStr, tokenType);
    }

    private void parseNumber() {
        StringBuilder number = new StringBuilder();
        boolean hasError = false;

        if (getCurrentChar() == '0' && (peekNextChar() == 'X' || peekNextChar() == 'x')) {
            while (currentIndex < sourceCode.length() && Character.isLetterOrDigit(getCurrentChar())) {
                number.append(getCurrentChar());
                currentIndex++;
                currentColumn++;
            }
            addToken(number.toString(), Token.TokenType.HEXCON);
        }
        else {
            while (currentIndex < sourceCode.length() && Character.isDigit(getCurrentChar())) {
                number.append(getCurrentChar());
                currentIndex++;
                currentColumn++;
            }

            // 检查数字后是否跟着字母（非法情况）
            if (currentIndex < sourceCode.length() && Character.isLetter(getCurrentChar())) {
                // 继续读取直到非字母数字字符
                while (currentIndex < sourceCode.length() &&
                        (Character.isLetterOrDigit(getCurrentChar()) || getCurrentChar() == '_')) {
                    number.append(getCurrentChar());
                    currentIndex++;
                    currentColumn++;
                }
                hasError = true;
            }

            if (hasError) {
                addErrorToken(number.toString(), "Invalid number format");
            } else {
                addToken(number.toString(), Token.TokenType.INTCON);
            }
        }
    }

    private void parseString() {
        StringBuilder string = new StringBuilder();
        string.append(getCurrentChar()); // 添加开始的双引号
        currentIndex++;
        currentColumn++;

        boolean terminated = false;

        while (currentIndex < sourceCode.length()) {
            char ch = getCurrentChar();

            if (ch == '"') {
                string.append(ch);
                currentIndex++;
                currentColumn++;
                terminated = true;
                break;
            } else if (ch == '\n') {
                // 字符串不能跨行
                handleLexicalError("Unterminated string literal");
                addErrorToken(string.toString(), "Unterminated string");
                return;
            } else if (ch == '\\') {
                // 处理转义字符
                string.append(ch);
                currentIndex++;
                currentColumn++;

                if (currentIndex < sourceCode.length()) {
                    string.append(getCurrentChar());
                    currentIndex++;
                    currentColumn++;
                }
            } else {
                string.append(ch);
                currentIndex++;
                currentColumn++;
            }
        }

        if (!terminated) {
            addErrorToken(string.toString(), "Unterminated string");
        } else {
            addToken(string.toString(), Token.TokenType.STRCON);
        }
    }

    private void parseOperator() {
        char ch = getCurrentChar();
        char nextChar = peekNextChar();

        switch (ch) {
            case '+':
                if (nextChar == '+') {
                    addToken("++", Token.TokenType.INC);
                    advance(2);
                } else if (nextChar == '=') {
                    addToken("+=", Token.TokenType.PLUSASSIGN);
                    advance(2);
                } else {
                    addToken("+", Token.TokenType.PLUS);
                    advance();
                }
                break;
            case '-':
                if (nextChar == '-') {
                    addToken("--", Token.TokenType.DEC);
                    advance(2);
                } else if (nextChar == '=') {
                    addToken("-=", Token.TokenType.MINUASSIGN);
                    advance(2);
                } else {
                    addToken("-", Token.TokenType.MINU);
                    advance();
                }
                break;
            case '*':
                if (nextChar == '=') {
                    addToken("*=", Token.TokenType.MULTASSIGN);
                    advance(2);
                } else {
                    addToken("*", Token.TokenType.MULT);
                    advance();
                }
                break;
            case '/':
                if (nextChar == '=') {
                    addToken("/=", Token.TokenType.DIVASSIGN);
                    advance(2);
                } else {
                    addToken("/", Token.TokenType.DIV);
                    advance();
                }
                break;
            case '%':
                if (nextChar == '=') {
                    addToken("%=", Token.TokenType.MODASSIGN);
                    advance(2);
                } else {
                    addToken("%", Token.TokenType.MOD);
                    advance();
                }
                break;
            case '<':
                if (nextChar == '=') {
                    addToken("<=", Token.TokenType.LEQ);
                    advance(2);
                } else if (nextChar == '<') {
                    addToken("<<", Token.TokenType.SHLK);
                    advance(2);
                } else {
                    addToken("<", Token.TokenType.LSS);
                    advance();
                }
                break;
            case '>':
                if (nextChar == '=') {
                    addToken(">=", Token.TokenType.GEQ);
                    advance(2);
                } else if (nextChar == '>') {
                    addToken(">>", Token.TokenType.ASHRK);
                    advance(2);
                } else {
                    addToken(">", Token.TokenType.GRE);
                    advance();
                }
                break;
            case '=':
                if (nextChar == '=') {
                    addToken("==", Token.TokenType.EQL);
                    advance(2);
                } else {
                    addToken("=", Token.TokenType.ASSIGN);
                    advance();
                }
                break;
            case '!':
                if (nextChar == '=') {
                    addToken("!=", Token.TokenType.NEQ);
                    advance(2);
                } else {
                    addToken("!", Token.TokenType.NOT);
                    advance();
                }
                break;
            case '&':
                if (nextChar == '&') {
                    addToken("&&", Token.TokenType.AND);
                    advance(2);
                } else {
                    addToken("&", Token.TokenType.BITANDK);
                    advance();
                }
                break;
            case '|':
                if (nextChar == '|') {
                    addToken("||", Token.TokenType.OR);
                    advance(2);
                } else {
                    addToken("|", Token.TokenType.BITORK);
                    advance();
                }
                break;
            case '^':
                addToken("^", Token.TokenType.BITXORK);
                advance();
                break;
            default:
                handleIllegalCharacter();
                break;
        }
    }

    private void parsePunctuation() {
        char ch = getCurrentChar();

        switch (ch) {
            case ';':
                addToken(";", Token.TokenType.SEMICN);
                break;
            case ',':
                addToken(",", Token.TokenType.COMMA);
                break;
            case '(':
                addToken("(", Token.TokenType.LPARENT);
                break;
            case ')':
                addToken(")", Token.TokenType.RPARENT);
                break;
            case '[':
                addToken("[", Token.TokenType.LBRACK);
                break;
            case ']':
                addToken("]", Token.TokenType.RBRACK);
                break;
            case '{':
                addToken("{", Token.TokenType.LBRACE);
                break;
            case '}':
                addToken("}", Token.TokenType.RBRACE);
                break;
            case '?':
                addToken("?", Token.TokenType.QUESTION);
                break;
            case ':':
                addToken(":", Token.TokenType.COLON);
                break;
            default:
                handleIllegalCharacter();
                return;
        }
        advance();
    }

    private boolean isOperatorStart(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '%' ||
                ch == '<' || ch == '>' || ch == '=' || ch == '!' || ch == '&' || ch == '|' || ch == '^';
    }

    private boolean isPunctuation(char ch) {
        return ch == ';' || ch == ',' || ch == '(' || ch == ')' ||
                ch == '[' || ch == ']' || ch == '{' || ch == '}' ||
                ch == '?' || ch == ':';
    }

    private void handleIllegalCharacter() {
        char illegalChar = getCurrentChar();
        String charStr = String.valueOf(illegalChar);

        // 记录错误到错误处理系统
        ErrorManager.AddError(Error.createError(ErrorType.ILLEGAL_SYMBOL, currentLine));

        // 创建ERR token
        addErrorToken(charStr, "Illegal character: '" + illegalChar + "'");

        // 继续处理下一个字符
        advance();
    }

    private void handleLexicalError(String errorMessage) {
        ErrorManager.AddError(Error.createError(ErrorType.ILLEGAL_SYMBOL, currentLine));
        errorMessages.add(currentLine + " a"); // 格式化错误信息
    }

    private void skipToNextValidPosition() {
        // 跳过当前字符，尝试从下一个字符恢复
        if (currentIndex < sourceCode.length()) {
            advance();
        }
    }

    private void addToken(String content, Token.TokenType type) {
        tokens.add(new Token(content, type, currentLine));
    }

    private void addErrorToken(String content, String errorMessage) {
        if (content.equals("&")) {
            tokens.add(new Token(content, Token.TokenType.AND, currentLine));
        } else if (content.equals("|")) {
            tokens.add(new Token(content, Token.TokenType.OR, currentLine));
        } else {
            tokens.add(new Token(content, Token.TokenType.ERR, currentLine));
        }

        errorMessages.add(currentLine + " a"); // 格式: 行号 + 错误类型编码
    }

    private char getCurrentChar() {
        if (currentIndex >= sourceCode.length()) {
            return '\uD83D'; // EOF
        }
        return sourceCode.charAt(currentIndex);
    }

    private char peekNextChar() {
        if (currentIndex + 1 >= sourceCode.length()) {
            return '\uD83D'; // EOF
        }
        return sourceCode.charAt(currentIndex + 1);
    }

    private void advance() {
        advance(1);
    }

    private void advance(int count) {
        for (int i = 0; i < count && currentIndex < sourceCode.length(); i++) {
            if (getCurrentChar() == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
            currentIndex++;
        }
    }

    // Getter方法
    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public ArrayList<String> getErrors() {
        return errorMessages;
    }

    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }
}
