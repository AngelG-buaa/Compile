package middle.optimize;

import middle.llvm.IRModule;
import middle.llvm.Visitor;

import java.util.ArrayList;

/**
 * 优化管理器
 *
 * 负责串联并执行所有的中端优化 Pass。核心理念：
 * - 先构建 CFG 与支配关系，保证分析基础准确；
 * - 再进行语义保持的消除与简化（不可达块、死代码、Phi 简化、局部值编号等）；
 * - 对需要 SSA 化的变量使用 MemToReg（InsertPhi + 重命名）转换为寄存器值；
 * - 每次改变控制流或指令集后，必须重新构建 CFG；
 *
 * 典型优化序列（示例）：
 *  1) BuildCFG：识别基本块与边，计算支配与支配边界
 *  2) MemToReg：将非数组的 alloca 转换为 SSA：插入 Phi，移除 load/store
 *  3) RemoveUnReachCode：根据 CFG 清理不可达块与边
 *  4) RemoveDeadCode：删除非关键且无用户的指令；将只有一个来边值的 Phi 退化
 *  5) LocalValueNumbering：同等表达式消除，常量折叠
 *  6) BuildCFG：重建 CFG 以反映最新结构
 *
 * 优化前后 IR 示例（片段）：
 *  输入（未优化，含内存操作）：
 *    %var = alloca i32
 *    store i32 0, i32* %var
 *    br i1 %cond, label %b1, label %b2
 *  b1:
 *    store i32 1, i32* %var
 *    br label %b3
 *  b2:
 *    store i32 2, i32* %var
 *    br label %b3
 *  b3:
 *    %x = load i32, i32* %var
 *    ret i32 %x
 *
 *  经 MemToReg + BuildCFG → Phi 插入与重命名：
 *  entry:
 *    br i1 %cond, label %b1, label %b2
 *  b1:
 *    ; 当前值=1
 *    br label %b3
 *  b2:
 *    ; 当前值=2
 *    br label %b3
 *  b3:
 *    %x = phi i32 [ 1, %b1 ], [ 2, %b2 ]
 *    ret i32 %x
 *
 * 注意：顺序与重建频率直接影响正确性与优化效果。
 */
public class OptimizeManager {
    private static ArrayList<Optimizer> optimizers;
    private static IRModule irModule;

    /**
     * 初始化优化管理器
     * 
     * @param module IR模块
     */
    public static void init(IRModule module, Visitor visitor) {
        irModule = module;
        Optimizer.init(module);
        
        optimizers = new ArrayList<>();

        // 1. 首先删除不可达代码
        optimizers.add(new RemoveUnReachCode());
        optimizers.add(new BuildCFG());
        
        // 1.5 清理死块与合并基本块 (Jump Threading / Block Merging)
        optimizers.add(new RemoveDeadBlock());
        optimizers.add(new BuildCFG());

        // 2. 死代码消除
        optimizers.add(new RemoveDeadCode());
        optimizers.add(new BuildCFG());

        // 3. 内存到寄存器优化（插入 Phi，重命名，移除 load/store）
        optimizers.add(new MemToReg());
        optimizers.add(new BuildCFG());
//
        // 4. 再次删除不可达代码和死代码
        optimizers.add(new RemoveUnReachCode());
        optimizers.add(new BuildCFG());
        optimizers.add(new RemoveDeadCode());
        optimizers.add(new BuildCFG());

        // 4.5 循环不变式外提 (LICM)
        optimizers.add(new LoopInvariantCodeMotion());
        optimizers.add(new BuildCFG());
        // 再次清理可能产生的死代码
        optimizers.add(new RemoveDeadCode());
        optimizers.add(new BuildCFG());

        // 5. 多轮局部值编号和死代码消除优化
        for (int i = 0; i < 5; i++) {
            optimizers.add(new LocalValueNumbering());
            optimizers.add(new RemoveUnReachCode());
            optimizers.add(new BuildCFG());
            optimizers.add(new RemoveDeadCode());
            optimizers.add(new BuildCFG());
        }
        optimizers.add(new RegAllocaOptimizer());
        optimizers.add(new BuildCFG());
//
//        // -------------到这里是正确的----------------
//
        optimizers.add(new RemovePhi(visitor));
    }

    /**
     * 执行所有优化过程
     */
    public static void optimize(boolean debug) {
        if (optimizers == null) {
            throw new IllegalStateException("OptimizeManager not initialized. Call init() first.");
        }
        
        for (Optimizer optimizer : optimizers) {
            optimizer.optimize();

            if (debug) {
                System.out.println(optimizer.OptimizerName());
            }
        }

    }

    /**
     * 获取当前的IR模块
     * 
     * @return IR模块
     */
    public static IRModule getIRModule() {
        return irModule;
    }
}
