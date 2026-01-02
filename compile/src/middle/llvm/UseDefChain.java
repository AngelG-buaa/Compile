package middle.llvm;

import middle.llvm.value.IRUser;
import middle.llvm.value.IRValue;

/**
 * Use-Def链实现
 * 表示值的使用和定义关系
 *
 * @param user 使用者
 * @param used 被使用的值
 */
public record UseDefChain(IRUser user, IRValue used) {

    /**
     * 获取使用者
     */
    @Override
    public IRUser user() {
        return user;
    }

    /**
     * 获取被使用的值
     */
    @Override
    public IRValue used() {
        return used;
    }
}