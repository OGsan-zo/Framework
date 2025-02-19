package exception;

import java.util.HashMap;
import java.util.Map;
import other.ModelView;

public class ValidationException extends Exception {
    private ModelView modelView;
    private Map<String, String> validationErrors;
    private String redirectUrl;
    
    public ValidationException() {
        this.validationErrors = new HashMap<>();
    }
    
    public void setRedirectUrl(String url) {
        this.redirectUrl = url;
    }
    
    public String getRedirectUrl() {
        return this.redirectUrl;
    }
    
    public void addError(String field, String message) {
        this.validationErrors.put(field, message);
    }
    
    public Map<String, String> getValidationErrors() {
        return this.validationErrors;
    }
    
    public void setModelView(ModelView modelView) {
        this.modelView = modelView;
    }
    
    public ModelView getModelView() {
        return this.modelView;
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder("Validation errors:\n");
        validationErrors.forEach((field, error) -> {
            message.append("- ").append(field).append(": ").append(error).append("\n");
        });
        return message.toString();
    }
}