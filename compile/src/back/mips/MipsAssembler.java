package back.mips;

import back.mips.data.*;
import back.mips.instruction.*;
import back.mips.instruction.assign.*;
import back.mips.instruction.branch.*;
import back.mips.instruction.compare.*;
import back.mips.instruction.compute.*;
import back.mips.instruction.hilo.*;
import back.mips.instruction.jump.*;
import back.mips.instruction.memory.*;
import back.mips.register.Reg;
import middle.llvm.value.IRFunction;
import middle.llvm.value.IRValue;
import middle.llvm.value.instruction.BinaryOperationInstruction;
import middle.llvm.value.instruction.CompareInstruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * MIPS 汇编器基类
 * 提供底层指令生成、数据段管理和上下文状态维护
 */
public class MipsAssembler {
    // ==================== 数据段 (Data Segment) ====================
    protected final List<WordData> words = new ArrayList<>();
    protected final List<ByteData> bytes = new ArrayList<>();
    protected final List<SpaceData> spaces = new ArrayList<>();
    protected final List<AsciiData> ascii = new ArrayList<>();

    // ==================== 代码段 (Text Segment) ====================
    protected final List<InstrM> instructions = new ArrayList<>();

    // ==================== 上下文状态 (Context State) ====================
    protected int currentOffset = 0;
    protected HashMap<IRValue, Integer> valOffsets = new HashMap<>();
    protected HashMap<IRValue, Reg> valRegs = new HashMap<>();
    protected IRFunction currentFunction;

    public MipsAssembler() {}

    // ==================== 数据生成方法 ====================

    protected void makeAsciiData(String name, String content) {
        ascii.add(new AsciiData(name, content));
    }

    protected void makeSpaceData(String name, int size) {
        spaces.add(new SpaceData(name, size));
    }

    protected void makeWordData(String name, ArrayList<Integer> values) {
        words.add(new WordData(name, values));
    }

    protected void makeByteData(String name, ArrayList<Integer> values) {
        bytes.add(new ByteData(name, values));
    }

    // ==================== 函数上下文管理 ====================

    protected void beginFunction(IRFunction func) {
        currentFunction = func;
        currentOffset = 0;
        valOffsets = new HashMap<>();
        // 寄存器分配映射通常由上层传入，但这里做防御性清空
        if (func.getValue2reg() != null) {
            valRegs = func.getValue2reg();
        } else {
            valRegs = new HashMap<>();
        }
        instructions.add(new Label(func.getName().substring(1)));
    }

    protected void makeLabel(String name) {
        instructions.add(new Label(name));
    }

    // ==================== 栈帧管理 (核心对齐逻辑) ====================

    /**
     * 在栈上分配空间
     * @param size 分配大小
     * @param align 对齐字节数 (1 或 4)
     * @return 分配后的栈偏移
     */
    public int subAlign(int size, int align) {
        currentOffset -= size;
        if (align != 1) {
            // 只有非1字节对齐时，才进行4字节向下取整
            currentOffset = largestMultipleOfFour(currentOffset);
        }
        return currentOffset;
    }

    private static int largestMultipleOfFour(int num) {
        // 使用位运算高效对齐 (向下取整到4的倍数)
        return num & -4;
    }

    public void putOffset(IRValue val, int off) {
        valOffsets.put(val, off);
    }

    public void putReg(IRValue val, Reg r) {
        valRegs.put(val, r);
    }

    public Integer findOffset(IRValue val) {
        return valOffsets.get(val);
    }

    public Reg findReg(IRValue val) {
        return valRegs.get(val);
    }

    // ==================== 指令生成方法 ====================

    // --- 计算 ---
    protected void makeCompute(BinaryOperationInstruction instr, Reg dest, Reg left, Reg right) {
        makeCompute(instr.getOperator(), dest, left, right);
    }

