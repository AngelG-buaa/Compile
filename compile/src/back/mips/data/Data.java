package back.mips.data;

import back.mips.InstrM;
import java.util.Objects;

/**
 *  伪指令	分配单位	是否初始化	典型用途
 * .space	字节（任意数量）	不初始化	缓冲区、动态数据存储
 * .byte	字节（1 个）	初始化为指定值	定义小型数据或字符
 * .word	字（4 字节）	初始化为指定值	存储整数、数组或地址
 */
public abstract class Data extends InstrM {
    protected String identifier;

    public Data(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Data other = (Data) obj;
        return Objects.equals(identifier, other.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
