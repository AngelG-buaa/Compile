package middle.llvm;

import middle.llvm.value.IRBasicBlock;
import java.util.HashMap;
import java.util.Map;

public class SwitchStructure {
    private IRBasicBlock exitBlock;
    private IRBasicBlock defaultBlock;
    private Map<Integer, IRBasicBlock> cases;
    private IRBasicBlock headerBlock; // The block containing the switch/branch instructions

    public SwitchStructure(IRBasicBlock headerBlock, IRBasicBlock exitBlock) {
        this.headerBlock = headerBlock;
        this.exitBlock = exitBlock;
        this.cases = new HashMap<>();
        this.defaultBlock = null; // defaults to exitBlock if not set
    }

    public IRBasicBlock getExitBlock() { return exitBlock; }
    public IRBasicBlock getHeaderBlock() { return headerBlock; }
    
    public void addCase(int val, IRBasicBlock block) {
        cases.put(val, block);
    }
    
    public void setDefaultBlock(IRBasicBlock block) {
        this.defaultBlock = block;
    }
    
    public IRBasicBlock getDefaultBlock() {
        return defaultBlock != null ? defaultBlock : exitBlock;
    }
    
    public Map<Integer, IRBasicBlock> getCases() {
        return cases;
    }
}
