package back;

import back.mips.MipsCodeGenerator;
import middle.llvm.IRModule;
import utils.EnvInitializer;

import java.io.IOException;
import java.io.OutputStreamWriter;

import static utils.EnvInitializer.mips;

public class BackManager {
    private IRModule module;

    public BackManager(IRModule module) {
        this.module = module;
    }

    public void runMipsGenerator(boolean outputToFile) throws IOException {
        MipsCodeGenerator mapper = MipsCodeGenerator.getInstance();
        String mipsCode = mapper.generateMipsCode(module);
        
        if (outputToFile) {
            mips.write(mipsCode.getBytes());
            mips.flush();
        } else {
            System.out.println(mipsCode);
        }
    }
}
