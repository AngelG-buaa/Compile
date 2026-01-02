package middle.optimize;

import middle.llvm.IRModule;

/**
 * 优化器基类
 *
 * 用途：为所有优化 Pass 提供统一的上下文（`IRModule`）与统一入口（`optimize()`）。
 *
 * 使用方式：
 * - 在优化流程开始时，通过 `Optimizer.init(irModule)` 设置当前待优化的模块；
 * - 每个具体优化器继承本类并实现 `optimize()`，无需重复传递模块；
 * - `OptimizerName()` 可用于日志或调试输出标识该优化器。
 *
 * 典型生命周期（与 OptimizeManager 配合）：
 * - 初始化模块 → 构建/修复 CFG → 执行若干优化（Mem2Reg / URC / DCE / LVN 等）→ 根据需要再次重建 CFG → 迭代直到收敛。
 *
 * 扩展指南：
 * - 新增优化器时，只需继承 `Optimizer` 并实现 `optimize()`；
 * - 若优化会改变控制流结构，建议在 OptimizeManager 中于前后插入 `BuildCFG`；
 * - 遵循“尽量局部、幂等、可迭代”的原则，避免一次改动过多导致不易收敛。
 */
public abstract class Optimizer {
    static IRModule irModule;

    public static void init(IRModule irModule) {
        Optimizer.irModule = irModule;
    }

    public abstract void optimize();

    public String OptimizerName() {
        return "Optimizer";
    }
}
