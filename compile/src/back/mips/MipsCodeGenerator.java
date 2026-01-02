package back.mips;

import back.mips.register.Reg;
import middle.llvm.IRModule;
import middle.llvm.type.PointerType;
import middle.llvm.type.ArrayType;
import middle.llvm.type.IRType;
import middle.llvm.type.IntegerType;
import middle.llvm.type.VoidType;
import middle.llvm.value.*;
import middle.llvm.value.constant.IRConstant;
import middle.llvm.value.constant.IntegerConstant;
import middle.llvm.value.instruction.*;
import utils.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MIPS 代码生成器
 * <p>负责将 LLVM IR 转换为 MIPS 汇编代码</p>
 * <ul>
 *   <li>处理全局变量和字符串字面量</li>
 *   <li>处理函数定义和指令生成</li>
 *   <li>管理寄存器分配和栈帧布局</li>
 * </ul>
 */
public class MipsCodeGenerator extends MipsAssembler {

    private static class Holder {
        private static final MipsCodeGenerator INSTANCE = new MipsCodeGenerator();
    }

    /**
     * 获取 MipsCodeGenerator 的单例实例
     * @return MipsCodeGenerator 实例
     */
    public static MipsCodeGenerator getInstance() {
        return Holder.INSTANCE;
    }

    private MipsCodeGenerator() {
        super();
    }

    /**
     * 生成 MIPS 代码的主入口
     * @param module LLVM IR 模块
     * @return 生成的 MIPS 汇编代码字符串
     */
    public String generateMipsCode(IRModule module) {
        processGlobals(module);
        processFunctions(module);
        return super.toString();
    }

    // ==================== 全局变量处理 ====================

    /**
     * 处理全局变量和字符串常量
     * @param module LLVM IR 模块
     */
    private void processGlobals(IRModule module) {
        for (IRStringLiteral str : module.getStringLiterals()) {
            // 去除名称中的 '@' 前缀，并处理换行符转义
            makeAsciiData(str.getName().substring(1), str.getOriginalLiteral().replace("\n", "\\n"));
        }
        for (IRGlobalVariable global : module.getGlobalVariables()) {
            genGlobalData(global.getName().substring(1), global.getInit());
        }
        // 静态变量类似处理
        for (IRStaticVariable staticVar : module.getStaticVariables()) {
            genGlobalData(staticVar.getName().substring(1), staticVar.getInit());
        }
    }

    /**
     * 生成全局数据段内容
     * @param name 变量名
     * @param init 初始化常量
     */
    private void genGlobalData(String name, IRConstant init) {
        // 检查初始化值是否全为 0，若是则使用 .space 指令以节省空间
        ArrayList<Integer> vals = new ArrayList<>(init.getAllNumbers());
        boolean isAllZero = vals.stream().allMatch(v -> v == 0);

        if (isAllZero) {
            // 若全部为 0，使用 .space 指令分配指定大小的零初始化空间
            int size = init.getType().getByteSize(); // 获取总字节大小
            makeSpaceData(name, size);
        } else if (init.containsCharacterType()) {
            // 若包含字符类型（通常是字符串常量），使用 .byte 指令
            makeByteData(name, vals);
        } else {
            // 默认情况，使用 .word 指令
            makeWordData(name, vals);
        }
    }

    // ==================== 函数处理 ====================

    /**
     * 处理模块中的所有函数
     * @param module LLVM IR 模块
     */
    private void processFunctions(IRModule module) {
        List<IRFunction> funcs = module.getFunctionDefinitions();
        if (funcs.isEmpty()) return;

        // 优先生成 main 函数 (假设在列表最后)
        generateFunctionCode(funcs.get(funcs.size() - 1));

        for (int i = 0; i < funcs.size() - 1; i++) {
            generateFunctionCode(funcs.get(i));
        }
    }

