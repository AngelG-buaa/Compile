package middle.optimize;

public class RegAllocaOptimizer extends Optimizer {
    @Override
    public void optimize() {
        RegAlloca.getInstance(irModule).alloca();
    }

    @Override
    public String OptimizerName() {
        return "RegAlloca";
    }
}
