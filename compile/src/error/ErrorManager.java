package error;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class ErrorManager {
    private static final NavigableMap<Integer, Error> errors = new ConcurrentSkipListMap<>();
    private static volatile boolean isRecording = true;
    private static final Object lock = new Object();

    // 私有构造函数防止实例化
    private ErrorManager() {}

    public static boolean HaveNoError() {
        synchronized (lock) {
            return errors.isEmpty();
        }
    }

    public static void AddError(Error error) {
        if (error == null) return;

        synchronized (lock) {
            if (isRecording && !containsErrorAtLine(error.GetLineNumber())) {
                errors.put(error.GetLineNumber(), error);
            }
        }
    }

    private static boolean containsErrorAtLine(int lineNumber) {
        return errors.containsKey(lineNumber);
    }

    public static void SetStopRecordError() {
        synchronized (lock) {
            isRecording = false;
        }
    }

    public static void SetStartRecordError() {
        synchronized (lock) {
            isRecording = true;
        }
    }

    public static ArrayList<Error> GetErrorList() {
        synchronized (lock) {
            return errors.values()
                    .stream()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    // 额外的辅助方法（保持向后兼容）
    public static List<Error> getErrorsByType(ErrorType type) {
        synchronized (lock) {
            return errors.values()
                    .stream()
                    .filter(e -> e.GetErrorType() == type)
                    .collect(Collectors.toList());
        }
    }

    public static void clearErrors() {
        synchronized (lock) {
            errors.clear();
        }
    }

    public static int getErrorCount() {
        synchronized (lock) {
            return errors.size();
        }
    }
}