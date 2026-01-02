package error;

import java.util.HashMap;
import java.util.Map;

public enum ErrorType {
    ILLEGAL_SYMBOL,
    NAME_REDEFINE,
    NAME_UNDEFINED,
    FUNC_PARAM_NUM_ERR,
    FUNC_PARAM_TYPE_ERR,
    VOID_RETURN,
    MISS_RETURN,
    EDIT_CONST_VALUE,
    MISS_SEMICN,
    MISS_RPARENT,
    MISS_RBRACK,
    PRINTF_PARAM_NUM_ERR,
    BREAK_CONTINUE_ERR,
    UNDEFINED;

    private static final Map<ErrorType, String> ERROR_CODE_MAP = new HashMap<>();

    static {
        ERROR_CODE_MAP.put(ILLEGAL_SYMBOL, "a");
        ERROR_CODE_MAP.put(NAME_REDEFINE, "b");
        ERROR_CODE_MAP.put(NAME_UNDEFINED, "c");
        ERROR_CODE_MAP.put(FUNC_PARAM_NUM_ERR, "d");
        ERROR_CODE_MAP.put(FUNC_PARAM_TYPE_ERR, "e");
        ERROR_CODE_MAP.put(VOID_RETURN, "f");
        ERROR_CODE_MAP.put(MISS_RETURN, "g");
        ERROR_CODE_MAP.put(EDIT_CONST_VALUE, "h");
        ERROR_CODE_MAP.put(MISS_SEMICN, "i");
        ERROR_CODE_MAP.put(MISS_RPARENT, "j");
        ERROR_CODE_MAP.put(MISS_RBRACK, "k");
        ERROR_CODE_MAP.put(PRINTF_PARAM_NUM_ERR, "l");
        ERROR_CODE_MAP.put(BREAK_CONTINUE_ERR, "m");
        ERROR_CODE_MAP.put(UNDEFINED, "undefined");
    }

    @Override
    public String toString() {
        return ERROR_CODE_MAP.getOrDefault(this, "unknown");
    }
}