    /**
     * 生成单个函数的 MIPS 代码
     * @param func IR 函数对象
     */
    protected void generateFunctionCode(IRFunction func) {
        beginFunction(func);

        // 初始化寄存器映射 (来自 RegAlloca 的 $t 寄存器分配)
        if (func.getValue2reg() != null) {
            this.valRegs = new HashMap<>(func.getValue2reg());
        }

        handleParameters(func);
        preAllocateStack(func);

        for (IRBasicBlock bb : func.getBasicBlocks()) {
            makeLabel(bb.getName().substring(1));
            for (IRInstruction instr : bb.getAllInstructions()) {
                dispatchInstruction(instr);
            }
        }
    }

    /**
     * 处理函数参数
     * <p>处理参数在栈上的分配以及从寄存器/栈中获取参数值</p>
     * @param func IR 函数对象
     */
    private void handleParameters(IRFunction func) {
        List<IRFunctionParameter> params = func.getParameters();
        int argCount = params.size();
        
        for (int i = 0; i < params.size(); i++) {
            IRFunctionParameter param = params.get(i);

            if (i < 4) {
                // 情况 1: 前 4 个参数 (通过寄存器 $a0-$a3 传递)
                
                // 1. 分配栈空间 (Shadow Space / Backup Storage)
                // 即使参数在寄存器中，我们通常也为其在当前栈帧分配空间，
                // 以便在寄存器不足或需要取地址时将其保存到栈上。
                int align = (param.getType() == IntegerType.I8) ? 1 : 4;
                subAlign(param.getType().getByteSize(), align);
                putOffset(param, currentOffset);

                Reg argReg = Reg.getArgReg(i);

                // 2. 立即将参数寄存器值保存到栈 (Store) 
                // 此时 SP 还没动，是基于初始 SP (当前帧底) 写入。
                // 这确保了如果后续代码需要 Spill 该参数，栈上有正确的值。
                int size = param.getType().getByteSize();
                int storeAlign = (size == 1) ? 1 : 4;
                makeStore(storeAlign, argReg, currentOffset, Reg.sp);

                // 3. 如果 RegAlloca 为该参数分配了长期驻留的寄存器 (如 $sX 或 $tX)，搬运过去
                Reg allocatedReg = findReg(param);
                if (allocatedReg != null) {
                    if (allocatedReg != argReg) {
                        // 将参数从 $aX 移动到分配的寄存器
                        makeMove(allocatedReg, argReg);
                    }
                    // 注意：这里不需要再 putReg，因为 allocatedReg 已经在 valRegs 里了
                }
            } else {
                // 情况 2: 第 5 个及以后的参数 (通过栈传递)
                // 这些参数已经由 Caller 压入栈中，位于当前 SP (参数区底部) 的上方。
                
                // 计算相对于当前 SP 的偏移 (注意栈是向下增长的，但参数区是向上排列的)
                // p[argCount-1] at 0($sp)
                // p[4] at (argCount - 1 - 4) * 4 ($sp)
                int offset = (argCount - 1 - i) * 4;
                putOffset(param, offset);
                
                // 如果该参数被分配了寄存器，需要从栈上 Load 进来
                Reg allocatedReg = findReg(param);
                if (allocatedReg != null) {
                    makeLoad(4, allocatedReg, offset, Reg.sp);
                }
            }
        }
    }

    /**
     * 预先分配栈空间
     * <p>为没有分配寄存器的指令结果分配栈上空间</p>
     * @param func IR 函数对象
     */
    private void preAllocateStack(IRFunction func) {
        for (IRInstruction instr : func.getAllInstructions()) {
            // 如果指令有返回值(非void)，且未分配寄存器，且未分配偏移 -> 分配栈
            // 这些值通常是计算的中间结果，如果不能全在寄存器中，就需要溢出到栈
            if (!(instr.getType() instanceof VoidType) && findReg(instr) == null && findOffset(instr) == null) {
                int size = instr.getType().getByteSize();
                int align = (instr.getType() == IntegerType.I8) ? 1 : 4; // 根据类型决定对齐
                subAlign(size, align);
                putOffset(instr, currentOffset);
            }
            // 注意：CopyInstruction 的 Target 也要检查
            if (instr instanceof CopyInstruction) {
                IRValue target = ((CopyInstruction) instr).getTargetValue();
                if (findReg(target) == null && findOffset(target) == null) {
                    int size = target.getType().getByteSize();
                    int align = (target.getType() == IntegerType.I8) ? 1 : 4;
                    subAlign(size, align);
                    putOffset(target, currentOffset);
                }
            }
        }
    }

