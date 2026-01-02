package middle;

import error.ErrorManager;
import front.frontManager;
import front.parser.syntax.BranchNode;
import front.parser.syntax.CompUnit;
import middle.checker.symbol.SymbolManager;
import middle.checker.symbol.SymbolTable;
import middle.checker.Checker;
import middle.llvm.IRModule;
import middle.llvm.Visitor;
import middle.optimize.OptimizeManager;
import utils.EnvInitializer;

import java.io.IOException;

public class middleManager {
    private static IRModule module;
    private static Visitor visitor;

    // ---------------------------------语义分析----------------------------------------------------------
    /**
     * 运行语义分析
     */
    public static boolean runChecker(boolean putOut) throws IOException {
        // 获取AST根节点
        BranchNode astRoot = frontManager.getASTRoot();
        
        if (astRoot == null) {
            System.err.println("AST root is null, cannot visit");
            return true;
        }
        
        // 执行语义分析
        Checker.analyze(astRoot);

        boolean hasErr = outputErrors();

        if (putOut) {
            // 输出符号表（如果需要）
            outputSymbolTable();
        }

        return hasErr;
    }
    
    /**
     * 输出符号表到文件
     */
    private static void outputSymbolTable() throws IOException {
        SymbolTable rootTable = SymbolManager.getSymbolTable();
        if (rootTable != null) {
            String symbolOutput = rootTable.toString();
            if (!symbolOutput.isEmpty()) {
                EnvInitializer.symbol.write(symbolOutput.getBytes());
                EnvInitializer.symbol.flush();
            }
        }
    }
    
    /**
     * 输出错误信息到文件
     * 返回true表示有错误
     */
    private static boolean outputErrors() throws IOException {
        if (!ErrorManager.HaveNoError()) {
            var errors = ErrorManager.GetErrorList();
            for (var error : errors) {
                String errorOutput = error.toString() + "\n";
                EnvInitializer.error.write(errorOutput.getBytes());
            }
            EnvInitializer.error.flush();
            return true;
        }
        return false;
    }
    
    /**
     * 获取符号表
     */
    public static SymbolTable getSymbolTable() {
        return SymbolManager.getSymbolTable();
    }

    // ---------------------------------中间代码生成--------------------------------------------------------
    public static void runVisitor(boolean putOut) throws IOException {
        // 获取AST根节点
        BranchNode astRoot = frontManager.getASTRoot();

        visitor = Visitor.getVisitor();
        module = visitor.visit(astRoot);

        if (putOut) {
            String llvm_IR_init = module.toString();
            EnvInitializer.llvm_ir_init.write(llvm_IR_init.getBytes());
            EnvInitializer.llvm_ir_init.flush();
        }
    }

    // ---------------------------------中间代码优化--------------------------------------------------------
    public static void runOptimizer(boolean putOut, boolean debug) throws IOException {
        OptimizeManager.init(module,visitor);
        OptimizeManager.optimize(debug);

        if (putOut) {
            String llvm_IR_optimize = module.toString();
            EnvInitializer.llvm_ir.write(llvm_IR_optimize.getBytes());
            EnvInitializer.llvm_ir.flush();
        }
    }

    public static IRModule getIrModule() {
        return module;
    }

}