package exception;

import java.util.HashMap;
import java.util.Map;

public class ValidationError {
    private Map<String, String> fieldErrors;    // Erreurs par champ
    private Map<String, String> fieldValues;    // Valeurs par champ
    
    public ValidationError() {
        this.fieldErrors = new HashMap<>();
        this.fieldValues = new HashMap<>();
    }

    public void addError(String fieldName, String errorMessage) {
        this.fieldErrors.put(fieldName, errorMessage);
    }

    public void addValue(String fieldName, String value) {
        this.fieldValues.put(fieldName, value);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public Map<String, String> getFieldValues() {
        return fieldValues;
    }

    public boolean hasErrors() {
        return !fieldErrors.isEmpty();
    }
}