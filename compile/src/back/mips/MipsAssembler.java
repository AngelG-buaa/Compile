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
    // ==================== 数据段 ====================
    protected final List<WordData> words = new ArrayList<>();
    protected final List<ByteData> bytes = new ArrayList<>();
    protected final List<SpaceData> spaces = new ArrayList<>();
    protected final List<AsciiData> ascii = new ArrayList<>();

    // ==================== 代码段 ====================
    protected final List<InstrM> instructions = new ArrayList<>();

    // ==================== 上下文状态 ====================
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

    // ==================== 栈帧管理（核心对齐逻辑）====================

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
        // 使用位运算高效对齐（向下取整到 4 的倍数）
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

    // --- 内存访问（自动选择字节/字）---
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

    protected Addiu makeAddiu(Reg dest, Reg src, int imm) {
        // 封装 addiu：用于 SP/地址与小常量的相加，替代 li+addu 双指令
        Addiu instr = new Addiu(dest, src, imm);
        instructions.add(instr);
        return instr;
    }

    protected MemoryM makeLoadUnsignedByte(Reg dest, int offset, Reg base) {
        // 封装 lbu：从内存无符号装载字节，避免 lb 的符号扩展
        MemoryM instr = new Lbu(dest, offset, base);
        instructions.add(instr);
        return instr;
    }

    protected Andi makeAndi(Reg dest, Reg src, int imm) {
        // 封装 andi：用于寄存器内零扩展掩码 (0xFF) 等
        Andi instr = new Andi(dest, src, imm);
        instructions.add(instr);
        return instr;
    }

    protected Srl makeSrl(Reg dest, Reg src, int shift) {
        // 封装 srl：逻辑右移，常用于无符号位操作及除以 2 的幂
        Srl instr = new Srl(dest, src, shift);
        instructions.add(instr);
        return instr;
    }

    protected Sra makeSra(Reg dest, Reg src, int shift) {
        // 封装 sra：算术右移，保留符号位
        Sra instr = new Sra(dest, src, shift);
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

    // ==================== 输出生成（优化布局）====================
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // 输出前进行一次简单的窥孔优化：移除冗余 move，折叠 li+addu、li+addu+mem 等序列
        optimizeInstructions();

        sb.append(".data\n");
        // 1. .word 数据
        for (WordData d : words) sb.append(d).append("\n");
        if (!words.isEmpty()) sb.append("\n");

        // 2. 对齐的 .space（4 字节对齐）
        for (SpaceData d : spaces) {
            if (d.getByteNum() % 4 == 0) sb.append(d).append("\n");
        }

        // 3. 非对齐的 .space
        for (SpaceData d : spaces) {
            if (d.getByteNum() % 4 != 0) sb.append(d).append("\n");
        }
        if (!spaces.isEmpty()) sb.append("\n");

        // 4. .byte 数据
        for (ByteData d : bytes) sb.append(d).append("\n");
        if (!bytes.isEmpty()) sb.append("\n");

        // 5. .ascii 数据
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

    private void optimizeInstructions() {
        // 窥孔优化规则：
        // - 移除 move r, r
        // - 折叠 [li rC, imm] + [addu dst, base, rC] => addiu dst, base, imm
        // - 折叠 [li rC, imm] + [addu addr, base, rC] + [mem *, 0(addr)] => [mem *, imm(base)]
        // - 分支反转：beq/bne + j + 目标标签 => 反转分支指向 j 的目标，移除 j
        // - 字节零扩展：lb + andi 0xFF => lbu
        ArrayList<InstrM> out = new ArrayList<>();
        int i = 0;
        while (i < instructions.size()) {
            InstrM cur = instructions.get(i);
            // 移除自拷贝的 move 指令
            if (cur instanceof Move) {
                Move m = (Move) cur;
                if (m.getTo() == m.getFrom()) {
                    i++;
                    continue;
                }
            }

            // 将 addu 中与 $zero 的加法转为 move（更易读、便于后续清理）
            if (cur instanceof Addu) {
                Addu add = (Addu) cur;
                if (add.getLeft() == Reg.zero && add.hasDestination()) {
                    out.add(new Move(add.getDestination(), add.getRight()));
                    i++;
                    continue;
                }
                if (add.getRight() == Reg.zero && add.hasDestination()) {
                    out.add(new Move(add.getDestination(), add.getLeft()));
                    i++;
                    continue;
                }
            }

            // 折叠 [li rC, imm] + [addu dst, base, rC] => addiu dst, base, imm
            if (i + 1 < instructions.size() && cur instanceof Li && instructions.get(i + 1) instanceof Addu) {
                Li li = (Li) cur;
                Addu add = (Addu) instructions.get(i + 1);
                Reg immReg = li.getTo();
                int imm = li.getFrom();
                if (imm >= -32768 && imm <= 32767) {
                    if (add.getRight() == immReg) {
                        out.add(new Addiu(add.getDestination(), add.getLeft(), imm));
                        i += 2;
                        continue;
                    } else if (add.getLeft() == immReg) {
                        out.add(new Addiu(add.getDestination(), add.getRight(), imm));
                        i += 2;
                        continue;
                    }
                }
            }

            // 移除被下一条对同一寄存器的 li 覆盖的冗余 li
            if (i + 1 < instructions.size() && cur instanceof Li && instructions.get(i + 1) instanceof Li) {
                Li li1 = (Li) cur;
                Li li2 = (Li) instructions.get(i + 1);
                if (li1.getTo() == li2.getTo()) {
                    i++;
                    continue;
                }
            }

            // 移除 addiu r, r, 0 的空操作
            if (cur instanceof Addiu) {
                Addiu a = (Addiu) cur;
                if (a.getImmediate() == 0 && a.getDestination() == a.getSource()) {
                    i++;
                    continue;
                }
            }

            // 分支反转以消除紧随其后的 j 并让程序自然落入目标标签
            if (i + 2 < instructions.size() && cur instanceof back.mips.instruction.branch.BranchM && instructions.get(i + 1) instanceof J && instructions.get(i + 2) instanceof back.mips.data.Label) {
                back.mips.instruction.branch.BranchM br = (back.mips.instruction.branch.BranchM) cur;
                J jmp = (J) instructions.get(i + 1);
                back.mips.data.Label lbl = (back.mips.data.Label) instructions.get(i + 2);
                Object destObj = jmp.getDestination();
                if (destObj instanceof String) {
                    String jDest = (String) destObj;
                    if (lbl.getIdentifier().equals(br.getTargetLabel())) {
                        InstrM replaced;
                        if (br instanceof back.mips.instruction.branch.Beq) {
                            replaced = new back.mips.instruction.branch.Bne(br.getLeft(), br.getRight(), jDest);
                        } else if (br instanceof back.mips.instruction.branch.Bne) {
                            replaced = new back.mips.instruction.branch.Beq(br.getLeft(), br.getRight(), jDest);
                        } else {
                            replaced = null;
                        }
                        if (replaced != null) {
                            out.add(replaced);
                            i += 2; // 跳过 j，保留后续的标签
                            continue;
                        }
                    }
                }
            }

            // 折叠 lb + andi 0xFF 为 lbu，避免额外的零扩展指令
            if (i + 1 < instructions.size() && cur instanceof Lb && instructions.get(i + 1) instanceof back.mips.instruction.compute.Andi) {
                Lb lb = (Lb) cur;
                back.mips.instruction.compute.Andi andi = (back.mips.instruction.compute.Andi) instructions.get(i + 1);
                if (andi.getImmediate() == 0xFF && andi.getSource() == lb.getTargetReg() && andi.getDestination() == lb.getTargetReg()) {
                    out.add(new Lbu(lb.getTargetReg(), lb.getOffset(), lb.getBaseReg()));
                    i += 2;
                    continue;
                }
            }

            // 合并连续的 addiu：addiu r, base, imm1; addiu r, r, imm2 => addiu r, base, (imm1+imm2)
            if (i + 1 < instructions.size() && cur instanceof Addiu && instructions.get(i + 1) instanceof Addiu) {
                Addiu a1 = (Addiu) cur;
                Addiu a2 = (Addiu) instructions.get(i + 1);
                if (a1.getDestination() == a2.getDestination() && a2.getSource() == a2.getDestination()) {
                    long sum = (long) a1.getImmediate() + (long) a2.getImmediate();
                    if (sum >= -32768 && sum <= 32767) {
                        out.add(new Addiu(a1.getDestination(), a1.getSource(), (int) sum));
                        i += 2;
                        continue;
                    }
                }
            }

            // 折叠 [li rC, imm] + [addu addr, base, rC] + [mem *, 0(addr)] => [mem *, imm(base)]
            if (i + 2 < instructions.size() && cur instanceof Li && instructions.get(i + 1) instanceof Addu) {
                InstrM third = instructions.get(i + 2);
                if (third instanceof MemoryM) {
                    Li li = (Li) cur;
                    Addu add = (Addu) instructions.get(i + 1);
                    MemoryM mem = (MemoryM) third;
                    Reg immReg = li.getTo();
                    int imm = li.getFrom();
                    if (imm >= -32768 && imm <= 32767 && mem.getOffset() == 0 && mem.getBaseReg() == add.getDestination()) {
                        Reg base = add.getLeft() == immReg ? add.getRight() : (add.getRight() == immReg ? add.getLeft() : null);
                        if (base != null) {
                            InstrM replaced;
                            if (mem instanceof Lw) replaced = new Lw(mem.getTargetReg(), imm, base);
                            else if (mem instanceof Lb) replaced = new Lb(mem.getTargetReg(), imm, base);
                            else if (mem instanceof Lbu) replaced = new Lbu(mem.getTargetReg(), imm, base);
                            else if (mem instanceof Sw) replaced = new Sw(mem.getTargetReg(), imm, base);
                            else if (mem instanceof Sb) replaced = new Sb(mem.getTargetReg(), imm, base);
                            else replaced = null;
                            if (replaced != null) {
                                out.add(replaced);
                                i += 3;
                                continue;
                            }
                        }
                    }
                }
            }

            // 折叠 [addiu addr, base, imm] + [mem *, 0(addr)] => [mem *, imm(base)]
            if (i + 1 < instructions.size() && cur instanceof Addiu && instructions.get(i + 1) instanceof MemoryM) {
                Addiu addiu = (Addiu) cur;
                MemoryM mem = (MemoryM) instructions.get(i + 1);
                if (mem.getOffset() == 0 && mem.getBaseReg() == addiu.getDestination()) {
                    int imm = addiu.getImmediate();
                    Reg base = addiu.getSource();
                    InstrM replaced;
                    if (mem instanceof Lw) replaced = new Lw(mem.getTargetReg(), imm, base);
                    else if (mem instanceof Lb) replaced = new Lb(mem.getTargetReg(), imm, base);
                    else if (mem instanceof Lbu) replaced = new Lbu(mem.getTargetReg(), imm, base);
                    else if (mem instanceof Sw) replaced = new Sw(mem.getTargetReg(), imm, base);
                    else if (mem instanceof Sb) replaced = new Sb(mem.getTargetReg(), imm, base);
                    else replaced = null;
                    if (replaced != null) {
                        out.add(replaced);
                        i += 2;
                        continue;
                    }
                }
            }

            // 若下一条为目标标签，移除 j TARGET
            if (i + 1 < instructions.size() && cur instanceof J && instructions.get(i + 1) instanceof back.mips.data.Label) {
                J jmp = (J) cur;
                back.mips.data.Label lbl = (back.mips.data.Label) instructions.get(i + 1);
                Object dest = jmp.getDestination();
                if (dest instanceof String && ((String) dest).equals(lbl.getIdentifier())) {
                    i++;
                    continue;
                }
            }

            // 折叠 [la addr, label] + [mem *, 0(addr)] => [mem *, label]
            if (i + 1 < instructions.size() && cur instanceof La && instructions.get(i + 1) instanceof MemoryM) {
                La la = (La) cur;
                MemoryM mem = (MemoryM) instructions.get(i + 1);
                if (mem.getOffset() == 0 && mem.getBaseReg() == la.getTo()) {
                    String label = la.getFrom();
                    InstrM replaced;
                    if (mem instanceof Lw) replaced = new LwLabel(mem.getTargetReg(), label);
                    else if (mem instanceof Lb) replaced = new LbLabel(mem.getTargetReg(), label);
                    else if (mem instanceof Lbu) replaced = new LbuLabel(mem.getTargetReg(), label);
                    else if (mem instanceof Sw) replaced = new SwLabel(mem.getTargetReg(), label);
                    else if (mem instanceof Sb) replaced = new SbLabel(mem.getTargetReg(), label);
                    else replaced = null;
                    if (replaced != null) {
                        out.add(replaced);
                        i += 2;
                        continue;
                    }
                }
            }

            // 折叠 [la addr, label] + [addiu addr, addr, imm] + [mem *, 0(addr)] => [mem *, label+imm]
            if (i + 2 < instructions.size() && cur instanceof La && instructions.get(i + 1) instanceof Addiu && instructions.get(i + 2) instanceof MemoryM) {
                La la = (La) cur;
                Addiu addiu = (Addiu) instructions.get(i + 1);
                MemoryM mem = (MemoryM) instructions.get(i + 2);
                if (addiu.getDestination() == la.getTo() && addiu.getSource() == la.getTo() && mem.getOffset() == 0 && mem.getBaseReg() == addiu.getDestination()) {
                    String label = la.getFrom();
                    int imm = addiu.getImmediate();
                    InstrM replaced;
                    if (mem instanceof Lw) replaced = new LwLabelOff(mem.getTargetReg(), label, imm);
                    else if (mem instanceof Lb) replaced = new LbLabelOff(mem.getTargetReg(), label, imm);
                    else if (mem instanceof Lbu) replaced = new LbuLabelOff(mem.getTargetReg(), label, imm);
                    else if (mem instanceof Sw) replaced = new SwLabelOff(mem.getTargetReg(), label, imm);
                    else if (mem instanceof Sb) replaced = new SbLabelOff(mem.getTargetReg(), label, imm);
                    else replaced = null;
                    if (replaced != null) {
                        out.add(replaced);
                        i += 3;
                        continue;
                    }
                }
            }

            out.add(cur);
            i++;
        }
        instructions.clear();
        instructions.addAll(out);
    }
}