    // ==================== 指令映射 ====================

    /**
     * 分发指令生成任务
     * @param instr IR 指令
     */
    protected void dispatchInstruction(IRInstruction instr) {
        if (instr instanceof BinaryOperationInstruction) mapCompute((BinaryOperationInstruction) instr);
        else if (instr instanceof AllocaInstruction) mapAlloca((AllocaInstruction) instr);
        else if (instr instanceof CallInstruction) mapCall((CallInstruction) instr);
        else if (instr instanceof GetElementPtrInstruction) mapGep((GetElementPtrInstruction) instr);
        else if (instr instanceof CompareInstruction) mapIcmp((CompareInstruction) instr);
        else if (instr instanceof ReturnInstruction) mapRet((ReturnInstruction) instr);
        else if (instr instanceof TruncateInstruction) mapTrunc((TruncateInstruction) instr);
        else if (instr instanceof CopyInstruction) mapCopy((CopyInstruction) instr);
        else if (instr instanceof ZeroExtendInstruction) mapZext((ZeroExtendInstruction) instr);
        else if (instr instanceof BranchInstruction) mapBranch((BranchInstruction) instr);
        else if (instr instanceof JumpInstruction) mapJump((JumpInstruction) instr);
        else if (instr instanceof StoreInstruction) mapStore((StoreInstruction) instr);
        else if (instr instanceof LoadInstruction) mapLoad((LoadInstruction) instr);
    }

    /**
     * 将 IR 值加载到指定寄存器
     * @param val IR 值
     * @param r 目标寄存器
     */
    private void loadValToReg(IRValue val, Reg r) {
        if (val instanceof IntegerConstant) {
            // 如果是立即数，直接加载 (li)
            makeLi(r, ((IntegerConstant) val).getConstantValue());
        } else if (findReg(val) != null) {
            // 如果值已经在寄存器中，直接移动 (move)
            if (findReg(val) != r) makeMove(r, findReg(val));
        } else {
            // 如果值在栈上，加载到寄存器 (lw/lb)
            // 注意区分类型大小/对齐 (1字节 vs 4字节)
            int align = (val.getType() == IntegerType.I8) ? 1 : 4;
            makeLoad(align, r, findOffset(val), Reg.sp);
        }
    }

    /**
     * 映射二元运算指令
     * @param instr 二元运算指令
     */
    public void mapCompute(BinaryOperationInstruction instr) {
        Reg target = findReg(instr);
        Reg r1 = Reg.k0, r2 = Reg.k1;
        loadValToReg(instr.getOperand(0), r1);
        loadValToReg(instr.getOperand(1), r2);

        if (target != null) {
            // 如果目标分配了寄存器，直接写入目标寄存器
            makeCompute(instr, target, r1, r2);
        } else {
            // 如果目标在栈上，先计算到临时寄存器 k0，再存入栈
            makeCompute(instr, r1, r1, r2); // 计算结果暂存 k0 (这里复用了 r1，即 k0)
            makeStore(4, r1, findOffset(instr), Reg.sp); // i32 运算结果存栈
        }
    }

