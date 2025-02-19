package other;

import java.util.Map;

public class FormUtils {

    public static String getValue(Map<String, String> fieldValues, String fieldName) {
        if (fieldValues != null && fieldValues.containsKey(fieldName)) {
            return fieldValues.get(fieldName);
        }
        return "";
    }

    public static String getError(Map<String, String> fieldErrors, String fieldName) {
        if (fieldErrors != null && fieldErrors.containsKey(fieldName)) {
            return fieldErrors.get(fieldName);
        }
        return null;
    }
}
