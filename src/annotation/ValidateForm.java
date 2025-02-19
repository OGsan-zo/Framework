package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import exception.ValidationError;
import exception.ValidationException;
import other.ModelView;

public class ValidateForm {
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface NotNull {
        String message() default "Ce champ ne peut pas être null";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ValidNumber {
        int min() default Integer.MIN_VALUE;
        int max() default Integer.MAX_VALUE;
        String message() default "La valeur n'est pas dans l'intervalle valide";
        boolean positiveOnly() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ValidEmail {
        String message() default "Format d'email invalide";
        String atSymbol() default "@";
        String domainSuffix() default ".com";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Length {
        int min() default 0;
        int max() default Integer.MAX_VALUE;
        String message() default "La longueur n'est pas valide";
    }

    public ValidationError validateObject(Object obj) throws ValidationException {
        ValidationError validationError = new ValidationError();

        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                validateField(field, obj, validationError);
            } catch (IllegalAccessException e) {
                validationError.addError(field.getName(), "Erreur d'accès au champ " + field.getName());
            }
        }

        if (validationError.hasErrors()) {
            // On utilise les méthodes existantes pour transférer les erreurs
            ValidationException ve = new ValidationException();
            validationError.getFieldErrors().forEach((field, error) -> 
                ve.addError(field, error)
            );
            throw ve;
        }

        return validationError;
    }
    
    private void validateField(Field field, Object obj, ValidationError validationError) 
            throws IllegalAccessException {
        
        String fieldName = field.getName();
        Object value = field.get(obj);
        
        // Toujours sauvegarder la valeur actuelle
        if (value != null) {
            validationError.addValue(fieldName, value.toString());
        }

        // Validation NotNull
        if (field.isAnnotationPresent(NotNull.class)) {
            NotNull annotation = field.getAnnotation(NotNull.class);
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                validationError.addError(fieldName, annotation.message());
            }
        }

        // Validation ValidNumber
        if (field.isAnnotationPresent(ValidNumber.class)) {
            ValidNumber annotation = field.getAnnotation(ValidNumber.class);
            if (value instanceof Number) {
                int numValue = ((Number) value).intValue();
                if (numValue < annotation.min() || numValue > annotation.max()) {
                    validationError.addError(fieldName, annotation.message());
                }
                if (annotation.positiveOnly() && numValue <= 0) {
                    validationError.addError(fieldName, 
                        "La valeur du champ " + fieldName + " doit être positive");
                }
            }
        }

        // Validation ValidEmail
        if (field.isAnnotationPresent(ValidEmail.class)) {
            ValidEmail annotation = field.getAnnotation(ValidEmail.class);
            if (value instanceof String) {
                String email = (String) value;
                if (!email.contains(annotation.atSymbol()) || 
                    !email.endsWith(annotation.domainSuffix())) {
                    validationError.addError(fieldName, annotation.message());
                }
            }
        }
    }
}