    /**
     * 映射 Alloca 指令
     * @param alloca Alloca 指令
     */
    public void mapAlloca(AllocaInstruction alloca) {
        IRType pointee = ((PointerType) alloca.getType()).getPointeeType();
        int size = pointee.getByteSize();
        // 根据被分配类型决定对齐
        int align = (pointee == IntegerType.I8) ? 1 : 4;

        // 运行时在栈上开辟空间 (移动 currentOffset)
        subAlign(size, align);

        // Alloca 指令本身返回的是地址(指针)，这个地址值(offset)需要存起来
        // 该地址是相对于当前函数栈底(SP)的动态偏移
        Reg target = findReg(alloca);
        
        if (target != null) {
            // 计算绝对地址：SP + currentOffset
            makeLi(target, currentOffset).setNote(new Note(alloca));
            makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, target, Reg.sp, target);
        } else {
            // 如果 alloca 指针本身也存栈上
            makeLi(Reg.k0, currentOffset).setNote(new Note(alloca));
            makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, Reg.k0, Reg.sp, Reg.k0);
            makeStore(4, Reg.k0, findOffset(alloca), Reg.sp); // 指针本身是4字节
        }
    }

    /**
     * 映射函数调用指令
     * @param call 函数调用指令
     */
    public void mapCall(CallInstruction call) {
        IRFunction func = (IRFunction) call.getCalledFunction();
        String name = func.getName().substring(1);

        // 1. Syscall 特殊处理 (保持不变)
        if (handleSyscall(call, name, func)) return;

        // ================== 调用约定处理 ==================

        // 2. 确定需要保存的寄存器 (Caller-Saved)
        // 包括两部分：
        // A. RegAlloca 分析出的活跃变量 (主要是 $t 寄存器)
        //    这些变量在函数调用后仍然存活，因此必须保存。
        Set<Reg> regsToSaveSet = new HashSet<>(call.liveRegSet);

        // B. 当前占据 $a0-$a3 的参数变量 (因为即将进行函数调用，这些寄存器会被覆盖)
        //    我们遍历当前所有的寄存器映射，找出位于 $a0-$a3 的变量。
        //    这些变量如果不保存，在设置新函数参数时就会被覆盖。
        List<IRValue> argsInRegsToSpill = new ArrayList<>();

        for (Map.Entry<IRValue, Reg> entry : valRegs.entrySet()) {
            Reg r = entry.getValue();
            int rIdx = Reg.getIndex(r);
            // 如果是 $a0(4) - $a3(7)，且不在 RegAlloca 的集合中(通常也不在)，必须强制保存
            if (rIdx >= 4 && rIdx <= 7) {
                regsToSaveSet.add(r);
                argsInRegsToSpill.add(entry.getKey());
            }
        }

        ArrayList<Reg> saved = new ArrayList<>(regsToSaveSet);
        // 排序以保持输出确定性
        saved.sort(Comparator.comparingInt(Reg::getIndex));

        // 3. 执行保存动作 (Spill)
        // 3.1 保存普通的 Caller-Saved 寄存器 (主要是 $t) -> 压入新栈帧的顶部区域
        //     这些寄存器保存到当前栈帧的动态增长区。
        int savedRegsSize = saved.size() * 4;
        for (int i = 0; i < saved.size(); i++) {
            makeStore(4, saved.get(i), currentOffset - 4 * (i + 1), Reg.sp);
        }

        // 3.2 [关键] 将位于 $a0-$a3 的变量归位到它们自己的栈槽 (Backup Home)
        //     因为对于函数参数，我们在 handleParameters 里分配了固定位置，而不是像 $t 那样临时压栈。
        //     这里将它们“归位”到那个固定位置。
        for (IRValue val : argsInRegsToSpill) {
            Reg r = findReg(val); // 肯定是 a0-a3
            Integer offset = findOffset(val); // 找它在函数开头分配的固定栈位置
            if (offset != null) {
                // 将寄存器值刷回栈
                makeStore(4, r, offset, Reg.sp);
                // 关键：暂时从寄存器映射中移除它，因为马上寄存器就要被覆盖了
                // 这样后续代码(如passArgs)就会强制去栈上取值，避免取到被覆盖后的错误值
                // 注意：这里只是临时移除映射，还是要在 valOffsets 里保留偏移
                valRegs.remove(val);
            }
        }

        // 4. 计算栈帧布局
        int argCount = call.getOperandCount() - 1;
        // 计算需要通过栈传递的参数数量 (>4 的部分)
        int stackArgsCount = (argCount > 4) ? (argCount - 4) : 0;
        int stackArgsSize = stackArgsCount * 4;
        // 总栈帧大小 = 保存的寄存器 + RA + 栈参数区
        int frameSize = savedRegsSize + 4 + stackArgsSize; // +4 is for RA

        // 5. 保存 RA
        makeStore(4, Reg.ra, currentOffset - savedRegsSize - 4, Reg.sp);

        // 6. 传递参数 (passArgs)
        // 这里需要微调：因为刚才我们把 $a0-$a3 的旧值存回栈并移除了映射，
        // 所以这里取参数时，如果发现 map 里没了，会自动去栈上 load (makeLoad)，这是安全的。
        for (int i = 0; i < argCount; i++) {
            IRValue arg = call.getOperand(i + 1);

            // 目标寄存器或栈位置
            if (i < 4) {
                // 前4个参数 -> $a0-$a3
                Reg argReg = Reg.getArgReg(i);
                loadValueToTarget(arg, argReg);
            } else {
                // 后续参数 -> 压栈
                Reg temp = Reg.v1; // 使用 v1 搬运，避免干扰参数寄存器
                loadValueToTarget(arg, temp);
                // 计算在目标栈帧中的位置 (注意这里的 currentOffset 是基于当前函数的)
                int offset = currentOffset - savedRegsSize - 4 - (i - 3) * 4;
                makeStore(4, temp, offset, Reg.sp);
            }
        }

        // 7. 移动 SP, 跳转 Jal, 恢复 RA, 恢复 SP
        // 移动 SP 到新栈帧底部
        makeLi(Reg.k0, currentOffset - frameSize);
        makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, Reg.sp, Reg.sp, Reg.k0);

        makeJal(name);

        // 调用返回后，恢复 RA (注意它在栈中的位置，此时 SP 还在新栈帧底部，需要根据偏移找回)
        makeLoad(4, Reg.ra, stackArgsSize, Reg.sp); // 恢复 RA

        // 恢复 SP 到原栈帧位置
        makeLi(Reg.k0, -(currentOffset - frameSize));
        makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, Reg.sp, Reg.sp, Reg.k0);


        // 8. 恢复上下文 (Restore)
        // 8.1 恢复 $t 等寄存器 (从临时保存区)
        for (int i = 0; i < saved.size(); i++) {
            makeLoad(4, saved.get(i), currentOffset - 4 * (i + 1), Reg.sp);
        }

        // 8.2 [关键] 恢复 $a0-$a3 的映射
        // 不需要写代码去 load 回 $a0-$a3，采用 Lazy Reload 策略。
        // 因为这些变量已经被存回栈里的固定位置 (Backup Home)，
        // 后续指令如果需要用到它们，会发现寄存器映射没了，自动生成 Load 指令从栈里取。
        // 这避免了不必要的 Load 操作。

        // 9. 处理返回值
        if (!(call.getType() instanceof VoidType)) {
            // 将 $v0 的返回值移动到目标寄存器或存入栈
            if (findReg(call) != null) makeMove(findReg(call), Reg.v0);
            else makeStore(call.getType().getByteSize(), Reg.v0, findOffset(call), Reg.sp);
        }
    }

    /**
     * 辅助函数：统一处理加载逻辑
     * @param val 源 IR 值
     * @param target 目标寄存器
     */
    private void loadValueToTarget(IRValue val, Reg target) {
        if (val instanceof IntegerConstant) {
            makeLi(target, ((IntegerConstant) val).getConstantValue());
        } else {
            Reg src = findReg(val);
            if (src != null) {
                if (src != target) makeMove(target, src);
            } else {
                // 从栈加载
                int align = (val.getType() == IntegerType.I8) ? 1 : 4;
                makeLoad(align, target, findOffset(val), Reg.sp);
            }
        }
    }

    /**
     * 处理系统调用
     * @param call 调用指令
     * @param name 函数名
     * @param func 函数对象
     * @return 是否为系统调用
     */
    private boolean handleSyscall(CallInstruction call, String name, IRFunction func) {
        if (name.equals("getint") || name.equals("getchar")) {
            // 输入类系统调用
            makeLi(Reg.v0, name.equals("getint") ? 5 : 12);
            makeSyscall();
            // 处理返回值
            if (findReg(call) != null) makeMove(findReg(call), Reg.v0);
            else makeStore(func.getReturnType().getByteSize(), Reg.v0, findOffset(call), Reg.sp);
            return true;
        } else if (name.equals("putint") || name.equals("putch")) {
            // 输出类系统调用 (int/char)
            makeMove(Reg.k1, Reg.a0); // 保护 a0，因为 syscall 使用 a0 传参
            loadValToReg(call.getOperand(1), Reg.a0);
            makeLi(Reg.v0, name.equals("putint") ? 1 : 11);
            makeSyscall();
            makeMove(Reg.a0, Reg.k1); // 恢复 a0
            return true;
        } else if (name.equals("putstr")) {
            // 输出字符串
            makeMove(Reg.k1, Reg.a0);
            IRValue arg = call.getOperand(1);
            if (arg instanceof IRStringLiteral) makeLa(Reg.a0, arg.getName().substring(1));
            else loadValToReg(arg, Reg.a0);
            makeLi(Reg.v0, 4);
            makeSyscall();
            makeMove(Reg.a0, Reg.k1);
            return true;
        }
        return false;
    }

    /**
     * 映射 GEP (GetElementPtr) 指令
     * @param gep GEP 指令
     */
    public void mapGep(GetElementPtrInstruction gep) {
        Reg baseReg = Reg.k0;
        IRValue base = gep.getBasePointer();

        // 1. 加载基地址
        if (base instanceof IRGlobalVariable || base instanceof IRStaticVariable || base instanceof IRStringLiteral) {
            makeLa(baseReg, base.getName().substring(1));
        } else if (findReg(base) != null) {
            baseReg = findReg(base);
        } else {
            makeLoad(4, baseReg, findOffset(base), Reg.sp);
        }

        // 2. 计算偏移量
        Reg offsetReg = Reg.k1;
        makeLi(offsetReg, 0);

        IRType type = ((PointerType) base.getType()).getPointeeType();
        for (int i = 0; i < gep.getIndexCount(); i++) {
            IRValue idx = gep.getIndex(i);
            int elementSize;
            if (i == 0) {
                // 第一个索引通常是指针移动，大小由 pointeeType 决定
                // ptr[i] -> address = ptr + i * sizeof(pointee)
                elementSize = type.getByteSize();
            } else {
                // 后续索引是数组内部移动
                // array[i][j] -> 第二个索引 j 的步长是 sizeof(element)
                if (type instanceof ArrayType) {
                    type = ((ArrayType) type).getElementType();
                    elementSize = type.getByteSize();
                } else {
                    elementSize = type.getByteSize();
                }
            }
            addIndexOffset(offsetReg, idx, elementSize);
        }

        // 3. 基地址 + 偏移量 -> 目标地址
        if (findReg(gep) != null) {
            makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, findReg(gep), baseReg, offsetReg);
        } else {
            makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, Reg.k0, baseReg, offsetReg);
            makeStore(4, Reg.k0, findOffset(gep), Reg.sp);
        }
    }

    /**
     * 计算并添加索引偏移
     * @param offsetReg 偏移寄存器
     * @param idx 索引值
     * @param size 元素大小
     */
    private void addIndexOffset(Reg offsetReg, IRValue idx, int size) {
        if (idx instanceof IntegerConstant) {
            // 常量索引优化
            int val = ((IntegerConstant) idx).getConstantValue();
            if (val == 0) return; // 偏移为0，忽略
            makeLi(Reg.t0, val * size);
            makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, offsetReg, offsetReg, Reg.t0);
        } else {
            // 变量索引
            Reg idxReg = Reg.t0;
            if (findReg(idx) != null) idxReg = findReg(idx);
            else makeLoad(4, idxReg, findOffset(idx), Reg.sp);

            if (size == 4) {
                // 如果元素大小是 4，用左移 2 位代替乘法
                makeSll(Reg.t2, idxReg, 2);
                makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, offsetReg, offsetReg, Reg.t2);
            } else {
                // 通用乘法
                makeLi(Reg.t1, size);
                makeCompute(BinaryOperationInstruction.BinaryOperator.MUL, Reg.t2, idxReg, Reg.t1);
                makeCompute(BinaryOperationInstruction.BinaryOperator.ADD, offsetReg, offsetReg, Reg.t2);
            }
        }
    }

    /**
     * 映射 Store 指令
     * @param store Store 指令
     */
    public void mapStore(StoreInstruction store) {
        IRValue src = store.getOperand(0);
        IRValue dest = store.getOperand(1);
        Reg data = Reg.k0;
        Reg addr = Reg.k1;

        // 1. 准备地址
        if (dest instanceof IRGlobalVariable || dest instanceof IRStaticVariable) {
            makeLa(addr, dest.getName().substring(1));
        } else if (findReg(dest) != null) {
            addr = findReg(dest);
        } else {
            makeLoad(4, addr, findOffset(dest), Reg.sp);
        }

        // 2. 准备数据
        if (src instanceof IntegerConstant) {
            makeLi(data, ((IntegerConstant) src).getConstantValue());
        } else if (findReg(src) != null) {
            data = findReg(src);
        } else {
            int align = (src.getType() == IntegerType.I8) ? 1 : 4;
            makeLoad(align, data, findOffset(src), Reg.sp);
        }

        // 3. 存储 (区分 sw 和 sb)
        int align = (src.getType() == IntegerType.I8) ? 1 : 4;
        makeStore(align, data, 0, addr);
    }

    /**
     * 映射 Load 指令
     * @param load Load 指令
     */
    public void mapLoad(LoadInstruction load) {
        IRValue src = load.getOperand(0);
        Reg addr = Reg.k0;

        // 1. 准备地址
        if (src instanceof IRGlobalVariable || src instanceof IRStaticVariable) {
            makeLa(addr, src.getName().substring(1));
        } else if (findReg(src) != null) {
            addr = findReg(src);
        } else {
            makeLoad(4, addr, findOffset(src), Reg.sp);
        }

        // 2. 加载 (区分 lw 和 lb)
        Reg target = findReg(load);
        int align = (load.getType() == IntegerType.I8) ? 1 : 4;

        if (target != null) {
            makeLoad(align, target, 0, addr);
        } else {
            makeLoad(align, Reg.k0, 0, addr);
            makeStore(align, Reg.k0, findOffset(load), Reg.sp);
        }
    }

    /**
     * 映射比较指令 (Icmp)
     * @param icmp 比较指令
     */
    public void mapIcmp(CompareInstruction icmp) {
        Reg r1 = Reg.k0, r2 = Reg.k1;
        loadValToReg(icmp.getLeftOperand(), r1);
        loadValToReg(icmp.getRightOperand(), r2);

        Reg target = findReg(icmp);
        if (target == null) target = Reg.k0;

        // 生成比较指令 (如 slt, seq 等)
        makeCompare(icmp.getCondition(), target, r1, r2);

        if (findReg(icmp) == null) {
            makeStore(1, target, findOffset(icmp), Reg.sp); // boolean is i1/i8 -> byte
        }
    }

    /**
     * 映射分支指令 (Branch)
     * @param br 分支指令
     */
    public void mapBranch(BranchInstruction br) {
        if (br.getOperandCount() > 1) { // Conditional
            IRValue cond = br.getOperand(0);
            String trueLbl = ((IRBasicBlock) br.getOperand(1)).getName().substring(1);
            String falseLbl = ((IRBasicBlock) br.getOperand(2)).getName().substring(1);

            if (cond instanceof IntegerConstant) {
                // 常量折叠：直接跳转
                makeJ(((IntegerConstant) cond).getConstantValue() != 0 ? trueLbl : falseLbl);
            } else {
                Reg condReg = findReg(cond);
                if (condReg == null) {
                    condReg = Reg.k0;
                    // Branch condition is i1 -> byte
                    makeLoad(1, condReg, findOffset(cond), Reg.sp);
                }
                // 如果条件为 0 (false)，跳转到 falseLbl
                makeBeq(condReg, Reg.zero, falseLbl);
                // 否则跳转到 trueLbl
                makeJ(trueLbl);
            }
        } else { // Unconditional
            String dest = ((IRBasicBlock) br.getOperand(0)).getName().substring(1);
            makeJ(dest);
        }
    }

    /**
     * 映射返回指令 (Return)
     * @param ret 返回指令
     */
    public void mapRet(ReturnInstruction ret) {
        if (currentFunction.getName().equals("@main")) {
            // main 函数返回时，调用 exit syscall
            makeLi(Reg.v0, 10);
            makeSyscall();
        } else {
            if (ret.hasReturnValue()) {
                IRValue val = ret.getOperand(0);
                if (val instanceof IntegerConstant) {
                    makeLi(Reg.v0, ((IntegerConstant) val).getConstantValue());
                } else if (findReg(val) != null) {
                    makeMove(Reg.v0, findReg(val));
                } else {
                    int align = (val.getType() == IntegerType.I8) ? 1 : 4;
                    makeLoad(align, Reg.v0, findOffset(val), Reg.sp);
                }
            }
            // 跳转回 RA
            makeJr(Reg.ra);
        }
    }

    /**
     * 映射拷贝指令 (Copy)
     * @param copy 拷贝指令
     */
    public void mapCopy(CopyInstruction copy) {
        IRValue src = copy.getSourceValue();
        IRValue dest = copy.getTargetValue();
        Reg destReg = findReg(dest); // Target reg
        Reg srcReg = findReg(src);   // Source reg if available

        if (src instanceof IntegerConstant) {
            int val = ((IntegerConstant) src).getConstantValue();
            if (destReg != null) makeLi(destReg, val);
            else {
                makeLi(Reg.k0, val);
                int align = (dest.getType() == IntegerType.I8) ? 1 : 4;
                makeStore(align, Reg.k0, findOffset(dest), Reg.sp);
            }
        } else {
            int align = (src.getType() == IntegerType.I8) ? 1 : 4;
            if (destReg != null) {
                if (srcReg != null) makeMove(destReg, srcReg);
                else makeLoad(align, destReg, findOffset(src), Reg.sp);
            } else {
                if (srcReg != null) makeStore(align, srcReg, findOffset(dest), Reg.sp);
                else {
                    makeLoad(align, Reg.k0, findOffset(src), Reg.sp);
                    makeStore(align, Reg.k0, findOffset(dest), Reg.sp);
                }
            }
        }
    }

    /**
     * 映射零扩展指令 (Zext)
     * @param zext 零扩展指令
     */
    public void mapZext(ZeroExtendInstruction zext) {
        Reg tmp = Reg.k0;
        // Zext from i1/i8 -> i32. Load as byte, store as word/reg
        loadValToReg(zext.getOperand(0), tmp); // This handles load byte if needed

        Reg target = findReg(zext);
        if (target == null) {
            makeStore(4, tmp, findOffset(zext), Reg.sp);
        } else if (target != tmp) {
            makeMove(target, tmp);
        }
    }

    /**
     * 映射截断指令 (Trunc)
     * @param trunc 截断指令
     */
    public void mapTrunc(TruncateInstruction trunc) {
        // Trunc i32 -> i8. Load word, store byte.
        Reg tmp = Reg.k0;
        loadValToReg(trunc.getOriginalValue(), tmp);

        Reg target = findReg(trunc);
        if (target == null) {
            makeStore(1, tmp, findOffset(trunc), Reg.sp);
        } else if (target != tmp) {
            makeMove(target, tmp);
        }
    }

    /**
     * 映射跳转指令 (Jump)
     * @param jump 跳转指令
     */
    private void mapJump(JumpInstruction jump) {
        String dest = ((IRBasicBlock) jump.getOperand(0)).getName().substring(1);
        makeJ(dest);
    }
}