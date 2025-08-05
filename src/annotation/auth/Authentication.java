// Authentication.java
package annotation.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})  // Ajout du support pour TYPE
@Inherited                                      // Important pour l'héritage des annotations de classe
public @interface Authentication {
    String value() default "";
    boolean ignoreAuth() default false;  // Pour désactiver l'auth sur certaines méthodes
}