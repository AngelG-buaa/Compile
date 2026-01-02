import utils.EnvInitializer;
import front.frontManager;
import middle.middleManager;
import back.BackManager;

import java.io.IOException;

/**
 * clang -ccc-print-phases main.c               # 查看编译的过程
 * clang -E -Xclang -dump-tokens main.c         # 生成 tokens（词法分析）
 * clang -fsyntax-only -Xclang -ast-dump main.c # 生成抽象语法树
 * clang -S -emit-llvm main.c -o main.ll -O0    # 生成 LLVM ir (不开优化)
 * clang -S -emit-llvm main.m -o main.ll -Os    # 生成 LLVM ir (中端优化)
 * clang -S main.c -o main.s                    # 生成汇编
 * clang -S main.bc -o main.s -Os               # 生成汇编（后端优化）
 * clang -c main.c -o main.o                    # 生成目标文件
 * clang main.c -o main                         # 直接生成可执行文件
 * ========================================================================================
 * 用 lli main.ll 解释执行生成的 .ll 文件。如果一切正常，输入 echo $? 查看上一条指令的返回值。
 * 注意，$? 只保存返回值的第八位，即实际返回值与 0xff 按位与的结果。
 * ========================================================================================
 * 若用到库函数
 * # 分别导出 libsysy 和 main.c 对应的的 .ll 文件
 * clang -emit-llvm -S libsysy.c -o lib.ll
 * clang -emit-llvm -S main.c -o main.ll
 * # 使用 llvm-link 将两个文件链接，生成新的 IR 文件
 * llvm-link main.ll lib.ll -S -o out.ll
 * # 用 lli 解释运行
 * lli out.ll
 */

public class Compiler {
    // 注意，应该是false false ... true的形式
    public static void main(String[] args) throws IOException {
        /**
         * 用EnvInitializer初始化输入输出文件，进行文件读入
         */
        EnvInitializer.initializeIO();
        String testCode = EnvInitializer.readTestFile();

        /**
         * 将读入的文件的内容输入到Lexer进行词法分析
         */
        frontManager.runLexer(testCode,true);

        /**
         * 将词法分析得到的token串输入到Parser进行语法分析
         */
        frontManager.runParser(true);

        /**
         * 进行语义分析
         */
        boolean hasErr = middleManager.runChecker(true);
        if (hasErr) {
            EnvInitializer.closeStreams();
            return;
        }

        /**
         * llvm_ir中间代码生成
         */
        middleManager.runVisitor(true);

        /**
         * llvm_ir中间代码优化
         */
        middleManager.runOptimizer(true,false);

        /**
         * mips代码生成
         */
        BackManager backManager = new BackManager(middleManager.getIrModule());
        backManager.runMipsGenerator(true);

        /**
         * 关闭输入输出流
         */
        EnvInitializer.closeStreams();
    }
}