    protected void makeCompute(BinaryOperationInstruction.BinaryOperator op, Reg dest, Reg left, Reg right) {
        switch (op) {
            case ADD: instructions.add(new Addu(dest, left, right)); break;
            case SUB: instructions.add(new Subu(dest, left, right)); break;
            case MUL:
                instructions.add(new Mult(left, right));
                instructions.add(new Mflo(dest));
                break;
            case SDIV:
                instructions.add(new Div(left, right));
                instructions.add(new Mflo(dest));
                break;
            case SREM:
                instructions.add(new Div(left, right));
                instructions.add(new Mfhi(dest));
                break;
            default: break;
        }
    }

    // --- 移位 ---
    protected Sll makeSll(Reg dest, Reg src, int shift) {
        Sll instr = new Sll(dest, src, shift);
        instructions.add(instr);
        return instr;
    }

    // --- 赋值 ---
    protected Li makeLi(Reg dest, int imm) {
        Li instr = new Li(dest, imm);
        instructions.add(instr);
        return instr;
    }

    protected Move makeMove(Reg to, Reg from) {
        Move instr = new Move(to, from);
        instructions.add(instr);
        return instr;
    }

    protected La makeLa(Reg dest, String label) {
        La instr = new La(dest, label);
        instructions.add(instr);
        return instr;
    }

    // --- 内存访问 (自动选择 Byte/Word) ---
    protected MemoryM makeStore(int align, Reg src, int offset, Reg base) {
        MemoryM instr;
        if (align == 1) {
            instr = new Sb(src, offset, base);
        } else {
            instr = new Sw(src, offset, base);
        }
        instructions.add(instr);
        return instr;
    }

    protected MemoryM makeLoad(int align, Reg dest, int offset, Reg base) {
        MemoryM instr;
        if (align == 1) {
            instr = new Lb(dest, offset, base);
        } else {
            instr = new Lw(dest, offset, base);
        }
        instructions.add(instr);
        return instr;
    }

    // --- 跳转与分支 ---
    protected Beq makeBeq(Reg left, Reg right, String target) {
        Beq instr = new Beq(left, right, target);
        instructions.add(instr);
        return instr;
    }

    protected J makeJ(String target) {
        J instr = new J(target);
        instructions.add(instr);
        return instr;
    }

    protected Jal makeJal(String target) {
        Jal instr = new Jal(target);
        instructions.add(instr);
        return instr;
    }

    protected Jr makeJr(Reg target) {
        Jr instr = new Jr(target);
        instructions.add(instr);
        return instr;
    }

    // --- 系统调用 ---
    protected void makeSyscall() {
        instructions.add(new SyscallM());
    }

    // --- 比较 ---
    protected void makeCompare(CompareInstruction.CompareCondition cond, Reg dest, Reg left, Reg right) {
        switch (cond) {
            case EQ: instructions.add(new Seq(dest, left, right)); break;
            case NE: instructions.add(new Sne(dest, left, right)); break;
            case SLT: instructions.add(new Slt(dest, left, right)); break;
            case SLE: instructions.add(new Sle(dest, left, right)); break;
            case SGT: instructions.add(new Sgt(dest, left, right)); break;
            case SGE: instructions.add(new Sge(dest, left, right)); break;
            default: break;
        }
    }

    // ==================== 输出生成 (优化布局) ====================
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(".data\n");
        // 1. Words
        for (WordData d : words) sb.append(d).append("\n");
        if (!words.isEmpty()) sb.append("\n");

        // 2. Aligned Spaces (4字节对齐的)
        for (SpaceData d : spaces) {
            if (d.getByteNum() % 4 == 0) sb.append(d).append("\n");
        }

        // 3. Unaligned Spaces
        for (SpaceData d : spaces) {
            if (d.getByteNum() % 4 != 0) sb.append(d).append("\n");
        }
        if (!spaces.isEmpty()) sb.append("\n");

        // 4. Bytes
        for (ByteData d : bytes) sb.append(d).append("\n");
        if (!bytes.isEmpty()) sb.append("\n");

        // 5. Ascii
        for (AsciiData d : ascii) sb.append(d).append("\n");
        sb.append("\n\n");

        sb.append(".text\n");
        for (InstrM instr : instructions) {
            sb.append(instr.getNote()); // 输出注释
            if (!(instr instanceof Label)) {
                sb.append("    ");
            }
            sb.append(instr).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
}