package utils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EnvInitializer {
    // IO相关
    public static InputStream input;
    public static OutputStream error;
    public static OutputStream lexer;
    public static OutputStream parser;
    public static OutputStream symbol;
    public static OutputStream llvm_ir;
    public static OutputStream llvm_ir_init;
    public static OutputStream mips;

    /**
     * 初始化输入输出文件
     */
    public static void initializeIO() throws IOException {
        input = Files.newInputStream(Paths.get("testfile.txt"));
        error = new FileOutputStream("error.txt");
        lexer = new FileOutputStream("lexer.txt");
        parser = new FileOutputStream("parser.txt");
        symbol = new FileOutputStream("symbol.txt");
        llvm_ir = new FileOutputStream("llvm_ir.txt");
        llvm_ir_init = new FileOutputStream("llvm_ir_init.txt");
        mips = new FileOutputStream("mips.txt");
    }

    /**
     * 读文件
     */
    public static String readTestFile() throws IOException {
        StringBuilder sb = new StringBuilder();
        String string;

        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        while ((string = br.readLine()) != null) {
            sb.append(string);
            sb.append('\n'); // 添加换行符以保持原始文件格式
        }
        return sb.toString();
    }

    /**
     * 关闭所有流
     */
    public static void closeStreams() throws IOException {
        if (input != null) {
            input.close();
        }
        if (error != null) {
            error.close();
        }
        if (lexer != null) {
            lexer.close();
        }
        if (parser != null) {
            parser.close();
        }
        if (symbol != null) {
            symbol.close();
        }
        if (llvm_ir != null) {
            llvm_ir.close();
        }
        if (llvm_ir_init != null) {
            llvm_ir_init.close();
        }
        if (mips != null) {
            mips.close();
        }
    }
}
