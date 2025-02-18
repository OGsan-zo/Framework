package exception;

import java.util.ArrayList;
import java.util.List;
import other.ModelView;

public class ValidationException extends Exception {
    private List<String> errors;
    private ModelView modelView;

    public ValidationException(ModelView errorView) {
        super();
        this.errors = new ArrayList<>();
        this.modelView = errorView;
    }

    public ValidationException() {
        this.errors = new ArrayList<>();
        this.modelView = new ModelView();
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder("Validation errors:\n");
        for (String error : errors) {
            message.append("- ").append(error).append("\n");
        }
        return message.toString();
    }

    public ModelView getModelView() {
        return modelView;
    }

    public void setModelView(ModelView modelView) {
        this.modelView = modelView;
    }
}