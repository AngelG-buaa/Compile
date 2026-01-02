package front;

import error.Error;
import error.ErrorManager;
import front.lexer.Lexer;
import front.lexer.Token;
import front.parser.Parser;
import front.parser.syntax.BranchNode;
import utils.EnvInitializer;

import java.io.IOException;
import java.util.ArrayList;

public class frontManager {
    // ---------------------------------Lexer---------------------------------

    public static Lexer lexer = new Lexer(); // 实例化lexer

    public static void runLexer(String testCode, boolean putOut) throws IOException {
        // 词法分析器运行
        lexer.init(testCode);
        lexer.run();

        // 词法分析结果打印输出
        if (putOut) {
            ArrayList<Token> lexerOutput = lexer.getTokens();
            ArrayList<String> lexerErrors = lexer.getErrors();

            if (!lexerErrors.isEmpty()) {
                writeErrorsToStream(lexerErrors);
            } else {
                writeTokensToStream(lexerOutput);
            }

        }
    }

    private static void writeTokensToStream(ArrayList<Token> tokens) throws IOException {
        for (Token token : tokens) {
            // 输出所有token，包括ERR token，但跳过EOF
            if (token.getTokenType() != Token.TokenType.EOF) {
                String output = token.getTokenType() + " " + token.getTokenContent() + "\n";
                EnvInitializer.lexer.write(output.getBytes());
            }
        }
        // 刷新输出流
        EnvInitializer.lexer.flush();
    }

    private static void writeErrorsToStream(ArrayList<String> errors) throws IOException {
        for (String error : errors) {
            String output = error + "\n";
            EnvInitializer.error.write(output.getBytes());
        }
        // 刷新输出流
        EnvInitializer.error.flush();
    }

    // ---------------------------------Parser---------------------------------

    public static Parser parser = new Parser();

    public static void runParser(boolean putOut) throws IOException {
        // 词法分析器运行
        parser.init(lexer.getTokens());
        parser.run();

        // 语法分析结果打印输出
        if (putOut) {
            // DEBUG
            // parser.getRoot().prettyPrint("|");

            // parser.txt
            String parserOutput = parser.getRoot().toString();

            // error.txt
            ArrayList<Error> parserErrors = ErrorManager.GetErrorList();

            // 输出至文件
            if (!parserErrors.isEmpty()) {
                for (Error error : parserErrors) {
                    String errorOutput = error.toString() + "\n";
                    EnvInitializer.error.write(errorOutput.getBytes());
                }
                EnvInitializer.error.flush();
            } else {
                EnvInitializer.parser.write(parserOutput.getBytes());
                EnvInitializer.parser.flush();
            }

        }
    }

    /**
     * 获取AST根节点
     */
    public static BranchNode getASTRoot() {
        return parser.getRoot();
    }

}