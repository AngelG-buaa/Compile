package back.mips.register;

import java.util.HashMap;
import java.util.Map;

public class Reg {
    // General Purpose Registers
    public static final Reg zero = new Reg(0, "zero");
    public static final Reg at = new Reg(1, "at");
    public static final Reg v0 = new Reg(2, "v0");
    public static final Reg v1 = new Reg(3, "v1");
    public static final Reg a0 = new Reg(4, "a0");
    public static final Reg a1 = new Reg(5, "a1");
    public static final Reg a2 = new Reg(6, "a2");
    public static final Reg a3 = new Reg(7, "a3");
    public static final Reg t0 = new Reg(8, "t0");
    public static final Reg t1 = new Reg(9, "t1");
    public static final Reg t2 = new Reg(10, "t2");
    public static final Reg t3 = new Reg(11, "t3");
    public static final Reg t4 = new Reg(12, "t4");
    public static final Reg t5 = new Reg(13, "t5");
    public static final Reg t6 = new Reg(14, "t6");
    public static final Reg t7 = new Reg(15, "t7");
    public static final Reg s0 = new Reg(16, "s0");
    public static final Reg s1 = new Reg(17, "s1");
    public static final Reg s2 = new Reg(18, "s2");
    public static final Reg s3 = new Reg(19, "s3");
    public static final Reg s4 = new Reg(20, "s4");
    public static final Reg s5 = new Reg(21, "s5");
    public static final Reg s6 = new Reg(22, "s6");
    public static final Reg s7 = new Reg(23, "s7");
    public static final Reg t8 = new Reg(24, "t8");
    public static final Reg t9 = new Reg(25, "t9");
    public static final Reg k0 = new Reg(26, "k0");
    public static final Reg k1 = new Reg(27, "k1");
    public static final Reg gp = new Reg(28, "gp");
    public static final Reg sp = new Reg(29, "sp");
    public static final Reg fp = new Reg(30, "fp");
    public static final Reg ra = new Reg(31, "ra");

    private static final Reg[] ARGUMENT_REGISTERS = {a0, a1, a2, a3};
    
    private static final Map<Integer, String> REG_NAMES = new HashMap<>();
    
    static {
        Reg[] allRegs = {
            zero, at, v0, v1, a0, a1, a2, a3,
            t0, t1, t2, t3, t4, t5, t6, t7,
            s0, s1, s2, s3, s4, s5, s6, s7,
            t8, t9, k0, k1, gp, sp, fp, ra
        };
        
        for (Reg r : allRegs) {
            REG_NAMES.put(r.id, r.name);
        }
    }

    public static Reg getArgReg(int idx) {
        return (idx >= 0 && idx < ARGUMENT_REGISTERS.length) ? ARGUMENT_REGISTERS[idx] : null;
    }
   
    public static int getIndex(Reg reg) {
        return reg.id;
    }

    private final int id;
    private final String name;
    
    private Reg(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "$" + name;
    }
}
