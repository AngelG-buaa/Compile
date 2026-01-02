package error;

public class Error {
    private final ErrorType type;
    private final int line;

    private Error(ErrorType type, int line) {
        this.type = type;
        this.line = line;
    }

    public static Error createError(ErrorType errorType, int lineNumber) {
        return new Error(errorType, lineNumber);
    }

    public ErrorType GetErrorType() {
        return type;
    }

    public int GetLineNumber() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%d %s", line, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Error)) return false;
        Error other = (Error) obj;
        return line == other.line && type == other.type;
    }

    @Override
    public int hashCode() {
        return line * 31 + type.hashCode();
    }
}