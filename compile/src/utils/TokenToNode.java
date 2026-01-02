package utils;

import front.lexer.Token;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;

import java.util.ArrayList;
import java.util.List;

public class TokenToNode {

    /**
     * 将Token列表转换为TokenNode列表
     * @param tokens Token列表
     * @return TokenNode列表
     */
    public static ArrayList<TokenNode> convert(List<Token> tokens) {
        ArrayList<TokenNode> tokenNodes = new ArrayList<>();

        for (Token token : tokens) {
            SynType synType = mapTokenTypeToSynType(token.getTokenType());
            TokenNode tokenNode = new TokenNode(
                    synType,
                    token.getTokenContent(),
                    token.getLineNum()
            );
            tokenNodes.add(tokenNode);
        }

        return tokenNodes;
    }

    /**
     * 将单个Token转换为TokenNode
     * @param token Token对象
     * @return TokenNode对象
     */
    public static TokenNode convertSingle(Token token) {
        SynType synType = mapTokenTypeToSynType(token.getTokenType());
        return new TokenNode(
                synType,
                token.getTokenContent(),
                token.getLineNum()
        );
    }

    /**
     * 将TokenType映射到SynType
     * @param tokenType Token的类型
     * @return 对应的SynType
     */
    private static SynType mapTokenTypeToSynType(Token.TokenType tokenType) {
        switch (tokenType) {
            case HEXCON:
                return SynType.HEXCON;
            case UNTILK:
                return SynType.UNTILTK;
            case REPEATK:
                return SynType.REPEATTK;
            case IDENFR:
                return SynType.IDENFR;
            case INTCON:
                return SynType.INTCON;
            case STRCON:
                return SynType.STRCON;
            case CONSTTK:
                return SynType.CONSTTK;
            case INTTK:
                return SynType.INTTK;
            case STATICTK:
                return SynType.STATICTK;
            case BREAKTK:
                return SynType.BREAKTK;
            case CONTINUETK:
                return SynType.CONTINUETK;
            case IFTK:
                return SynType.IFTK;
            case MAINTK:
                return SynType.MAINTK;
            case ELSETK:
                return SynType.ELSETK;
            case NOT:
                return SynType.NOT;
            case AND:
                return SynType.AND;
            case OR:
                return SynType.OR;
            case FORTK:
                return SynType.FORTK;
            case RETURNTK:
                return SynType.RETURNTK;
            case VOIDTK:
                return SynType.VOIDTK;
            case PLUS:
                return SynType.PLUS;
            case MINU:
                return SynType.MINU;
            case PRINTFTK:
                return SynType.PRINTFTK;
            case MULT:
                return SynType.MULT;
            case DIV:
                return SynType.DIV;
            case MOD:
                return SynType.MOD;
            case LSS:
                return SynType.LSS;
            case LEQ:
                return SynType.LEQ;
            case GRE:
                return SynType.GRE;
            case GEQ:
                return SynType.GEQ;
            case EQL:
                return SynType.EQL;
            case NEQ:
                return SynType.NEQ;
            case SEMICN:
                return SynType.SEMICN;
            case COMMA:
                return SynType.COMMA;
            case LPARENT:
                return SynType.LPARENT;
            case RPARENT:
                return SynType.RPARENT;
            case LBRACK:
                return SynType.LBRACK;
            case RBRACK:
                return SynType.RBRACK;
            case LBRACE:
                return SynType.LBRACE;
            case RBRACE:
                return SynType.RBRACE;
            case ASSIGN:
                return SynType.ASSIGN;
            case EOF:
                return SynType.EOF;
            case ERR:
                return SynType.ERROR;
            default:
                // 如果遇到未知的TokenType，返回ERROR类型
                return SynType.ERROR;
        }
    }

    /**
     * 批量转换Token数组为TokenNode列表
     * @param tokens Token数组
     * @return TokenNode列表
     */
    public static List<TokenNode> convert(Token[] tokens) {
        List<TokenNode> tokenNodes = new ArrayList<>();
        for (Token token : tokens) {
            tokenNodes.add(convertSingle(token));
        }
        return tokenNodes;
    }

    /**
     * 打印TokenNode列表（用于调试）
     * @param tokenNodes TokenNode列表
     */
    public static void printTokenNodes(List<TokenNode> tokenNodes) {
        for (TokenNode node : tokenNodes) {
            System.out.println(node.toString());
        }
    }
}