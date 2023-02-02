package fr.openent.lystore.utils;

public class LystoreUtils {
    public static String generateErrorMessage(Class classToThrow, String functionName, String errorInfos, Exception exception) {
        return generateErrorMessage(classToThrow, functionName, errorInfos, exception.getMessage());
    }

    public static String generateErrorMessage(Class classToThrow, String functionName, String errorInfos, Throwable exception) {
        return generateErrorMessage(classToThrow, functionName, errorInfos, exception.getMessage());
    }

    public static String generateErrorMessage(Class classToThrow, String functionName, String errorInfos, String exceptionMessage) {
        return String.format("[Lystore:%s@%s] %s : %s ", classToThrow.toString(), functionName, errorInfos, exceptionMessage);
    }
}
